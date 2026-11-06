package com.captain.kplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

/**
 * Builds ExoPlayer instances and MediaItems.
 *
 * Decoding modes, like MX Player:
 * - "hw": hardware decoders only, no software fallback.
 * - "hw_plus": hardware decoders with software fallback when hardware fails.
 * - "sw": software decoders only, for devices with broken hardware codecs.
 */
object PlayerFactory {

    fun buildPlayer(context: Context, decodingMode: String): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context).apply {
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
