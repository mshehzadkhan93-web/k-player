package com.captain.kplayer.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.captain.kplayer.data.formatDuration
import com.captain.kplayer.ui.components.ArtImage
import com.captain.kplayer.ui.components.ArtKind
import com.captain.kplayer.ui.components.GlassButton
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.ui.theme.TextSecondary
import com.captain.kplayer.viewmodel.PlayerViewModel

/**
 * Music player: large rotating album art over a blurred album-art
 * background, glass transport controls, seek bar, shuffle, repeat,
 * playlist, sleep timer and equalizer.
 */
@Composable
fun MusicPlayerScreen(
    navController: NavHostController,
    playerVm: PlayerViewModel,
    onOpenPlaylist: () -> Unit
) {
    val entry by playerVm.currentEntry.collectAsState()
    val isPlaying by playerVm.isPlaying.collectAsState()
    val position by playerVm.positionMs.collectAsState()
    val duration by playerVm.durationMs.collectAsState()
    val shuffle by playerVm.shuffle.collectAsState()
    val repeatMode by playerVm.repeatMode.collectAsState()
    val sleepRemaining by playerVm.sleepRemainingMs.collectAsState()

    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepMenu by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableStateOf<Float?>(null) }

    // The art keeps rotating while music plays and freezes when paused.
    val rotation = rememberInfiniteTransition(label = "albumRotation")
    val angle by rotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "albumAngle"
    )
    var frozenAngle by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) frozenAngle = angle
    }

    GradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            // Blurred album-art background.
            if (entry != null) {
                ArtImage(
                    key = entry!!.artKey,
                    kind = ArtKind.MUSIC,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(60.dp),
                    isVideo = false
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "Now Playing",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Rotating album art.
                if (entry != null) {
                    ArtImage(
                        key = entry!!.artKey,
                        kind = ArtKind.MUSIC,
                        modifier = Modifier
                            .size(280.dp)
                            .rotate(if (isPlaying) angle else frozenAngle)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = entry?.title ?: "Nothing playing",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = entry?.subtitle ?: "Pick a song from the library",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 6.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Seek bar.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDuration(
                            scrubFraction?.let { (it * duration).toLong() } ?: position
                        ),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = scrubFraction ?: if (duration > 0) {
                            position.toFloat() / duration.toFloat()
                        } else 0f,
                        onValueChange = { scrubFraction = it },
                        onValueChangeFinished = {
                            scrubFraction?.let {
                                playerVm.seekTo((it * duration).toLong())
                            }
                            scrubFraction = null
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = LocalAccent.current.start,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )
                    Text(
                        text = formatDuration(duration),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                // Transport controls.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { playerVm.toggleShuffle() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffle) LocalAccent.current.start else TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(
                        onClick = { playerVm.playPrevious() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = TextPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(
                        onClick = { playerVm.togglePlay() },
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(LocalAccent.current.brush)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(
                        onClick = { playerVm.playNext() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = TextPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(
                        onClick = { playerVm.cycleRepeatMode() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatMode == 0) TextSecondary else LocalAccent.current.start,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Up next: the next items in the queue — tap one to jump to it.
                val queue by playerVm.queue.collectAsState()
                val currentIndex by playerVm.currentIndex.collectAsState()
                val upNext = queue.drop(currentIndex + 1).take(3)
                if (upNext.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Up next",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        upNext.forEachIndexed { offset, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerVm.playQueueEntry(currentIndex + 1 + offset)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${offset + 1}.",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.subtitle,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (item.durationMs > 0) {
                                    Text(
                                        text = formatDuration(item.durationMs),
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassButton(
                            onClick = { showSleepMenu = true },
                            icon = Icons.Rounded.Timer,
                            text = if (sleepRemaining != null) {
                                "Sleep ${formatDuration(sleepRemaining!!)}"
                            } else {
                                "Sleep timer"
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = showSleepMenu,
                            onDismissRequest = { showSleepMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Off") },
                                onClick = {
                                    playerVm.cancelSleepTimer()
                                    showSleepMenu = false
                                }
                            )
                            listOf(15, 30, 60).forEach { minutes ->
                                DropdownMenuItem(
                                    text = { Text("$minutes minutes") },
                                    onClick = {
                                        playerVm.startSleepTimer(minutes)
                                        showSleepMenu = false
                                    }
                                )
                            }
                        }
                    }
                    GlassButton(
                        onClick = { showEqualizer = true },
                        icon = Icons.Rounded.Equalizer,
                        text = "Equalizer",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onOpenPlaylist,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlaylistPlay,
                            contentDescription = "Playlist",
                            tint = TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    if (showEqualizer) {
        EqualizerDialog(playerVm = playerVm, onDismiss = { showEqualizer = false })
    }
}
