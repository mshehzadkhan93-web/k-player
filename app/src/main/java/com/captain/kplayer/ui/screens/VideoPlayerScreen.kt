package com.captain.kplayer.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.captain.kplayer.ui.components.GlassCard
import com.captain.kplayer.ui.components.GlassDialog
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.LocalTextPrimary
import com.captain.kplayer.ui.theme.LocalTextSecondary
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.viewmodel.LibraryViewModel
import com.captain.kplayer.viewmodel.PlayerViewModel
import com.captain.kplayer.viewmodel.SettingsViewModel
import com.captain.kplayer.viewmodel.VideoAspect
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/** Gesture overlay kinds shown while the user drags or double-taps. */
private sealed interface GestureOverlay {
    data class Brightness(val percent: Int) : GestureOverlay
    data class Volume(val percent: Int) : GestureOverlay
    data class Seek(val previewMs: Long, val totalMs: Long) : GestureOverlay
    data class DoubleTap(val label: String) : GestureOverlay
    data object Speed2x : GestureOverlay
    /** Live aspect label shown while pinching (Fit / Fill / Zoom). */
    data class Zoom(val label: String) : GestureOverlay
}

/**
 * Fullscreen video player, laid out exactly like the approved mockup:
 * one glass top bar (back, gradient brand, video title, five round glass
 * icons: volume, subtitles, equalizer, aspect, menu), one row of five
 * LARGE frosted-glass transport circles (frosted play button with a
 * gradient triangle, not a solid gradient fill), and one SHORT glass
 * bottom panel (seek row + ONE row of six small pills).
 *
 * Gestures live on the ROOT Box: one pointer-input block owns tap,
 * double-tap, long-press and drag. It uses
 * awaitFirstDown(requireUnconsumed = false) so the gesture layer sees
 * EVERY down — even one a child (or the embedded PlayerView) already
 * consumed — instead of starving when a down arrives pre-consumed.
 * A tap / double-tap / long-press only fires when the down was NOT
 * consumed (so button taps never double-fire); vertical brightness /
 * volume drags always apply; horizontal seek drags are skipped when the
 * down was consumed (the seek Slider owns those).
 * Speed (0.25x-4x sheet), aspect controls, screen lock, background play,
 * picture-in-picture, audio/subtitle sheets, equalizer, playlist and video
 * info are all wired as before. The top-bar menu also holds the sleep
 * timer, volume boost (100-200%), audio delay (-1000..+1000 ms) and the
 * HW / HW+ / SW decoder switch; A-B repeat points are marked with amber
 * ticks on the seek bar.
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
    val aPointMs by playerVm.aPointMs.collectAsState()
    val bPointMs by playerVm.bPointMs.collectAsState()
    val abRepeatEnabled by playerVm.abRepeatEnabled.collectAsState()
    val audioDelayMs by playerVm.audioDelayMs.collectAsState()
    val sleepRemainingMs by playerVm.sleepRemainingMs.collectAsState()
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
    val decodingMode by settingsVm.decodingMode.collectAsState()
    val audioBoost by settingsVm.audioBoost.collectAsState()
    val audioPassthrough by playerVm.audioPassthrough.collectAsState()

    var controlsVisible by remember { mutableStateOf(true) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showVideoInfo by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showBoostDialog by remember { mutableStateOf(false) }
    var showDelayDialog by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
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

    /**
     * The live brightness at this moment: the window value when the app has
     * set one, otherwise the system brightness setting, otherwise the
     * ViewModel value. The window reports -1f while it still follows the
     * system default, so that case must fall through to the system setting
     * instead of being treated as a real brightness.
     */
    fun liveBrightness(): Float {
        val windowValue = activity?.window?.attributes?.screenBrightness ?: -1f
        if (windowValue >= 0f) return windowValue
        val systemValue = try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                -1
            ) / 255f
        } catch (_: Exception) {
            -1f
        }
        return if (systemValue >= 0f) systemValue else playerVm.brightnessPercent.value
    }

    // Gesture layer on the ROOT Box: one pointer-input block owns tap,
    // double-tap, long-press and drag. It uses
    // awaitFirstDown(requireUnconsumed = false) so the layer sees EVERY
    // down — even one a child (button, Slider, or the embedded
    // PlayerView) already consumed — instead of starving when a down
    // arrives pre-consumed. A tap / double-tap / long-press only fires
    // when the down was NOT consumed (so button taps never double-fire);
    // vertical brightness / volume drags always apply; horizontal seek
    // drags are skipped when the down was consumed (the seek Slider owns
    // those).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(
                locked, doubleTapStep, gestureBrightness, gestureVolume,
                gestureSeek, gestureSensitivity
            ) {
                val widthPx = size.width.toFloat()
                val heightPx = size.height.toFloat()
                val touchSlop = viewConfiguration.touchSlop
                val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
                // Pinch-to-zoom helpers: aspect steps go through the
                // ViewModel so the Fit pill and the PlayerView
                // resizeMode stay in sync with pinching.
                fun stepAspectZoomIn() {
                    val next = when (playerVm.aspect.value) {
                        VideoAspect.FIT -> VideoAspect.FILL
                        VideoAspect.FILL -> VideoAspect.ZOOM
                        VideoAspect.ZOOM -> null
                    }
                    if (next != null) {
                        playerVm.setAspect(next)
                        overlay = GestureOverlay.Zoom(aspectLabel(next))
                    }
                }
                fun stepAspectZoomOut() {
                    val next = when (playerVm.aspect.value) {
                        VideoAspect.ZOOM -> VideoAspect.FILL
                        VideoAspect.FILL -> VideoAspect.FIT
                        VideoAspect.FIT -> null
                    }
                    if (next != null) {
                        playerVm.setAspect(next)
                        overlay = GestureOverlay.Zoom(aspectLabel(next))
                    }
                }
                var lastTapUpMs = 0L
                var lastTapX = 0f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downConsumed = down.isConsumed
                    val downPos = down.position
                    val downMs = System.currentTimeMillis()
                    // Baselines are re-synced from LIVE system state
                    // at the start of every gesture — never from a
                    // cached value. That is what used to reset the
                    // volume to 0 after the hardware volume buttons
                    // had been used, and what left the brightness
                    // swipe starting from a stale value.
                    playerVm.refreshVolume()
                    val startVolume = playerVm.volumePercent.value
                    val startBrightness = liveBrightness()
                    val startPosition = playerVm.positionMs.value
                    val isSecondTap = downMs - lastTapUpMs < 300L &&
                        abs(downPos.x - lastTapX) < touchSlop * 4
                    var totalDrag = Offset.Zero
                    var dragKind = 0 // 0 undecided, 1 brightness, 2 volume, 3 seek
                    var seekPreview = startPosition
                    var lastLiveSeekMs = 0L
                    var longPressed = false
                    var finished = false
                    // Pinch-to-zoom state: a second finger switches
                    // the gesture to pinch mode; one-finger drag logic
                    // is ignored until both fingers lift.
                    var pinchId: PointerId? = null
                    var pinchActive = false
                    var pinched = false
                    var pinchDone = false
                    var firstPos = downPos
                    var secondPos: Offset? = null
                    var lastPinchDistance: Float? = null
                    var pinchAccumPx = 0f
                    // Cumulative finger-spread change per aspect step.
                    val pinchStepPx = 120f
                    while (!finished) {
                        val remainingLongPress =
                            longPressTimeoutMs - (System.currentTimeMillis() - downMs)
                        val event = if (!longPressed && !pinchActive && dragKind == 0 &&
                            totalDrag == Offset.Zero && remainingLongPress > 0
                        ) {
                            // Finger still down, no movement yet:
                            // wait only until the long-press timeout.
                            withTimeoutOrNull(remainingLongPress) {
                                awaitPointerEvent()
                            }
                        } else {
                            awaitPointerEvent()
                        }
                        if (event == null) {
                            // Held still past the long-press timeout.
                            if (!locked && !downConsumed && !pinchActive) {
                                playerVm.setSpeedTemporarily(2f)
                                overlay = GestureOverlay.Speed2x
                                longPressed = true
                            }
                            continue
                        }
                        for (change in event.changes) {
                            // --- Second finger: pinch-to-zoom. ---
                            // A new pointer going down while the first
                            // finger is held switches to pinch mode.
                            // Two fingers only: a one-finger swipe can
                            // never enter this branch.
                            if (change.id != down.id && change.pressed &&
                                pinchId == null && !pinchDone &&
                                !locked && !downConsumed
                            ) {
                                pinchId = change.id
                                pinchActive = true
                                pinched = true
                                secondPos = change.position
                                lastPinchDistance = null
                                pinchAccumPx = 0f
                                // Abandon any one-finger drag already in
                                // progress, and restore 2x if a
                                // long-press had started it.
                                if (longPressed) {
                                    playerVm.restoreSpeed()
                                    longPressed = false
                                }
                                dragKind = 0
                                totalDrag = Offset.Zero
                                overlay = GestureOverlay.Zoom(
                                    aspectLabel(playerVm.aspect.value)
                                )
                                continue
                            }
                            if (change.id == pinchId) {
                                // Second finger lifted: the pinch ends.
                                // The first finger may still be down,
                                // but its drags stay ignored until it
                                // lifts too.
                                if (!change.pressed) {
                                    pinchId = null
                                    pinchActive = false
                                    pinchDone = true
                                    secondPos = null
                                    lastPinchDistance = null
                                    pinchAccumPx = 0f
                                    overlay = null
                                } else {
                                    secondPos = change.position
                                }
                                continue
                            }
                            if (change.id != down.id) continue
                            // Finger lifted or gesture cancelled:
                            // after the initial down, pressed is
                            // false on both release and cancel.
                            if (!change.pressed) {
                                finished = true
                                break
                            }
                            firstPos = change.position
                            // While pinching (or after a pinch, until
                            // both fingers lift), one-finger drag logic
                            // is ignored entirely.
                            if (pinchActive || pinchDone) continue
                            val delta = change.positionChange()
                            if (delta != Offset.Zero) {
                                totalDrag += delta
                                if (dragKind == 0 && !longPressed) {
                                    if (abs(totalDrag.x) > touchSlop ||
                                        abs(totalDrag.y) > touchSlop
                                    ) {
                                        dragKind = when {
                                            abs(totalDrag.x) > abs(totalDrag.y) ->
                                                if (gestureSeek && !downConsumed) 3 else 0
                                            change.position.x < widthPx / 2f ->
                                                if (gestureBrightness) 1 else 0
                                            else ->
                                                if (gestureVolume) 2 else 0
                                        }
                                    }
                                }
                                // Only consume once this layer owns the
                                // drag: consuming earlier would cancel
                                // the seek Slider's own slop detection
                                // (it aborts when a change arrives
                                // already consumed).
                                if (dragKind != 0) change.consume()
                                if (!locked) {
                                    when (dragKind) {
                                        1 -> {
                                            // Left-half vertical drag.
                                            val next = (startBrightness +
                                                -totalDrag.y / heightPx *
                                                gestureSensitivity)
                                                .coerceIn(0.05f, 1f)
                                            playerVm.setBrightnessPercent(next)
                                            overlay = GestureOverlay.Brightness(
                                                (next * 100).toInt()
                                            )
                                        }
                                        2 -> {
                                            // Right-half vertical drag.
                                            val next = (startVolume +
                                                (-totalDrag.y / heightPx * 100f *
                                                    gestureSensitivity).toInt())
                                                .coerceIn(0, 100)
                                            playerVm.setVolumePercent(next)
                                            overlay = GestureOverlay.Volume(next)
                                        }
                                        3 -> {
                                            // Horizontal drag: live
                                            // preview + throttled
                                            // live seeks, settling
                                            // on release.
                                            val total = playerVm.durationMs.value
                                            seekPreview = (startPosition +
                                                (totalDrag.x / widthPx * total *
                                                    gestureSensitivity).toLong())
                                                .coerceIn(0L, total.coerceAtLeast(0L))
                                            overlay = GestureOverlay.Seek(
                                                seekPreview, total
                                            )
                                            val now = System.currentTimeMillis()
                                            if (now - lastLiveSeekMs > 150L) {
                                                lastLiveSeekMs = now
                                                playerVm.seekTo(seekPreview)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Pinch distance for this event: step the aspect
                        // on cumulative spread change (out = zoom in,
                        // in = zoom out). The first frame only sets the
                        // baseline, so a pinch never jumps a step on
                        // its first movement.
                        if (pinchActive) {
                            val second = secondPos
                            if (second != null) {
                                val distance = (firstPos - second).getDistance()
                                val last = lastPinchDistance
                                if (last != null) {
                                    pinchAccumPx += distance - last
                                    while (pinchAccumPx >= pinchStepPx) {
                                        pinchAccumPx -= pinchStepPx
                                        stepAspectZoomIn()
                                    }
                                    while (pinchAccumPx <= -pinchStepPx) {
                                        pinchAccumPx += pinchStepPx
                                        stepAspectZoomOut()
                                    }
                                }
                                lastPinchDistance = distance
                            }
                        }
                    }
                    // Finger up (or gesture cancelled): settle. Tap,
                    // double-tap and long-press only fire when the down
                    // was NOT consumed (a button/slider owned it).
                    if (pinched) {
                        // A pinch is never a tap / double-tap / seek
                        // settle — just clear the zoom bubble.
                        overlay = null
                    } else if (longPressed) {
                        playerVm.restoreSpeed()
                        overlay = null
                    } else if (!locked && !downConsumed) {
                        when {
                            dragKind == 3 -> {
                                playerVm.seekTo(seekPreview)
                                overlay = null
                            }
                            dragKind == 1 || dragKind == 2 -> overlay = null
                            isSecondTap -> {
                                val stepMs = doubleTapStep * 1000L
                                if (downPos.x < widthPx / 2f) {
                                    playerVm.seekRelative(-stepMs)
                                    overlay = GestureOverlay.DoubleTap("-${doubleTapStep}s")
                                } else {
                                    playerVm.seekRelative(stepMs)
                                    overlay = GestureOverlay.DoubleTap("+${doubleTapStep}s")
                                }
                                lastTapUpMs = System.currentTimeMillis()
                                lastTapX = downPos.x
                            }
                            else -> {
                                controlsVisible = !controlsVisible
                                lastTapUpMs = System.currentTimeMillis()
                                lastTapX = downPos.x
                            }
                        }
                    } else if (!locked && (dragKind == 1 || dragKind == 2)) {
                        // A brightness/volume drag that started on a
                        // control: hide its bubble on release.
                        overlay = null
                    }
                }
            }
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
                    is GestureOverlay.Zoom -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AspectRatio, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(current.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
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

        // Glass controls: one top panel, one center transport row, one
        // bottom panel. Fade in/out over ~300ms; the center row also
        // scales from 0.92 so it never pops in abruptly.
        AnimatedVisibility(
            visible = controlsVisible && !locked,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar: a single glass panel — back, gradient brand,
                // video title, round glass icon buttons.
                GlassCard(
                    radius = 24.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassCircleButton(
                            size = 44.dp,
                            icon = Icons.Rounded.ArrowBack,
                            description = "Back",
                            iconSize = 24.dp
                        ) {
                            playerVm.onLeavePlayer()
                            navController.popBackStack()
                        }
                        Text(
                            text = "K-Player",
                            style = TextStyle(
                                brush = LocalAccent.current.brush,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            modifier = Modifier.padding(start = 10.dp)
                        )
                        Text(
                            text = entry?.title.orEmpty(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        )
                        // Five icons, like the mockup: volume, subtitles,
                        // equalizer, aspect, menu. Everything else lives in
                        // the menu so the bar stays one clean row.
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlassCircleButton(
                                size = 40.dp,
                                icon = Icons.Rounded.VolumeUp,
                                description = "Audio track",
                                iconSize = 20.dp
                            ) { showAudioSheet = true }
                            GlassCircleButton(
                                size = 40.dp,
                                icon = Icons.Rounded.Subtitles,
                                description = "Subtitles",
                                iconSize = 20.dp
                            ) { showSubtitleSheet = true }
                            GlassCircleButton(
                                size = 40.dp,
                                icon = Icons.Rounded.Equalizer,
                                description = "Equalizer",
                                iconSize = 20.dp
                            ) { showEqualizer = true }
                            GlassCircleButton(
                                size = 40.dp,
                                icon = Icons.Rounded.AspectRatio,
                                description = "Aspect ratio",
                                iconSize = 20.dp
                            ) { playerVm.cycleAspect() }
                            Box {
                                GlassCircleButton(
                                    size = 40.dp,
                                    icon = Icons.Rounded.MoreVert,
                                    description = "More",
                                    iconSize = 20.dp
                                ) { showTopMenu = true }
                                DropdownMenu(
                                    expanded = showTopMenu,
                                    onDismissRequest = { showTopMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Add subtitle file") },
                                        onClick = {
                                            showTopMenu = false
                                            subtitlePicker.launch("*/*")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Picture-in-picture") },
                                        onClick = {
                                            showTopMenu = false
                                            if (pipEnabled) {
                                                enterPip()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Enable PiP in Settings > Playback",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rotate screen") },
                                        onClick = { showTopMenu = false; rotate() }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (backgroundPlay) "Background play: On"
                                                else "Background play: Off"
                                            )
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            settingsVm.setBackgroundPlay(!backgroundPlay)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Video info") },
                                        onClick = { showTopMenu = false; showVideoInfo = true }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when (abState) {
                                                    PlayerViewModel.ABState.OFF -> "A-B repeat: Off"
                                                    PlayerViewModel.ABState.A_SET -> "A-B repeat: A set…"
                                                    PlayerViewModel.ABState.LOOPING -> "A-B repeat: Looping"
                                                }
                                            )
                                        },
                                        onClick = {
                                            showTopMenu = false
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
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                sleepRemainingMs?.let { "Sleep timer: ${formatDuration(it)} left" }
                                                    ?: "Sleep timer: Off"
                                            )
                                        },
                                        onClick = { showTopMenu = false; showSleepDialog = true }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Volume boost: ${(audioBoost * 100).toInt()}%") },
                                        onClick = { showTopMenu = false; showBoostDialog = true }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when {
                                                    audioDelayMs > 0 -> "Audio delay: +${audioDelayMs} ms"
                                                    audioDelayMs < 0 -> "Audio delay: ${audioDelayMs} ms"
                                                    else -> "Audio delay: 0 ms"
                                                }
                                            )
                                        },
                                        onClick = { showTopMenu = false; showDelayDialog = true }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Decoder: HW" +
                                                    if (decodingMode == "hw") " (current)" else ""
                                            )
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            settingsVm.setDecodingMode("hw")
                                            Toast.makeText(
                                                context,
                                                "Decoder: HW - player restarting...",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Decoder: HW+" +
                                                    if (decodingMode == "hw_plus") " (current)" else ""
                                            )
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            settingsVm.setDecodingMode("hw_plus")
                                            Toast.makeText(
                                                context,
                                                "Decoder: HW+ - player restarting...",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Decoder: SW" +
                                                    if (decodingMode == "sw") " (current)" else ""
                                            )
                                        },
                                        onClick = {
                                            showTopMenu = false
                                            settingsVm.setDecodingMode("sw")
                                            Toast.makeText(
                                                context,
                                                "Decoder: SW - player restarting...",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Playlist") },
                                        onClick = { showTopMenu = false; onOpenPlaylist() }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Audio track") },
                                        onClick = { showTopMenu = false; showAudioSheet = true }
                                    )
                                }
                            }
                        }
                    }
                }

                // Center transport: one row of five LARGE frosted-glass
                // circles, evenly spread across the full width, bright
                // white borders — exactly like the mockup. The play
                // button is frosted glass with a gradient triangle,
                // NOT a solid gradient-filled circle.
                AnimatedVisibility(
                    visible = controlsVisible && !locked,
                    enter = fadeIn(animationSpec = tween(300)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) +
                        scaleOut(targetScale = 0.92f, animationSpec = tween(300)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassCircleButton(
                            size = 84.dp,
                            icon = Icons.Rounded.SkipPrevious,
                            description = "Previous",
                            iconSize = 40.dp
                        ) { playerVm.playPrevious() }
                        GlassCircleButton(
                            size = 84.dp,
                            icon = Icons.Rounded.Replay10,
                            description = "Back ${doubleTapStep} seconds",
                            iconSize = 40.dp
                        ) { playerVm.seekRelative(-doubleTapStep * 1000L) }
                        // Play / pause: the biggest circle, frosted glass
                        // with a bright border and a soft accent glow.
                        // Paused shows a gradient triangle; playing shows
                        // the white pause icon.
                        Box(
                            modifier = Modifier
                                .size(112.dp)
                                .shadow(
                                    16.dp,
                                    CircleShape,
                                    ambientColor = LocalAccent.current.start.copy(alpha = 0.55f),
                                    spotColor = LocalAccent.current.start.copy(alpha = 0.55f)
                                )
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                .border(
                                    1.5.dp,
                                    Color.White.copy(alpha = 0.75f),
                                    CircleShape
                                )
                                .clip(CircleShape)
                                .clickable { playerVm.togglePlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPlaying) {
                                Icon(
                                    imageVector = Icons.Rounded.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(52.dp)
                                )
                            } else {
                                GradientTriangle(modifier = Modifier.size(52.dp))
                            }
                        }
                        GlassCircleButton(
                            size = 84.dp,
                            icon = Icons.Rounded.Forward10,
                            description = "Forward ${doubleTapStep} seconds",
                            iconSize = 40.dp
                        ) { playerVm.seekRelative(doubleTapStep * 1000L) }
                        GlassCircleButton(
                            size = 84.dp,
                            icon = Icons.Rounded.SkipNext,
                            description = "Next",
                            iconSize = 40.dp
                        ) { playerVm.playNext() }
                    }
                }

                // Bottom panel: a single glass panel holding the seek row
                // and one row of six essential pills.
                GlassCard(
                    radius = 24.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    val seekFraction = scrubFraction
                        ?: if (duration > 0) position.toFloat() / duration.toFloat() else 0f
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        // A-B repeat: live status + clear, shown only while
                        // point A is set. The A/B ticks on the seek bar
                        // below show exactly where the loop runs.
                        if (abState != PlayerViewModel.ABState.OFF) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (abState) {
                                        PlayerViewModel.ABState.A_SET ->
                                            "A-B: A at ${formatDuration(aPointMs)} - set B from the menu"
                                        PlayerViewModel.ABState.LOOPING ->
                                            "A-B loop: ${formatDuration(aPointMs)} -> ${formatDuration(bPointMs)}"
                                        else -> ""
                                    },
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Clear",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { playerVm.clearABRepeat() }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatDuration(
                                    scrubFraction?.let { (it * duration).toLong() } ?: position
                                ),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            // Gradient accent track + white thumb. The stock
                            // Slider cannot draw a gradient track, so its
                            // own track is transparent and a gradient bar is
                            // drawn underneath it.
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxWidth(seekFraction.coerceIn(0f, 1f))
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(LocalAccent.current.brush)
                                )
                                // A-B markers: amber ticks at A and (while
                                // looping) B, plus a translucent range
                                // highlight between them. They sit under the
                                // Slider, whose own track is transparent.
                                if (abState != PlayerViewModel.ABState.OFF && duration > 0) {
                                    val aFrac = (aPointMs.toFloat() / duration).coerceIn(0f, 1f)
                                    val bFrac = (bPointMs.toFloat() / duration).coerceIn(0f, 1f)
                                    if (abState == PlayerViewModel.ABState.LOOPING && bFrac > aFrac) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .fillMaxWidth(bFrac)
                                                .height(5.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth((bFrac - aFrac) / bFrac)
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(
                                                        Color(0xFFFFD54F).copy(alpha = 0.45f)
                                                    )
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxWidth(aFrac)
                                            .height(12.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .fillMaxHeight()
                                                .background(
                                                    Color(0xFFFFD54F),
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                    if (abState == PlayerViewModel.ABState.LOOPING) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .fillMaxWidth(bFrac)
                                                .height(12.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .fillMaxHeight()
                                                    .background(
                                                        Color(0xFFFFD54F),
                                                        RoundedCornerShape(2.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                                Slider(
                                    value = seekFraction,
                                    onValueChange = {
                                        scrubFraction = it
                                        // Live time preview bubble + smooth
                                        // scrub — the video follows the thumb
                                        // (throttled live seeks), settling
                                        // on release.
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
                                        activeTrackColor = Color.Transparent,
                                        inactiveTrackColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Text(
                                text = formatDuration(duration),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        // ONE row of six small pills, essentials only:
                        // prev, play/pause, next, aspect (Fit), speed,
                        // lock. The -10s/+10s pills live on the center
                        // row and double-tap; A-B, playlist, audio track
                        // and rotate live in the top bar ⋯ menu, and
                        // background play is in that menu too.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GlassPillButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.SkipPrevious,
                                onClick = { playerVm.playPrevious() }
                            )
                            GlassPillButton(
                                modifier = Modifier.weight(1f),
                                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                onClick = { playerVm.togglePlay() }
                            )
                            GlassPillButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.SkipNext,
                                onClick = { playerVm.playNext() }
                            )
                            GlassPillButton(
                                modifier = Modifier.weight(1f),
                                text = aspectLabel(aspect),
                                onClick = { playerVm.cycleAspect() }
                            )
                            GlassPillButton(
                                modifier = Modifier.weight(1f),
                                text = if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x",
                                onClick = { showSpeedMenu = true }
                            )
                            GlassPillButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.LockOpen,
                                onClick = { playerVm.setLocked(true) }
                            )
                        }
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
    if (showSpeedMenu) {
        SpeedSheetDialog(
            speed = speed,
            options = playerVm.speedOptions,
            onSelect = { playerVm.setSpeed(it); showSpeedMenu = false },
            onDismiss = { showSpeedMenu = false }
        )
    }
    if (showSleepDialog) {
        SleepTimerDialog(
            remainingMs = sleepRemainingMs,
            onSet = { minutes -> playerVm.startSleepTimer(minutes); showSleepDialog = false },
            onDismiss = { showSleepDialog = false }
        )
    }
    if (showBoostDialog) {
        VolumeBoostDialog(
            boost = audioBoost,
            passthrough = audioPassthrough,
            onBoostChange = { playerVm.setAudioBoost(it) },
            onDismiss = { showBoostDialog = false }
        )
    }
    if (showDelayDialog) {
        AudioDelayDialog(
            delayMs = audioDelayMs,
            onDelayChange = { playerVm.setAudioDelayMs(it) },
            onDismiss = { showDelayDialog = false }
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

/** Uniform circular glass button (top-bar icons and center transport). */
@Composable
private fun GlassCircleButton(
    size: Dp,
    icon: ImageVector,
    description: String,
    iconSize: Dp = 28.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f))
            .background(Color.White.copy(alpha = 0.12f), CircleShape)
            .border(1.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Small compact glass pill — the six pills in the bottom panel, one row. */
@Composable
private fun GlassPillButton(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .background(
                Color.White.copy(alpha = if (enabled) 0.12f else 0.05f),
                RoundedCornerShape(18.dp)
            )
            .border(
                1.dp,
                Color.White.copy(alpha = if (enabled) 0.4f else 0.12f),
                RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp)
                )
            }
            if (text != null) {
                Text(
                    text = text,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * The play triangle from the mockup: a right-pointing triangle filled
 * with the theme's accent gradient, drawn on the frosted-glass play
 * button. (A plain white Icon cannot carry the gradient.)
 */
@Composable
private fun GradientTriangle(modifier: Modifier = Modifier) {
    val brush = LocalAccent.current.brush
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.30f, h * 0.20f)
            lineTo(w * 0.30f, h * 0.80f)
            lineTo(w * 0.78f, h * 0.50f)
            close()
        }
        drawPath(path = path, brush = brush)
    }
}

/** Short label for the current aspect, shared by the Fit pill and the pinch-zoom overlay. */
private fun aspectLabel(aspect: VideoAspect): String = when (aspect) {
    VideoAspect.FIT -> "Fit"
    VideoAspect.FILL -> "Fill"
    VideoAspect.ZOOM -> "Zoom"
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
 * MX-style speed sheet: all presets 0.25x-4x with the current speed
 * checked. Long-pressing the video still gives temporary 2x.
 */
@Composable
private fun SpeedSheetDialog(
    speed: Float,
    options: List<Float>,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Playback speed",
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (option % 1f == 0f) "${option.toInt()}x" else "${option}x",
                        color = LocalTextPrimary.current,
                        fontSize = 16.sp,
                        fontWeight = if (option == speed) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (option == speed) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = LocalAccent.current.start,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Long-press the video any time for temporary 2x speed.",
                color = LocalTextSecondary.current,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Sleep timer: presets up to 60 minutes, or Off to cancel. The timer runs
 * in the ViewModel (a countdown that pauses playback at zero) and keeps
 * running with the screen off while the app process is alive.
 */
@Composable
private fun SleepTimerDialog(
    remainingMs: Long?,
    onSet: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Sleep timer",
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            remainingMs?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Stops playback in ${formatDuration(it)}.",
                    color = LocalTextSecondary.current,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPillButton(modifier = Modifier.weight(1f), text = "Off") { onSet(0) }
                GlassPillButton(modifier = Modifier.weight(1f), text = "5 min") { onSet(5) }
                GlassPillButton(modifier = Modifier.weight(1f), text = "10 min") { onSet(10) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPillButton(modifier = Modifier.weight(1f), text = "15 min") { onSet(15) }
                GlassPillButton(modifier = Modifier.weight(1f), text = "30 min") { onSet(30) }
                GlassPillButton(modifier = Modifier.weight(1f), text = "60 min") { onSet(60) }
            }
        }
    }
}

/**
 * Volume boost, 100-200%, via the LoudnessEnhancer in EqualizerManager
 * (persisted as the audioBoost setting; default 100% = no boost, so
 * nothing distorts unless the user asks for it). While audio passthrough
 * is on the boost is bypassed and this slider does nothing.
 */
@Composable
private fun VolumeBoostDialog(
    boost: Float,
    passthrough: Boolean,
    onBoostChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Volume boost",
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${(boost * 100).toInt()}%",
                color = LocalTextPrimary.current,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = boost,
                onValueChange = onBoostChange,
                valueRange = 1f..2f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = LocalAccent.current.start,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (passthrough) {
                    "Audio passthrough is on - boost is bypassed while it is enabled."
                } else {
                    "Boosts quiet audio up to 200%. Default is 100% (no boost)."
                },
                color = LocalTextSecondary.current,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Audio delay (A/V sync), -1000..+1000 ms in 50 ms steps. Positive =
 * audio plays later (use when the audio is ahead of the video). Applied
 * live by the PCM delay processor in PlayerFactory; PCM audio only -
 * bitstream passthrough is not delayed.
 */
@Composable
private fun AudioDelayDialog(
    delayMs: Long,
    onDelayChange: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Audio delay",
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when {
                    delayMs > 0 -> "+${delayMs} ms - audio plays later"
                    delayMs < 0 -> "${delayMs} ms - audio plays earlier"
                    else -> "0 ms - in sync"
                },
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPillButton(
                    modifier = Modifier.weight(1f),
                    text = "-50 ms",
                    onClick = { onDelayChange(delayMs - 50) }
                )
                GlassPillButton(
                    modifier = Modifier.weight(1f),
                    text = "+50 ms",
                    onClick = { onDelayChange(delayMs + 50) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            GlassButton(
                onClick = { onDelayChange(0) },
                text = "Reset to 0 ms",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Range -1000 to +1000 ms. Applies to PCM audio only; " +
                    "bitstream passthrough is not delayed.",
                color = LocalTextSecondary.current,
                fontSize = 13.sp
            )
        }
    }
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
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You stopped watching at ${formatDuration(positionMs)}. " +
                    "Resume from there, or start from the beginning?",
                color = LocalTextPrimary.current,
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
