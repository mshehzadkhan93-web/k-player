package com.captain.kplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.captain.kplayer.ui.components.GlassBottomSheet
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.LocalTextPrimary
import com.captain.kplayer.ui.theme.LocalTextSecondary
import com.captain.kplayer.viewmodel.PlayerViewModel
import com.captain.kplayer.viewmodel.UiAudioTrack

/**
 * Audio track picker: every audio language in the file with its codec and
 * channel info, plus an Off option. Selection switches the track live.
 */
@Composable
fun AudioTrackSheet(
    playerVm: PlayerViewModel,
    audioTracks: List<UiAudioTrack>,
    audioDisabled: Boolean,
    onDismiss: () -> Unit
) {
    val accent = LocalAccent.current
    // Two tracks can share a language (e.g. two English tracks: main
    // and commentary). Number the duplicates so each row is tappable
    // without guessing which is which. Computed purely from the list,
    // keyed by track identity — no mutation during composition.
    val displayLabels = remember(audioTracks) {
        val counts = mutableMapOf<String, Int>()
        audioTracks.associate { track ->
            val n = (counts[track.label] ?: 0) + 1
            counts[track.label] = n
            val total = audioTracks.count { it.label == track.label }
            (track.groupIndex to track.trackIndex) to
                if (total > 1) "${track.label} $n" else track.label
        }
    }
    GlassBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Audio Track",
            color = LocalTextPrimary.current,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
        )
        Text(
            text = "Choose the audio language for this video. Switching is live — playback keeps going.",
            color = LocalTextSecondary.current,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
        )
        if (audioTracks.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Audiotrack,
                    contentDescription = null,
                    tint = LocalTextSecondary.current,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Default audio",
                    color = LocalTextPrimary.current,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        audioTracks.forEach { track ->
            key(track.groupIndex, track.trackIndex) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        playerVm.selectAudioTrack(track)
                        onDismiss()
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = track.selected && !audioDisabled,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = accent.start)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayLabels[track.groupIndex to track.trackIndex]
                            ?: track.label,
                        color = LocalTextPrimary.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${track.codec} • ${track.channels}",
                        color = LocalTextSecondary.current,
                        fontSize = 13.sp
                    )
                }
                if (track.selected && !audioDisabled) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = accent.start,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    playerVm.disableAudio()
                    onDismiss()
                }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = audioDisabled,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = accent.start)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Off",
                color = LocalTextPrimary.current,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Preferred audio languages offered in Settings > Audio. */
val AUDIO_LANGUAGES = listOf("English", "Hindi", "Urdu", "Arabic", "Spanish", "French", "German")
