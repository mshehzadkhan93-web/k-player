package com.captain.kplayer.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captain.kplayer.ui.components.GlassBottomSheet
import com.captain.kplayer.ui.components.GlassButton
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.ui.theme.TextSecondary
import com.captain.kplayer.viewmodel.PlayerViewModel
import com.captain.kplayer.viewmodel.SettingsViewModel
import com.captain.kplayer.viewmodel.UiSubtitleTrack

/**
 * Subtitle picker: embedded subtitle languages, Off, loading an external
 * .srt/.vtt/.ass file, and live style controls (size, color, background)
 * plus sync delay in +/- 0.5s steps.
 */
@Composable
fun SubtitleSheet(
    playerVm: PlayerViewModel,
    settingsVm: SettingsViewModel,
    subtitleTracks: List<UiSubtitleTrack>,
    subtitlesDisabled: Boolean,
    onLoadFromFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = LocalAccent.current
    val delayMs by playerVm.subtitleDelayMs.collectAsState()
    val size by settingsVm.subtitleSize.collectAsState()
    val color by settingsVm.subtitleColor.collectAsState()
    val background by settingsVm.subtitleBackground.collectAsState()
    val font by settingsVm.subtitleFont.collectAsState()

    GlassBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Subtitles",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
        )
        Text(
            text = "Subtitles in .srt, .vtt and .ass are supported.",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
        )
        if (subtitleTracks.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Subtitles,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "No embedded subtitles in this video — load one from a file",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
        subtitleTracks.forEach { track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        playerVm.selectSubtitleTrack(track)
                        onDismiss()
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = track.selected && !subtitlesDisabled,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = accent.start)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = track.label,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                EmbeddedTag()
                Spacer(modifier = Modifier.width(8.dp))
                if (track.selected && !subtitlesDisabled) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = accent.start,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    playerVm.disableSubtitles()
                    onDismiss()
                }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = subtitlesDisabled,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = accent.start)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Off",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        GlassButton(
            onClick = onLoadFromFile,
            icon = Icons.Rounded.Add,
            text = "Load from file (.srt / .vtt / .ass)",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // Sync delay: +/- 0.5s steps.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sync delay",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { playerVm.adjustSubtitleDelay(-500L) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Delay minus 0.5s",
                    tint = TextPrimary
                )
            }
            Text(
                text = if (delayMs >= 0) "+${delayMs / 1000.0}s" else "${delayMs / 1000.0}s",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = { playerVm.adjustSubtitleDelay(500L) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Delay plus 0.5s",
                    tint = TextPrimary
                )
            }
        }

        // Live style controls (same values as Settings > Subtitles).
        Text(
            text = "Style",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Size",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            SheetCycleButton(
                value = sizeLabel(size),
                onClick = { settingsVm.setSubtitleSize(nextSize(size)) }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Color",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            SheetCycleButton(
                value = color,
                onClick = { settingsVm.setSubtitleColor(nextOption(SUBTITLE_COLORS, color)) }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Background",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            SheetCycleButton(
                value = background,
                onClick = {
                    settingsVm.setSubtitleBackground(
                        nextOption(SUBTITLE_BACKGROUNDS, background)
                    )
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Font",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            SheetCycleButton(
                value = font,
                onClick = { settingsVm.setSubtitleFont(nextOption(SUBTITLE_FONTS, font)) }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Small pill button that cycles through a list of values on tap. */
@Composable
fun SheetCycleButton(value: String, onClick: () -> Unit) {
    GlassButton(onClick = onClick, text = value)
}

/** Small pill marking a track as embedded in the video file. */
@Composable
private fun EmbeddedTag() {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = LocalAccent.current.start.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "Embedded",
            color = LocalAccent.current.start,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

val SUBTITLE_COLORS = listOf("White", "Yellow", "Cyan", "Green", "Pink")
val SUBTITLE_BACKGROUNDS = listOf("None", "Black", "Semi-transparent")
val SUBTITLE_FONTS = listOf("Sans Serif", "Serif", "Monospace")
val SUBTITLE_LANGUAGES = listOf("English", "Hindi", "Urdu", "Arabic", "Spanish", "French", "German")

fun nextOption(options: List<String>, current: String): String {
    val index = options.indexOf(current)
    return options[(index + 1).coerceAtLeast(0) % options.size]
}

fun sizeLabel(size: Float): String = when {
    size < 0.85f -> "Small"
    size < 1.15f -> "Medium"
    size < 1.35f -> "Large"
    else -> "Extra large"
}

fun nextSize(size: Float): Float = when {
    size < 0.85f -> 1.0f
    size < 1.15f -> 1.25f
    size < 1.35f -> 1.5f
    else -> 0.75f
}
