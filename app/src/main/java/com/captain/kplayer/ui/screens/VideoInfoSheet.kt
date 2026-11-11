package com.captain.kplayer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captain.kplayer.data.QueueEntry
import com.captain.kplayer.data.VideoItem
import com.captain.kplayer.data.formatDuration
import com.captain.kplayer.ui.components.GlassBottomSheet
import com.captain.kplayer.ui.theme.LocalTextPrimary
import com.captain.kplayer.ui.theme.LocalTextSecondary
import com.captain.kplayer.viewmodel.UiAudioTrack

/**
 * Video information sheet: file name, size, resolution, codec, bitrate,
 * frame rate and audio tracks for the video currently playing.
 *
 * The library [VideoItem] only carries what MediaStore reports, so
 * codec is the container MIME type, bitrate is unknown ("—") and the
 * audio list is a placeholder (Unknown / AAC / Stereo) — the real
 * per-track codec, channels and bitrate live in ExoPlayer's track
 * info. Pass [liveAudioTracks] (and [liveAudioBitrateKbps] when known)
 * from the player and the sheet prefers that live data over the
 * library placeholders. The call site is unchanged when they are
 * omitted.
 */
@Composable
fun VideoInfoSheet(
    entry: QueueEntry?,
    video: VideoItem?,
    onDismiss: () -> Unit,
    liveAudioTracks: List<UiAudioTrack>? = null,
    liveAudioBitrateKbps: Int? = null
) {
    GlassBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Video Info",
            color = LocalTextPrimary.current,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
        )
        if (entry == null) {
            Text(
                text = "Nothing is playing.",
                color = LocalTextSecondary.current,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 20.dp, bottom = 16.dp)
            )
        } else if (video != null) {
            InfoRow("File name", video.title)
            InfoRow("Path", video.url)
            InfoRow("Location", if (video.isLocal) "On this device" else "Network stream")
            InfoRow("Size", video.sizeText)
            InfoRow("Duration", formatDuration(video.durationMs))
            InfoRow("Resolution", video.resolution)
            InfoRow("Codec", video.codec)
            InfoRow("Bitrate", video.bitrateText)
            InfoRow("Frame rate", if (video.fps > 0) "${video.fps} fps" else "—")
            // Live ExoPlayer track info when the caller passes it;
            // otherwise fall back to the library's stored audio list.
            val audioText = if (!liveAudioTracks.isNullOrEmpty()) {
                liveAudioTracks.joinToString(", ") {
                    "${it.label} (${it.codec} ${it.channels})"
                }
            } else if (video.audioTracks.isNotEmpty()) {
                video.audioTracks.joinToString(", ") {
                    "${it.language} (${it.codec} ${it.channels})"
                }
            } else {
                "—"
            }
            InfoRow("Audio tracks", audioText)
            InfoRow(
                "Audio bitrate",
                if (liveAudioBitrateKbps != null && liveAudioBitrateKbps > 0) {
                    "${liveAudioBitrateKbps} kbps"
                } else {
                    "—"
                }
            )
            InfoRow(
                "Subtitles",
                if (video.subtitleTracks.isEmpty()) "None" else
                    video.subtitleTracks.joinToString(", ") {
                        "${it.language} (${it.source})"
                    }
            )
        } else {
            // Stream or audio queue entry without a library VideoItem.
            InfoRow("Title", entry.title)
            InfoRow("Source", entry.subtitle)
            InfoRow(
                "Duration",
                if (entry.durationMs > 0) formatDuration(entry.durationMs) else "Live / unknown"
            )
            InfoRow("URL", entry.url)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = LocalTextSecondary.current,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = value,
                color = LocalTextPrimary.current,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
