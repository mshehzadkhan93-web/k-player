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
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.ui.theme.TextSecondary

/**
 * Video information sheet: file name, size, resolution, codec, bitrate,
 * frame rate and audio tracks for the video currently playing.
 */
@Composable
fun VideoInfoSheet(
    entry: QueueEntry?,
    video: VideoItem?,
    onDismiss: () -> Unit
) {
    GlassBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Video Info",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 12.dp)
        )
        if (entry == null) {
            Text(
                text = "Nothing is playing.",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 20.dp, bottom = 16.dp)
            )
        } else if (video != null) {
            InfoRow("File name", video.title)
            InfoRow("Location", if (video.isLocal) "On this device" else "Network stream")
            InfoRow("Size", video.sizeText)
            InfoRow("Duration", formatDuration(video.durationMs))
            InfoRow("Resolution", video.resolution)
            InfoRow("Codec", video.codec)
            InfoRow("Bitrate", video.bitrateText)
            InfoRow("Frame rate", if (video.fps > 0) "${video.fps} fps" else "—")
            InfoRow(
                "Audio tracks",
                video.audioTracks.joinToString(", ") {
                    "${it.language} (${it.codec} ${it.channels})"
                }
            )
            InfoRow(
                "Subtitles",
                if (video.subtitleTracks.isEmpty()) "None" else
                    video.subtitleTracks.joinToString(", ") { it.language }
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
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
