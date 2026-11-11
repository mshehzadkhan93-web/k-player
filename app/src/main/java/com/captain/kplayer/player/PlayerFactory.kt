package com.captain.kplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.audio.AudioFormat
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/**
 * Builds ExoPlayer instances and MediaItems.
 *
 * Decoding modes, like MX Player:
 * - "hw": hardware decoders only, no software fallback.
 * - "hw_plus": hardware decoders with software fallback when hardware fails.
 * - "sw": software decoders only, for devices with broken hardware codecs.
 *
 * HARD LIMIT — software VIDEO decoding does not exist in this app, in any
 * mode: the FFmpeg extension bundled here is AUDIO-ONLY
 * (FfmpegAudioRenderer — DTS / DTS-HD / TrueHD / AC3 / E-AC3). "sw" only
 * selects the software MediaCodec decoders Android ships (OMX.google.* /
 * c2.android.*) plus the FFmpeg audio renderer. A video track no MediaCodec
 * on the phone can decode (e.g. 4K HEVC Main 10 without hardware support)
 * still cannot play in SW mode; [PlayerViewModel] says so plainly instead
 * of pretending a retry will fix it. No new dependency can change this
 * without a full FFmpeg video decoder extension, which this project does
 * not ship (see the report for the exact artifact that would be needed).
 *
 * Audio delay (A/V sync) is applied by [AudioDelayProcessor], a PCM
 * AudioProcessor inserted into the audio sink's processor chain. Media3
 * 1.2.1 has NO built-in audio-delay API — DefaultAudioSink only gained
 * setOutputStreamOffsetUs in a later Media3 release — so the delay is done
 * in PCM: a positive delay holds decoded audio back, a negative delay
 * drops the leading audio after each seek/flush. It therefore applies to
 * decoded PCM audio only, not to bitstream passthrough/offload.
 */
object PlayerFactory {

    /**
     * ID stamped on the subtitle configuration of an externally loaded
     * (.srt/.vtt/.ass) file. Media3 propagates a SubtitleConfiguration's
     * id onto the created text track's Format.id, so
     * [PlayerViewModel.refreshTrackLists] can tell an external file apart
     * from the video's embedded subtitle tracks (which carry no such id).
     */
    const val EXTERNAL_SUBTITLE_ID = "external-subtitle"

    /** The delay processor of the most recently built player, if any. */
    @Volatile
    private var lastDelayProcessor: AudioDelayProcessor? = null

    /**
     * Applies an audio delay to the current player, live. Positive = audio
     * plays later (use when audio is ahead of the video), negative = audio
     * plays earlier. No-op before the first player is built; the ViewModel
     * also passes the persisted delay into [buildPlayer] so a rebuild
     * (e.g. decoder-mode switch) re-applies it.
     */
    fun setAudioDelayMs(delayMs: Long) {
        lastDelayProcessor?.delayMs = delayMs
    }

    @OptIn(UnstableApi::class)
    fun buildPlayer(context: Context, decodingMode: String, audioDelayMs: Long = 0L): ExoPlayer {
        val renderersFactory = DelayAwareRenderersFactory(context).apply {
            delayProcessor.delayMs = audioDelayMs
            // PREFER extension renderers so the FFmpeg audio renderer
            // (media3-ffmpeg-decoder) handles DTS / DTS-HD / TrueHD /
            // AC3 / E-AC3 instead of failing on hardware decoders that
            // cannot decode them (bug 3: no audio on heavy films).
            setExtensionRendererMode(
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            )
            when (decodingMode) {
                "hw" -> setEnableDecoderFallback(false)
                "sw" -> {
                    setEnableDecoderFallback(true)
                    setMediaCodecSelector(SoftwareOnlySelector)
                }
                else -> setEnableDecoderFallback(true)
            }
        }
        lastDelayProcessor = renderersFactory.delayProcessor
        // Larger buffers stop stalls on high-bitrate files and slow
        // storage: keep at least 15s buffered, up to 60s, and prioritise
        // buffered time over buffered bytes (bug 2: stuck videos).
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        return ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build()
    }

    /**
     * DefaultRenderersFactory that builds its audio sink with our
     * [AudioDelayProcessor] in the PCM processor chain. The override
     * reproduces 1.2.1's default buildAudioSink body exactly, plus the
     * one extra setAudioProcessors call — nothing else about the sink
     * changes (float output and AudioTrack playback params are forwarded
     * untouched).
     */
    @OptIn(UnstableApi::class)
    private class DelayAwareRenderersFactory(context: Context) :
        DefaultRenderersFactory(context) {

        val delayProcessor = AudioDelayProcessor()

        protected override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink? {
            return DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf<AudioProcessor>(delayProcessor))
                .build()
        }
    }

    /**
     * PCM audio delay / advance, in milliseconds. Positive [delayMs] holds
     * decoded audio back (audio plays later); negative [delayMs] drops the
     * leading audio after each flush/seek (audio plays earlier). Changing
     * [delayMs] takes effect live on subsequently queued buffers; a seek
     * or track change restarts the delay from a clean buffer.
     *
     * The processor is always active (onConfigure returns the input format
     * unchanged) so a delay change applies immediately even when the
     * previous delay was 0; with delay 0 and an empty hold-back buffer it
     * is a straight copy. Only PCM reaches this chain — bitstream
     * passthrough/offload bypasses audio processors entirely, so the delay
     * does not apply there.
     *
     * This is the highest-risk new code in the PLAYER package: it was
     * written against the Media3 1.2.1 BaseAudioProcessor contract
     * (onConfigure / onQueueInput / onQueueEndOfStream / onFlush / onReset
     * / replaceOutputBuffer) but could NOT be compiled or run in this
     * sandbox — it needs the CI build plus a device check (delay +250ms
     * and -250ms on a real video) before it is trusted.
     */
    @OptIn(UnstableApi::class)
    private class AudioDelayProcessor : BaseAudioProcessor() {

        /** Wanted delay in ms: + = audio later, - = audio earlier. */
        @Volatile
        var delayMs: Long = 0L

        private var sampleRateHz: Int = 0
        private var bytesPerFrame: Int = 0

        /** Decoded audio held back to realise a positive delay (write mode: position = stored bytes). */
        private var pendingBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
        private var pendingSize: Int = 0

        /** Bytes still to drop after a flush, to realise a negative delay. */
        private var bytesToSkip: Long = 0L

        private fun delayBytes(): Long {
            val delay = delayMs
            if (delay <= 0L || sampleRateHz <= 0 || bytesPerFrame <= 0) return 0L
            return delay * sampleRateHz * bytesPerFrame / 1000L
        }

        private fun skipBytes(): Long {
            val delay = delayMs
            if (delay >= 0L || sampleRateHz <= 0 || bytesPerFrame <= 0) return 0L
            return -delay * sampleRateHz * bytesPerFrame / 1000L
        }

        override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
            sampleRateHz = inputAudioFormat.sampleRate
            bytesPerFrame = inputAudioFormat.bytesPerFrame
            bytesToSkip = skipBytes()
            return inputAudioFormat
        }

        override fun onQueueInput(inputBuffer: ByteBuffer) {
            if (!inputBuffer.hasRemaining()) return
            // Negative delay: drop the leading bytes of the stream after
            // each flush (seek / track change restarts the drop).
            if (bytesToSkip > 0) {
                val drop = minOf(bytesToSkip, inputBuffer.remaining().toLong()).toInt()
                inputBuffer.position(inputBuffer.position() + drop)
                bytesToSkip -= drop
                if (!inputBuffer.hasRemaining()) return
            }
            val delay = delayBytes()
            if (delay <= 0L && pendingSize == 0) {
                // No delay and nothing held back: straight copy.
                val out = replaceOutputBuffer(inputBuffer.remaining())
                out.put(inputBuffer)
                out.flip()
                return
            }
            // Positive delay (or draining after the delay shrank to 0):
            // append the input, emit everything older than `delay`.
            val inputBytes = inputBuffer.remaining()
            ensurePendingCapacity(pendingSize + inputBytes)
            pendingBuffer.put(inputBuffer)
            pendingSize = pendingBuffer.position()
            val outputBytes = (pendingSize - delay).coerceAtLeast(0L).toInt()
            if (outputBytes > 0) {
                val out = replaceOutputBuffer(outputBytes)
                pendingBuffer.flip() // limit = pendingSize, position = 0
                val oldLimit = pendingBuffer.limit()
                pendingBuffer.limit(outputBytes)
                out.put(pendingBuffer)
                pendingBuffer.limit(oldLimit)
                pendingBuffer.compact() // remaining bytes move to the front
                pendingSize = pendingBuffer.position()
                out.flip()
            }
        }

        override fun onQueueEndOfStream() {
            // Drain whatever is still held back so the tail of the audio
            // is not lost; BaseAudioProcessor reports ended once the sink
            // has consumed this output.
            if (pendingSize > 0) {
                val out = replaceOutputBuffer(pendingSize)
                pendingBuffer.flip()
                out.put(pendingBuffer)
                pendingBuffer.clear()
                pendingSize = 0
                out.flip()
            }
        }

        override fun onFlush() {
            // A seek restarts the delay from a clean buffer.
            pendingBuffer.clear()
            pendingSize = 0
            bytesToSkip = skipBytes()
        }

        override fun onReset() {
            pendingBuffer = AudioProcessor.EMPTY_BUFFER
            pendingSize = 0
            bytesToSkip = 0L
            sampleRateHz = 0
            bytesPerFrame = 0
        }

        private fun ensurePendingCapacity(needed: Int) {
            if (pendingBuffer.capacity() < needed) {
                val grown = ByteBuffer.allocateDirect(
                    max(needed, pendingBuffer.capacity() * 2).coerceAtLeast(4096)
                ).order(ByteOrder.nativeOrder())
                pendingBuffer.flip()
                grown.put(pendingBuffer)
                pendingBuffer = grown
            }
        }
    }

    private object SoftwareOnlySelector : MediaCodecSelector {
        override fun getDecoderInfos(
            mimeType: String,
            requiresSecureDecoder: Boolean,
            requiresTunnelingDecoder: Boolean
        ): List<MediaCodecInfo> {
            return MediaCodecSelector.DEFAULT
                .getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                .filter { info ->
                    info.name.startsWith("OMX.google.") || info.name.startsWith("c2.android.")
                }
        }
    }

    fun mediaItem(url: String, title: String, subtitle: String = ""): MediaItem {
        return MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(subtitle)
                    .build()
            )
            .build()
    }

    fun mediaItemWithSubtitles(
        url: String,
        title: String,
        subtitleUri: String,
        subtitleLanguage: String,
        mimeType: String
    ): MediaItem {
        val subtitle = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitleUri))
            .setId(EXTERNAL_SUBTITLE_ID)
            .setMimeType(mimeType)
            .setLanguage(subtitleLanguage)
            .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
            .build()
        return MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
            .setSubtitleConfigurations(listOf(subtitle))
            .build()
    }
}
