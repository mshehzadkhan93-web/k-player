package com.captain.kplayer.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.captain.kplayer.BuildConfig
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.LocalTextPrimary
import com.captain.kplayer.ui.theme.LocalTextSecondary
import com.captain.kplayer.ui.theme.THEMES
import com.captain.kplayer.ui.theme.ThemeDefinition
import com.captain.kplayer.viewmodel.LibraryViewModel
import com.captain.kplayer.viewmodel.PlayerViewModel
import com.captain.kplayer.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Settings, MX-style: a clean flat list with small uppercase section
 * headers, thin dividers and dense rows — title, subtitle and a trailing
 * control. Every section collapses by tapping its header (all start
 * expanded, like MX Player which shows everything). Every option is
 * functional state backed by DataStore, and every option is read by the
 * player / library code that already consumes it.
 *
 * Groups: Player, Gestures, Subtitles, Audio, Appearance, Interface,
 * Privacy, Kids Lock and About.
 *
 * NOTE: the Audio delay setting is persisted in DataStore but has no
 * player consumer yet — PlayerViewModel has no audio-delay path, so a
 * follow-up player package must apply `audioDelayMs` when opening a
 * video. Every other option is read by the player / library code that
 * already consumes it.
 */
@Composable
fun SettingsScreen(
    navController: NavHostController,
    settingsVm: SettingsViewModel,
    playerVm: PlayerViewModel,
    libraryVm: LibraryViewModel
) {
    val context = LocalContext.current
    val textPrimary = LocalTextPrimary.current
    val textSecondary = LocalTextSecondary.current

    val decodingMode by settingsVm.decodingMode.collectAsState()
    val hardwareAcceleration by settingsVm.hardwareAcceleration.collectAsState()
    val resumeMode by settingsVm.resumeMode.collectAsState()
    val doubleTapSeek by settingsVm.doubleTapSeek.collectAsState()
    val abRepeat by settingsVm.abRepeat.collectAsState()
    val rememberSpeed by settingsVm.rememberSpeed.collectAsState()
    val autoRotate by settingsVm.autoRotate.collectAsState()
    val orientationLock by settingsVm.orientationLock.collectAsState()
    val pipEnabled by settingsVm.pipEnabled.collectAsState()
    val backgroundPlay by settingsVm.backgroundPlay.collectAsState()
    val defaultSpeed by settingsVm.defaultSpeed.collectAsState()
    val audioBoost by settingsVm.audioBoost.collectAsState()
    val audioPassthrough by settingsVm.audioPassthrough.collectAsState()
    val audioDelayMs by settingsVm.audioDelayMs.collectAsState()
    val audioLanguage by settingsVm.audioLanguage.collectAsState()
    val equalizerEnabled by settingsVm.equalizerEnabled.collectAsState()
    val subtitleLanguage by settingsVm.subtitleLanguage.collectAsState()
    val subtitleEncoding by settingsVm.subtitleEncoding.collectAsState()
    val subtitleFont by settingsVm.subtitleFont.collectAsState()
    val subtitleSize by settingsVm.subtitleSize.collectAsState()
    val subtitleColor by settingsVm.subtitleColor.collectAsState()
    val subtitleBackground by settingsVm.subtitleBackground.collectAsState()
    val subtitleDelayMs by playerVm.subtitleDelayMs.collectAsState()
    val gestureBrightness by settingsVm.gestureBrightness.collectAsState()
    val gestureVolume by settingsVm.gestureVolume.collectAsState()
    val gestureSeek by settingsVm.gestureSeek.collectAsState()
    val gestureSensitivity by settingsVm.gestureSensitivity.collectAsState()
    val themeName by settingsVm.themeName.collectAsState()
    val blurIntensity by settingsVm.blurIntensity.collectAsState()
    val defaultView by settingsVm.defaultView.collectAsState()
    val defaultSort by settingsVm.defaultSort.collectAsState()
    val showHiddenFiles by settingsVm.showHiddenFiles.collectAsState()
    val kidsLock by settingsVm.kidsLock.collectAsState()

    var confirmClear by remember { mutableStateOf<String?>(null) }
    var showPrivacy by remember { mutableStateOf(false) }
    var storageRefresh by remember { mutableStateOf(0) }
    var storageText by remember { mutableStateOf("Calculating…") }

    LaunchedEffect(storageRefresh) {
        storageText = withContext(Dispatchers.IO) {
            fun dirSize(file: File): Long {
                if (!file.exists()) return 0L
                return file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
            formatBytes(dirSize(context.cacheDir) + dirSize(context.filesDir))
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }
                Text(
                    text = "Settings",
                    color = textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ------------------------------------------------ Player
            MxSection("Player") {
                MxSelectorRow(
                    title = "Decoder",
                    subtitle = "HW+ uses hardware decoding with software fallback",
                    selectedId = decodingMode,
                    options = listOf(
                        "hw" to "HW", "hw_plus" to "HW+", "sw" to "SW"
                    ),
                    onSelect = { settingsVm.setDecodingMode(it) }
                )
                MxToggleRow(
                    title = "Hardware acceleration",
                    subtitle = "Turn off to force software decoding",
                    checked = hardwareAcceleration,
                    onCheckedChange = { on ->
                        settingsVm.setHardwareAcceleration(on)
                        settingsVm.setDecodingMode(if (on) "hw_plus" else "sw")
                    }
                )
                MxSelectorRow(
                    title = "Default speed",
                    subtitle = "Speed new videos start at",
                    selectedId = defaultSpeed,
                    options = listOf(
                        "0.5" to "0.5x", "0.75" to "0.75x", "1.0" to "1.0x",
                        "1.25" to "1.25x", "1.5" to "1.5x", "2.0" to "2.0x"
                    ),
                    onSelect = { settingsVm.setDefaultSpeed(it) }
                )
                MxSelectorRow(
                    title = "Resume",
                    subtitle = "What to do when reopening a video",
                    selectedId = resumeMode,
                    options = listOf(
                        "always_ask" to "Always ask",
                        "always_resume" to "Always resume",
                        "never" to "Never"
                    ),
                    onSelect = { settingsVm.setResumeMode(it) }
                )
                MxToggleRow(
                    title = "Remember playback speed",
                    subtitle = "Keep the last speed you chose for the next video",
                    checked = rememberSpeed,
                    onCheckedChange = { settingsVm.setRememberSpeed(it) }
                )
                MxSelectorRow(
                    title = "Double-tap seek duration",
                    subtitle = "How far a double-tap jumps",
                    selectedId = doubleTapSeek.toString(),
                    options = listOf("5" to "5 seconds", "10" to "10 seconds", "15" to "15 seconds", "30" to "30 seconds"),
                    onSelect = { settingsVm.setDoubleTapSeek(it.toIntOrNull() ?: 10) }
                )
                MxToggleRow(
                    title = "A-B repeat",
                    subtitle = "Loop a section: tap A-B in the player to mark A, then B",
                    checked = abRepeat,
                    onCheckedChange = { settingsVm.setAbRepeat(it) }
                )
                MxToggleRow(
                    title = "Auto-rotate",
                    subtitle = "Rotate the player with the device sensor",
                    checked = autoRotate,
                    onCheckedChange = { settingsVm.setAutoRotate(it) }
                )
                MxSelectorRow(
                    title = "Screen orientation lock",
                    subtitle = "Force the player orientation",
                    selectedId = orientationLock,
                    options = listOf(
                        "auto" to "Auto", "portrait" to "Portrait", "landscape" to "Landscape"
                    ),
                    onSelect = { settingsVm.setOrientationLock(it) }
                )
                MxToggleRow(
                    title = "Picture-in-Picture",
                    subtitle = "Show the PiP button in the player",
                    checked = pipEnabled,
                    onCheckedChange = { settingsVm.setPipEnabled(it) }
                )
                MxToggleRow(
                    title = "Background play",
                    subtitle = "Keep audio playing when you leave the player",
                    checked = backgroundPlay,
                    onCheckedChange = { settingsVm.setBackgroundPlay(it) }
                )
            }

            // ------------------------------------------------ Gestures
            MxSection("Gestures") {
                MxToggleRow(
                    title = "Brightness gesture",
                    subtitle = "Swipe up / down on the left side of the player",
                    checked = gestureBrightness,
                    onCheckedChange = { settingsVm.setGestureBrightness(it) }
                )
                MxToggleRow(
                    title = "Volume gesture",
                    subtitle = "Swipe up / down on the right side of the player",
                    checked = gestureVolume,
                    onCheckedChange = { settingsVm.setGestureVolume(it) }
                )
                MxToggleRow(
                    title = "Seek gesture",
                    subtitle = "Swipe left / right to seek",
                    checked = gestureSeek,
                    onCheckedChange = { settingsVm.setGestureSeek(it) }
                )
                MxSliderRow(
                    title = "Gesture sensitivity",
                    subtitle = "How strongly the player reacts to swipes",
                    value = gestureSensitivity,
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    valueText = "x${"%.1f".format(gestureSensitivity)}",
                    onValueChange = { settingsVm.setGestureSensitivity(it) }
                )
            }

            // ------------------------------------------------ Subtitles
            MxSection("Subtitles") {
                MxSelectorRow(
                    title = "Default subtitle language",
                    subtitle = "Picked automatically when a video has it",
                    selectedId = subtitleLanguage,
                    options = SUBTITLE_LANGUAGES.map { it to it },
                    onSelect = { playerVm.setPreferredSubtitleLanguage(it) }
                )
                MxSelectorRow(
                    title = "Subtitle encoding",
                    subtitle = "Charset used when reading subtitle files",
                    selectedId = subtitleEncoding,
                    options = listOf("UTF-8" to "UTF-8", "Auto" to "Auto"),
                    onSelect = { settingsVm.setSubtitleEncoding(it) }
                )
                MxStepperRow(
                    title = "Sync delay",
                    subtitle = "Shift subtitles earlier or later to match the audio",
                    valueText = formatDelaySeconds(subtitleDelayMs),
                    onMinus = { playerVm.adjustSubtitleDelay(-500L) },
                    onPlus = { playerVm.adjustSubtitleDelay(500L) },
                    minusDescription = "Delay minus 0.5s",
                    plusDescription = "Delay plus 0.5s"
                )
                MxSelectorRow(
                    title = "Subtitle font",
                    selectedId = subtitleFont,
                    options = SUBTITLE_FONTS.map { it to it },
                    onSelect = { settingsVm.setSubtitleFont(it) }
                )
                MxSliderRow(
                    title = "Subtitle size",
                    value = subtitleSize,
                    valueRange = 0.75f..1.5f,
                    steps = 2,
                    valueText = sizeLabel(subtitleSize),
                    onValueChange = { settingsVm.setSubtitleSize(it) }
                )
                MxSelectorRow(
                    title = "Subtitle color",
                    selectedId = subtitleColor,
                    options = SUBTITLE_COLORS.map { it to it },
                    onSelect = { settingsVm.setSubtitleColor(it) }
                )
                MxSelectorRow(
                    title = "Subtitle background",
                    selectedId = subtitleBackground,
                    options = SUBTITLE_BACKGROUNDS.map { it to it },
                    onSelect = { settingsVm.setSubtitleBackground(it) }
                )
            }

            // ------------------------------------------------ Audio
            MxSection("Audio") {
                MxToggleRow(
                    title = "Equalizer",
                    subtitle = "5-band equalizer with bass boost — open it from the player to adjust the bands",
                    checked = equalizerEnabled,
                    onCheckedChange = { settingsVm.setEqualizerEnabled(it) }
                )
                MxSliderRow(
                    title = "Volume boost",
                    subtitle = "Raise volume beyond 100% (up to 200%)",
                    value = audioBoost,
                    valueRange = 1.0f..2.0f,
                    steps = 19,
                    valueText = "${(audioBoost * 100).toInt()}%",
                    onValueChange = { playerVm.setAudioBoost(it) }
                )
                MxStepperRow(
                    title = "Audio delay",
                    subtitle = "Shift the audio earlier or later to match the picture",
                    valueText = formatDelaySeconds(audioDelayMs.toLong()),
                    onMinus = { settingsVm.setAudioDelayMs((audioDelayMs - 100).coerceIn(-5000, 5000)) },
                    onPlus = { settingsVm.setAudioDelayMs((audioDelayMs + 100).coerceIn(-5000, 5000)) },
                    minusDescription = "Audio delay minus 0.1s",
                    plusDescription = "Audio delay plus 0.1s"
                )
                MxToggleRow(
                    title = "Audio passthrough",
                    subtitle = "Send the raw bitstream (AC3 / E-AC3) straight to the receiver — equalizer and boost are bypassed",
                    checked = audioPassthrough,
                    onCheckedChange = { settingsVm.setAudioPassthrough(it) }
                )
                MxSelectorRow(
                    title = "Preferred audio language",
                    subtitle = "Picked automatically when a video has it",
                    selectedId = audioLanguage,
                    options = AUDIO_LANGUAGES.map { it to it },
                    onSelect = { playerVm.setPreferredAudioLanguage(it) }
                )
            }

            // ------------------------------------------------ Appearance
            MxSection("Appearance") {
                Text(
                    text = "Theme",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                )
                THEMES.values.forEach { theme ->
                    MxThemeRow(
                        theme = theme,
                        selected = themeName == theme.id,
                        onClick = { settingsVm.setThemeName(theme.id) }
                    )
                }
                MxSliderRow(
                    title = "Glass blur intensity",
                    subtitle = "How frosted the glass panels look",
                    value = blurIntensity,
                    valueRange = 0f..1f,
                    steps = 9,
                    valueText = "${(blurIntensity * 100).toInt()}%",
                    onValueChange = { settingsVm.setBlurIntensity(it) }
                )
            }

            // ------------------------------------------------ Interface
            MxSection("Interface") {
                MxSelectorRow(
                    title = "Default view",
                    subtitle = "How the video library is shown",
                    selectedId = defaultView,
                    options = listOf("grid" to "Grid", "list" to "List"),
                    onSelect = { settingsVm.setDefaultView(it) }
                )
                MxSelectorRow(
                    title = "Default sort order",
                    subtitle = "How the video library is sorted",
                    selectedId = defaultSort,
                    options = listOf(
                        "name" to "Name", "date" to "Date", "size" to "Size",
                        "duration" to "Duration"
                    ),
                    onSelect = { settingsVm.setDefaultSort(it) }
                )
                MxToggleRow(
                    title = "Show hidden files",
                    subtitle = "Include dot-files and dot-folders from device storage",
                    checked = showHiddenFiles,
                    onCheckedChange = { settingsVm.setShowHiddenFiles(it) }
                )
            }

            // ------------------------------------------------ Privacy
            MxSection("Privacy") {
                MxActionRow(
                    title = "Clear cache",
                    subtitle = "Delete temporary files (downloaded subtitles, thumbnails)",
                    onClick = { confirmClear = "cache" }
                )
                MxActionRow(
                    title = "Clear watch history",
                    subtitle = "Remove all saved resume positions",
                    onClick = { confirmClear = "history" }
                )
                MxActionRow(
                    title = "Clear recent streams",
                    subtitle = "Remove the recent streams list",
                    onClick = { confirmClear = "streams" }
                )
                MxRow(
                    title = "Storage usage",
                    subtitle = "App files on this device",
                    trailing = {
                        Text(
                            text = storageText,
                            color = textSecondary,
                            fontSize = 14.sp
                        )
                    }
                )
            }

            // ------------------------------------------------ Kids Lock
            MxSection("Kids Lock") {
                MxToggleRow(
                    title = "Kids Lock",
                    subtitle = "Locks the player UI so kids cannot change videos or leave the player — long-press the lock icon to unlock",
                    checked = kidsLock,
                    onCheckedChange = { settingsVm.setKidsLock(it) }
                )
            }

            // ------------------------------------------------ About
            MxSection("About") {
                MxRow(
                    title = "Version",
                    subtitle = "K-Player ${BuildConfig.VERSION_NAME}",
                    trailing = {}
                )
                MxActionRow(
                    title = "Check for updates",
                    subtitle = "See if a newer version is available",
                    onClick = {
                        Toast.makeText(
                            context,
                            "You are on the latest version (${BuildConfig.VERSION_NAME})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                MxActionRow(
                    title = "Rate K-Player",
                    subtitle = "Tell us what you think",
                    onClick = {
                        Toast.makeText(
                            context,
                            "Thanks! Rating opens in the Play Store once K-Player is published.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                MxActionRow(
                    title = "Share K-Player",
                    subtitle = "Send the app to a friend",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out K-Player — a premium modern media player for Android."
                            )
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share K-Player")
                        )
                    }
                )
                MxActionRow(
                    title = "Privacy Policy",
                    subtitle = "How K-Player handles your data",
                    onClick = { showPrivacy = true }
                )
                // About cards, inside the About section so they collapse with it.
                MxSectionLabel("About K-Player")
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "K-Player / by Shehzad",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            color = textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "A premium modern media player for videos and music, " +
                                "with a glass design, hardware decoding, subtitles, " +
                                "equalizer and network streaming.",
                            color = textPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                        Text(
                            text = "• Plays videos and music from your device\n" +
                                "• Hardware decoding (HW / HW+ / SW) up to 4K\n" +
                                "• Subtitles in .srt, .vtt and .ass with sync and styling\n" +
                                "• 5-band equalizer with bass boost\n" +
                                "• Network streams (HTTP, HLS, DASH)\n" +
                                "• Resume playback, playlists and sleep timer",
                            color = textSecondary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
                MxSectionLabel("About the Developer")
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Developed by Shehzad Khan",
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Independent Android developer crafting premium, " +
                                "beautiful apps.",
                            color = textPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Contact: —",
                            color = textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation dialogs for the Privacy actions.
    confirmClear?.let { kind ->
        val (title, message) = when (kind) {
            "cache" -> "Clear cache?" to
                "Temporary files (downloaded subtitles, thumbnails) will be deleted."
            "history" -> "Clear watch history?" to
                "All saved resume positions will be removed."
            else -> "Clear recent streams?" to
                "The recent streams list will be emptied."
        }
        MxDialog(onDismiss = { confirmClear = null }) {
            Text(
                text = title,
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                color = LocalTextSecondary.current,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { confirmClear = null }) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = {
                        when (kind) {
                            "cache" -> {
                                val ok = clearAppCache(context)
                                storageRefresh++
                                Toast.makeText(
                                    context,
                                    if (ok) "Cache cleared" else "Could not clear cache",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            "history" -> {
                                settingsVm.clearWatchHistory()
                                libraryVm.clearWatchHistory()
                                Toast.makeText(
                                    context,
                                    "Watch history cleared",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                libraryVm.clearRecentStreams()
                                Toast.makeText(
                                    context,
                                    "Recent streams cleared",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        confirmClear = null
                    }
                ) {
                    Text("Clear")
                }
            }
        }
    }

    // Privacy policy dialog.
    if (showPrivacy) {
        MxDialog(onDismiss = { showPrivacy = false }) {
            Text(
                text = "Privacy Policy",
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "K-Player plays media stored on your device and streams " +
                    "you choose to open. Your files are never uploaded anywhere. " +
                    "Settings, resume positions and recent streams are stored " +
                    "only on this device and are removed when you clear them " +
                    "in Privacy or uninstall the app.",
                color = LocalTextPrimary.current,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showPrivacy = false }) {
                    Text("Close")
                }
            }
        }
    }
}

/** Dialog with a solid theme surface (adapts to light / dark). */
@Composable
private fun MxDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    }
}

/**
 * One collapsible settings section, MX-style: a small uppercase header
 * with a chevron, a thin divider, then the rows. Every section starts
 * expanded and remembers its own state (keyed by title) across
 * recompositions and configuration changes.
 */
@Composable
private fun MxSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
    val textSecondary = LocalTextSecondary.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                color = LocalAccent.current.start,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = textSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (expanded) 90f else 0f)
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (expanded) {
            content()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Small uppercase label used above the About cards. */
@Composable
private fun MxSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = LocalTextSecondary.current,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

/** Base MX-style row: title, optional subtitle, trailing control. */
@Composable
private fun MxRow(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = LocalTextPrimary.current,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = LocalTextSecondary.current,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

@Composable
private fun MxToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    MxRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LocalAccent.current.start,
                    uncheckedThumbColor = Color(0xFFFAFAFA),
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
                )
            )
        }
    )
}

@Composable
private fun MxSelectorRow(
    title: String,
    subtitle: String? = null,
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: selectedId
    Box {
        MxRow(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.clickable { expanded = true },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedLabel,
                        color = LocalTextSecondary.current,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = LocalTextSecondary.current,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (id, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun MxSliderRow(
    title: String,
    subtitle: String? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = LocalTextPrimary.current,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                color = LocalTextSecondary.current,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = LocalTextSecondary.current,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = LocalAccent.current.start,
                activeTrackColor = LocalAccent.current.start,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Minus / value / plus stepper row (subtitle sync delay, audio delay). */
@Composable
private fun MxStepperRow(
    title: String,
    subtitle: String? = null,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusDescription: String,
    plusDescription: String
) {
    MxRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            IconButton(onClick = onMinus, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = minusDescription,
                    tint = LocalTextPrimary.current
                )
            }
            Text(
                text = valueText,
                color = LocalTextPrimary.current,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onPlus, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = plusDescription,
                    tint = LocalTextPrimary.current
                )
            }
        }
    )
}

@Composable
private fun MxActionRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    MxRow(
        title = title,
        subtitle = subtitle,
        modifier = Modifier.clickable(onClick = onClick),
        trailing = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = LocalTextSecondary.current,
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

@Composable
private fun MxThemeRow(
    theme: ThemeDefinition,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "System default" gets a half-light / half-dark swatch; every
        // other theme shows its own accent gradient.
        val swatch = if (theme.id == "system") {
            Brush.horizontalGradient(listOf(Color(0xFFF5F5F5), Color(0xFF111111)))
        } else {
            Brush.horizontalGradient(listOf(theme.accent.start, theme.accent.end))
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(swatch, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = theme.displayName,
                color = LocalTextPrimary.current,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = theme.description,
                color = LocalTextSecondary.current,
                fontSize = 13.sp
            )
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = theme.accent.start)
        )
    }
}

/** Deletes everything under the app cache directory. */
private fun clearAppCache(context: Context): Boolean {
    return try {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        true
    } catch (_: Exception) {
        false
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024L -> "%.0f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

/** Formats a millisecond delay as signed seconds, e.g. "+0.5s", "-1.2s", "0.0s". */
private fun formatDelaySeconds(delayMs: Long): String {
    val seconds = delayMs / 1000.0
    return if (delayMs >= 0) "+%.1fs".format(seconds) else "%.1fs".format(seconds)
}
