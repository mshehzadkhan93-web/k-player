package com.captain.kplayer.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Rational
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.navigation.NavHostController
import com.captain.kplayer.data.formatDuration
import com.captain.kplayer.ui.components.AccentGradientButton
import com.captain.kplayer.ui.components.GlassButton
import com.captain.kplayer.ui.components.GlassDialog
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.viewmodel.LibraryViewModel
import com.captain.kplayer.viewmodel.PlayerViewModel
import com.captain.kplayer.viewmodel.SettingsViewModel
import com.captain.kplayer.viewmodel.VideoAspect
import kotlinx.coroutines.delay
import kotlin.math.abs

/** Gesture overlay kinds shown while the user drags or double-taps. */
private sealed interface GestureOverlay {
    data class Brightness(val percent: Int) : GestureOverlay
    data class Volume(val percent: Int) : GestureOverlay
    data class Seek(val previewMs: Long, val totalMs: Long) : GestureOverlay
    data class DoubleTap(val label: String) : GestureOverlay
    data object Speed2x : GestureOverlay
}

/**
 * Fullscreen video player: tap to toggle glass controls, double-tap to seek
 * by the configured step, long-press for 2x speed, drag gestures for
 * brightness / volume / seek, speed and aspect controls, screen lock,
 * background play, picture-in-picture, audio/subtitle sheets, equalizer,
 * playlist and video info.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoPlayerScreen(
    navController: NavHostController,
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel,
    settingsVm: SettingsViewModel,
    onOpenPlaylist: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current

    val entry by playerVm.currentEntry.collectAsState()
    val currentIndex by playerVm.currentIndex.collectAsState()
    val queue by playerVm.queue.collectAsState()
    val isPlaying by playerVm.isPlaying.collectAsState()
    val isBuffering by playerVm.isBuffering.collectAsState()
    val playerError by playerVm.playerError.collectAsState()
    val position by playerVm.positionMs.collectAsState()
    val duration by playerVm.durationMs.collectAsState()
    val speed by playerVm.speed.collectAsState()
    val aspect by playerVm.aspect.collectAsState()
    val locked by playerVm.locked.collectAsState()
    val audioTracks by playerVm.audioTracks.collectAsState()
    val subtitleTracks by playerVm.subtitleTracks.collectAsState()
    val audioDisabled by playerVm.audioDisabled.collectAsState()
    val subtitlesDisabled by playerVm.subtitlesDisabled.collectAsState()
    val resumePrompt by playerVm.resumePrompt.collectAsState()
    val abState by playerVm.abState.collectAsState()
    val abRepeatEnabled by playerVm.abRepeatEnabled.collectAsState()
    val doubleTapStep by playerVm.doubleTapSeekSeconds.collectAsState()
    val pipEnabled by playerVm.pipEnabled.collectAsState()
    val backgroundPlay by playerVm.backgroundPlay.collectAsState()
    val kidsLock by playerVm.kidsLock.collectAsState()
    val orientationLock by playerVm.orientationLock.collectAsState()
    val autoRotate by playerVm.autoRotate.collectAsState()
    val brightness by playerVm.brightnessPercent.collectAsState()

    val gestureBrightness by settingsVm.gestureBrightness.collectAsState()
    val gestureVolume by settingsVm.gestureVolume.collectAsState()
    val gestureSeek by settingsVm.gestureSeek.collectAsState()
    val gestureSensitivity by settingsVm.gestureSensitivity.collectAsState()
    val subtitleColor by settingsVm.subtitleColor.collectAsState()
    val subtitleBackground by settingsVm.subtitleBackground.collectAsState()
    val subtitleFont by settingsVm.subtitleFont.collectAsState()
    val subtitleSize by settingsVm.subtitleSize.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showVideoInfo by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<GestureOverlay?>(null) }
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    var lastSliderSeekMs by remember { mutableStateOf(0L) }

    // Kids Lock: the player UI starts locked (long-press the lock to unlock).
    LaunchedEffect(kidsLock) {
        if (kidsLock) playerVm.setLocked(true)
    }

    // Orientation: the lock selector wins; "Auto" follows the auto-rotate
    // toggle (sensor landscape when on, fixed landscape when off).
    DisposableEffect(orientationLock, autoRotate) {
        activity?.requestedOrientation = when (orientationLock) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> if (autoRotate) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Apply the brightness gesture to the window.
    LaunchedEffect(brightness) {
        activity?.window?.let { window ->
            val attrs = window.attributes
            attrs.screenBrightness = brightness
            window.attributes = attrs
        }
    }

    // Save the resume position when leaving the player.
    DisposableEffect(Unit) {
        onDispose { playerVm.onLeavePlayer() }
    }
    BackHandler {
        playerVm.onLeavePlayer()
        navController.popBackStack()
    }

    // Auto-hide the controls after 3s while playing.
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }
    // Double-tap / gesture overlays fade away on their own.
    LaunchedEffect(overlay) {
        val current = overlay
        if (current is GestureOverlay.DoubleTap) {
            delay(800)
            if (overlay == current) overlay = null
        }
    }
    // Bug 1: a new track resets any in-progress scrub, and stamps the
    // video as played so it surfaces in Recently Played (next / prev /
    // auto-advance all flow through the observed current entry).
    LaunchedEffect(entry?.id) {
        scrubFraction = null
        overlay = null
        val current = entry
        if (current != null && current.isVideo) {
            libraryVm.markVideoPlayed(current.id)
        }
    }
    // Surface player errors as a toast instead of a stuck frame / crash.
    LaunchedEffect(playerError) {
        playerError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            playerVm.clearPlayerError()
        }
    }

    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else "subtitle.srt"
            } ?: "subtitle.srt"
            playerVm.addExternalSubtitle(uri, name)
            Toast.makeText(context, "Subtitle loaded: $name", Toast.LENGTH_SHORT).show()
        }
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity?.enterPictureInPictureMode(params)
        }
    }

    fun rotate() {
        activity?.requestedOrientation = if (
            configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        ) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (playerVm.isPlayerReady) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerVm.player
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { view ->
                    view.player = playerVm.player
                    view.resizeMode = when (aspect) {
                        VideoAspect.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        VideoAspect.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        VideoAspect.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                    view.subtitleView?.let { subtitleView ->
                        subtitleView.setStyle(
                            CaptionStyleCompat(
                                subtitleColorValue(subtitleColor),
                                subtitleBackgroundValue(subtitleBackground),
                                android.graphics.Color.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_NONE,
                                android.graphics.Color.BLACK,
                                subtitleTypeface(subtitleFont)
                            )
                        )
                        subtitleView.setFractionalTextSize(
                            SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitleSize
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Loading player…", color = TextPrimary, fontSize = 16.sp)
            }
        }

        // Buffering spinner: shows while the player waits for data.
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Gesture layer: tap / double-tap / long-press / drag.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(locked, doubleTapStep, gestureBrightness, gestureVolume, gestureSeek, gestureSensitivity) {
                        detectTapGestures(
                            onTap = {
                                if (!locked) controlsVisible = !controlsVisible
                            },
                            onDoubleTap = { offset ->
                                if (locked) return@detectTapGestures
                                val stepMs = doubleTapStep * 1000L
                                if (offset.x < widthPx / 2f) {
                                    playerVm.seekRelative(-stepMs)
                                    overlay = GestureOverlay.DoubleTap("-${doubleTapStep}s")
                                } else {
                                    playerVm.seekRelative(stepMs)
                                    overlay = GestureOverlay.DoubleTap("+${doubleTapStep}s")
                                }
                            },
                            onLongPress = {
                                if (locked) return@detectTapGestures
                                playerVm.setSpeedTemporarily(2f)
                                overlay = GestureOverlay.Speed2x
                            },
                            onPress = {
                                val released = tryAwaitRelease()
                                if (released && overlay is GestureOverlay.Speed2x) {
                                    playerVm.restoreSpeed()
                                    overlay = null
                                }
                            }
                        )
                    }
                    .pointerInput(locked, gestureBrightness, gestureVolume, gestureSeek, gestureSensitivity) {
                        var dragKind = 0 // 0 none, 1 brightness, 2 volume, 3 seek
                        var startBrightness = 0f
                        var startVolume = 0
                        var startPosition = 0L
                        var seekAccum = 0f
                        var seekPreview = 0L
                        var lastLiveSeekMs = 0L
                        detectDragGestures(
                            onDragStart = {
                                dragKind = 0
                                startBrightness = playerVm.brightnessPercent.value
                                startVolume = playerVm.volumePercent.value
                                startPosition = playerVm.positionMs.value
                                seekAccum = 0f
                                seekPreview = startPosition
                            },
                            onDragEnd = {
                                if (dragKind == 3) playerVm.seekTo(seekPreview)
                                overlay = null
                                dragKind = 0
                            },
                            onDragCancel = {
                                overlay = null
                                dragKind = 0
                            },
                            onDrag = { change, dragAmount ->
                                if (locked) return@detectDragGestures
                                if (dragKind == 0) {
                                    dragKind = if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                        if (gestureSeek) 3 else 0
                                    } else if (change.position.x < widthPx / 2f) {
                                        if (gestureBrightness) 1 else 0
                                    } else {
                                        if (gestureVolume) 2 else 0
                                    }
                                }
                                when (dragKind) {
                                    1 -> {
                                        val delta = -dragAmount.y / heightPx * gestureSensitivity
                                        val next = (startBrightness + delta)
                                            .coerceIn(0.05f, 1f)
                                        playerVm.setBrightnessPercent(next)
                                        overlay = GestureOverlay.Brightness((next * 100).toInt())
                                    }
                                    2 -> {
                                        val delta = (-dragAmount.y / heightPx * 100f *
                                            gestureSensitivity).toInt()
                                        val next = (startVolume + delta).coerceIn(0, 100)
                                        playerVm.setVolumePercent(next)
                                        overlay = GestureOverlay.Volume(next)
                                    }
                                    3 -> {
                                        seekAccum += dragAmount.x
                                        val total = playerVm.durationMs.value
                                        val deltaMs = (seekAccum / widthPx * total *
                                            gestureSensitivity).toLong()
                                        seekPreview = (startPosition + deltaMs)
                                            .coerceIn(0L, total.coerceAtLeast(0L))
                                        overlay = GestureOverlay.Seek(seekPreview, total)
                                        // Bug 6: smooth scrub — the video
                                        // follows the finger (throttled live
                                        // seeks); the final position settles
                                        // on release.
                                        val now = System.currentTimeMillis()
                                        if (now - lastLiveSeekMs > 150L) {
                                            lastLiveSeekMs = now
                                            playerVm.seekTo(seekPreview)
                                        }
                                    }
                                }
                            }
                        )
                    }
            )
        }

        // Gesture overlay (brightness / volume / seek preview / 2x / double-tap).
        overlay?.let { current ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                when (current) {
                    is GestureOverlay.Brightness -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BrightnessHigh, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Brightness ${current.percent}%", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    is GestureOverlay.Volume -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VolumeUp, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Volume ${current.percent}%", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    is GestureOverlay.Seek -> Text(
                        "${formatDuration(current.previewMs)} / ${formatDuration(current.totalMs)}",
                        color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                    is GestureOverlay.DoubleTap -> Text(
                        current.label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                    GestureOverlay.Speed2x -> Text(
                        "2x speed", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Locked state: only the lock control is reachable.
        if (locked) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .combinedClickable(
                            onClick = {
                                if (!kidsLock) playerVm.setLocked(false)
                            },
                            onLongClick = {
                                if (kidsLock) playerVm.setLocked(false)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Unlock",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (kidsLock) "Long-press the lock to unlock" else "Tap the lock to unlock",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Glass controls.
        AnimatedVisibility(
            visible = controlsVisible && !locked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar.
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        playerVm.onLeavePlayer()
                        navController.popBackStack()
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
                    }
                    Text(
                        text = entry?.title ?: "K-Player",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    // Queue position, refreshed from the observed entry on
                    // every track change.
                    if (queue.size > 1) {
                        Text(
                            text = "${currentIndex + 1} / ${queue.size}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    IconButton(onClick = { showAudioSheet = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Audiotrack, "Audio track", tint = Color.White)
                    }
                    IconButton(onClick = { showSubtitleSheet = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Subtitles, "Subtitles", tint = Color.White)
                    }
                    IconButton(onClick = { subtitlePicker.launch("*/*") }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.FileUpload, "Add subtitle file", tint = Color.White)
                    }
                    IconButton(onClick = { showEqualizer = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Equalizer, "Equalizer", tint = Color.White)
                    }
                    if (pipEnabled) {
                        IconButton(onClick = { enterPip() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.PictureInPictureAlt, "Picture-in-picture", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { rotate() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.ScreenRotation, "Rotate", tint = Color.White)
                    }
                    IconButton(onClick = { showVideoInfo = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.MoreVert, "More", tint = Color.White)
                    }
                }

                // Center transport controls: fade + slight scale (~300ms),
                // never an abrupt appear/disappear.
                AnimatedVisibility(
                    visible = controlsVisible && !locked,
                    enter = fadeIn(animationSpec = tween(300)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) +
                        scaleOut(targetScale = 0.92f, animationSpec = tween(300)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        TextButton(
                            label = "-${doubleTapStep}s",
                            onClick = { playerVm.seekRelative(-doubleTapStep * 1000L) }
                        )
                        IconButton(onClick = { playerVm.playPrevious() }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                .clickable { playerVm.togglePlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        IconButton(onClick = { playerVm.playNext() }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(40.dp))
                        }
                        TextButton(
                            label = "+${doubleTapStep}s",
                            onClick = { playerVm.seekRelative(doubleTapStep * 1000L) }
                        )
                    }
                }

                // Bottom bar: seek + options.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatDuration(scrubFraction?.let { (it * duration).toLong() } ?: position),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = scrubFraction ?: if (duration > 0) {
                                position.toFloat() / duration.toFloat()
                            } else 0f,
                            onValueChange = {
                                scrubFraction = it
                                // Bug 6: live time preview bubble + smooth
                                // scrub — the video follows the thumb
                                // (throttled live seeks), settling on release.
                                val previewMs = (it * duration).toLong()
                                overlay = GestureOverlay.Seek(previewMs, duration)
                                val now = System.currentTimeMillis()
                                if (now - lastSliderSeekMs > 150L) {
                                    lastSliderSeekMs = now
                                    playerVm.seekTo(previewMs)
                                }
                            },
                            onValueChangeFinished = {
                                scrubFraction?.let {
                                    playerVm.seekTo((it * duration).toLong())
                                }
                                scrubFraction = null
                                overlay = null
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
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Bug 8: MX Player style transport on the seek-bar row.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { playerVm.playPrevious() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Color.White)
                        }
                        TextButton(
                            label = "-${doubleTapStep}s",
                            onClick = { playerVm.seekRelative(-doubleTapStep * 1000L) }
                        )
                        TextButton(
                            label = "+${doubleTapStep}s",
                            onClick = { playerVm.seekRelative(doubleTapStep * 1000L) }
                        )
                        IconButton(onClick = { playerVm.playNext() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            TextButton(
                                label = "${speed}x",
                                onClick = { showSpeedMenu = true }
                            )
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                playerVm.speedOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("${option}x") },
                                        onClick = {
                                            playerVm.setSpeed(option)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        TextButton(
                            label = when (aspect) {
                                VideoAspect.FIT -> "Fit"
                                VideoAspect.FILL -> "Fill"
                                VideoAspect.ZOOM -> "Zoom"
                            },
                            onClick = { playerVm.cycleAspect() }
                        )
                        IconButton(
                            onClick = { playerVm.setLocked(true) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.LockOpen, "Lock screen", tint = Color.White)
                        }
                        TextButton(
                            label = if (backgroundPlay) "BG On" else "BG Off",
                            onClick = { settingsVm.setBackgroundPlay(!backgroundPlay) }
                        )
                        IconButton(onClick = onOpenPlaylist, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.PlaylistPlay, "Playlist", tint = Color.White)
                        }
                        TextButton(
                            label = when (abState) {
                                PlayerViewModel.ABState.OFF -> "A-B"
                                PlayerViewModel.ABState.A_SET -> "A…"
                                PlayerViewModel.ABState.LOOPING -> "A-B ●"
                            },
                            enabled = abRepeatEnabled,
                            onClick = {
                                if (abRepeatEnabled) {
                                    playerVm.cycleABRepeat()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Enable A-B repeat in Settings > Playback",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Sheets and dialogs.
    if (showAudioSheet) {
        AudioTrackSheet(
            playerVm = playerVm,
            audioTracks = audioTracks,
            audioDisabled = audioDisabled,
            onDismiss = { showAudioSheet = false }
        )
    }
    if (showSubtitleSheet) {
        SubtitleSheet(
            playerVm = playerVm,
            settingsVm = settingsVm,
            subtitleTracks = subtitleTracks,
            subtitlesDisabled = subtitlesDisabled,
            onLoadFromFile = {
                showSubtitleSheet = false
                subtitlePicker.launch("*/*")
            },
            onDismiss = { showSubtitleSheet = false }
        )
    }
    if (showEqualizer) {
        EqualizerDialog(playerVm = playerVm, onDismiss = { showEqualizer = false })
    }
    if (showVideoInfo) {
        VideoInfoSheet(
            entry = entry,
            video = entry?.let { libraryVm.videoById(it.id) },
            onDismiss = { showVideoInfo = false }
        )
    }
    resumePrompt?.let { prompt ->
        ResumePromptDialog(
            positionMs = prompt.positionMs,
            onResume = { playerVm.confirmResumePrompt(true) },
            onRestart = { playerVm.confirmResumePrompt(false) }
        )
    }
}

@Composable
private fun TextButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.14f else 0.05f))
            .border(
                1.dp,
                Color.White.copy(alpha = if (enabled) 0.3f else 0.12f),
                RoundedCornerShape(24.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.45f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun subtitleColorValue(name: String): Int = when (name) {
    "Yellow" -> android.graphics.Color.YELLOW
    "Cyan" -> android.graphics.Color.CYAN
    "Green" -> android.graphics.Color.GREEN
    "Pink" -> 0xFFF472B6.toInt()
    else -> android.graphics.Color.WHITE
}

private fun subtitleBackgroundValue(name: String): Int = when (name) {
    "Black" -> android.graphics.Color.BLACK
    "Semi-transparent" -> 0x80000000.toInt()
    else -> android.graphics.Color.TRANSPARENT
}

private fun subtitleTypeface(name: String): Typeface = when (name) {
    "Serif" -> Typeface.SERIF
    "Monospace" -> Typeface.MONOSPACE
    else -> Typeface.SANS_SERIF
}

/**
 * Shown when resume mode is "Always ask" and a saved position exists for
 * the video that just started.
 */
@Composable
private fun ResumePromptDialog(
    positionMs: Long,
    onResume: () -> Unit,
    onRestart: () -> Unit
) {
    GlassDialog(onDismiss = onRestart) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Resume playback?",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You stopped watching at ${formatDuration(positionMs)}. " +
                    "Resume from there, or start from the beginning?",
                color = TextPrimary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            AccentGradientButton(
                onClick = onResume,
                text = "Resume from ${formatDuration(positionMs)}",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            GlassButton(
                onClick = onRestart,
                text = "Start from beginning",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
