package com.captain.kplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
            when (decodingMode) {
                "hw" -> setEnableDecoderFallback(false)
                "sw" -> {
                    setEnableDecoderFallback(true)
                    setMediaCodecSelector(SoftwareOnlySelector)
                }
                else -> setEnableDecoderFallback(true)
            }
        }
        return ExoPlayer.Builder(context, renderersFactory).build()
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
