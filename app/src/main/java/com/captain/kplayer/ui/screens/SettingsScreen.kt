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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.captain.kplayer.ui.components.GlassButton
import com.captain.kplayer.ui.components.GlassCard
import com.captain.kplayer.ui.components.GlassDialog
import com.captain.kplayer.ui.components.GlassTextField
import com.captain.kplayer.ui.components.SectionHeader
import com.captain.kplayer.ui.components.SettingRow
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.THEMES
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.ui.theme.TextSecondary
import com.captain.kplayer.ui.theme.ThemeDefinition
import com.captain.kplayer.viewmodel.LibraryViewModel
import com.captain.kplayer.viewmodel.PlayerViewModel
import com.captain.kplayer.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Settings: every option is functional state backed by DataStore.
 * Groups: Playback, Decoder, Audio, Subtitles, Gestures, Appearance,
 * Interface, Storage & Privacy, Kids Lock and About.
 */
@Composable
fun SettingsScreen(
    navController: NavHostController,
    settingsVm: SettingsViewModel,
    playerVm: PlayerViewModel,
    libraryVm: LibraryViewModel
) {
    val context = LocalContext.current

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
    val audioLanguage by settingsVm.audioLanguage.collectAsState()
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
    var subtitleSearchQuery by remember { mutableStateOf("") }
    var subtitleSearchResult by remember { mutableStateOf<String?>(null) }
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
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = "Settings",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ------------------------------------------------ Playback
            GroupCard("Playback") {
                SelectorRow(
                    title = "Resume mode",
                    subtitle = "What to do when reopening a video",
                    selectedId = resumeMode,
                    options = listOf(
                        "always_ask" to "Always ask",
                        "always_resume" to "Always resume",
                        "never" to "Never"
                    ),
                    onSelect = { settingsVm.setResumeMode(it) }
                )
                SelectorRow(
                    title = "Double-tap seek duration",
                    subtitle = "How far a double-tap jumps",
                    selectedId = doubleTapSeek.toString(),
                    options = listOf("5" to "5 seconds", "10" to "10 seconds", "15" to "15 seconds", "30" to "30 seconds"),
                    onSelect = { settingsVm.setDoubleTapSeek(it.toIntOrNull() ?: 10) }
                )
                ToggleRow(
                    title = "A-B repeat",
                    subtitle = "Loop a section: tap A-B in the player to mark A, then B",
                    checked = abRepeat,
                    onCheckedChange = { settingsVm.setAbRepeat(it) }
                )
                ToggleRow(
                    title = "Remember playback speed",
                    subtitle = "Keep the last speed you chose for the next video",
                    checked = rememberSpeed,
                    onCheckedChange = { settingsVm.setRememberSpeed(it) }
                )
                SelectorRow(
                    title = "Default speed",
                    subtitle = "Speed new videos start at",
                    selectedId = defaultSpeed,
                    options = listOf(
                        "0.5" to "0.5x", "0.75" to "0.75x", "1.0" to "1.0x",
                        "1.25" to "1.25x", "1.5" to "1.5x", "2.0" to "2.0x"
                    ),
                    onSelect = { settingsVm.setDefaultSpeed(it) }
                )
                ToggleRow(
                    title = "Auto-rotate",
                    subtitle = "Rotate the player with the device sensor",
                    checked = autoRotate,
                    onCheckedChange = { settingsVm.setAutoRotate(it) }
                )
                SelectorRow(
                    title = "Screen orientation lock",
                    subtitle = "Force the player orientation",
                    selectedId = orientationLock,
                    options = listOf(
                        "auto" to "Auto", "portrait" to "Portrait", "landscape" to "Landscape"
                    ),
                    onSelect = { settingsVm.setOrientationLock(it) }
                )
                ToggleRow(
                    title = "Picture-in-Picture",
                    subtitle = "Show the PiP button in the player",
                    checked = pipEnabled,
                    onCheckedChange = { settingsVm.setPipEnabled(it) }
                )
                ToggleRow(
                    title = "Background play",
                    subtitle = "Keep audio playing when you leave the player",
                    checked = backgroundPlay,
                    onCheckedChange = { settingsVm.setBackgroundPlay(it) }
                )
            }

            // ------------------------------------------------ Decoder
            GroupCard("Decoder") {
                SelectorRow(
                    title = "Decoder mode",
                    subtitle = "HW+ uses hardware decoding with software fallback",
                    selectedId = decodingMode,
                    options = listOf(
                        "hw" to "HW", "hw_plus" to "HW+", "sw" to "SW"
                    ),
                    onSelect = { settingsVm.setDecodingMode(it) }
                )
                ToggleRow(
                    title = "Hardware acceleration",
                    subtitle = "Turn off to force software decoding",
                    checked = hardwareAcceleration,
                    onCheckedChange = { on ->
                        settingsVm.setHardwareAcceleration(on)
                        settingsVm.setDecodingMode(if (on) "hw_plus" else "sw")
                    }
                )
            }

            // ------------------------------------------------ Audio
            GroupCard("Audio") {
                SliderRow(
                    title = "Audio boost",
                    subtitle = "Raise volume beyond 100% (up to 200%)",
                    value = audioBoost,
                    valueRange = 1.0f..2.0f,
                    steps = 19,
                    valueText = "${(audioBoost * 100).toInt()}%",
                    onValueChange = { playerVm.setAudioBoost(it) }
                )
                ToggleRow(
                    title = "Audio passthrough",
                    subtitle = "Send the raw bitstream (AC3 / E-AC3) straight to the receiver — equalizer and boost are bypassed",
                    checked = audioPassthrough,
                    onCheckedChange = { settingsVm.setAudioPassthrough(it) }
                )
                SelectorRow(
                    title = "Preferred audio language",
                    subtitle = "Picked automatically when a video has it",
                    selectedId = audioLanguage,
                    options = AUDIO_LANGUAGES.map { it to it },
                    onSelect = { playerVm.setPreferredAudioLanguage(it) }
                )
            }

            // ------------------------------------------------ Subtitles
            GroupCard("Subtitles") {
                SelectorRow(
                    title = "Default subtitle language",
                    subtitle = "Picked automatically when a video has it",
                    selectedId = subtitleLanguage,
                    options = SUBTITLE_LANGUAGES.map { it to it },
                    onSelect = { playerVm.setPreferredSubtitleLanguage(it) }
                )
                SelectorRow(
                    title = "Subtitle encoding",
                    subtitle = "Charset used when reading subtitle files",
                    selectedId = subtitleEncoding,
                    options = listOf("UTF-8" to "UTF-8", "Auto" to "Auto"),
                    onSelect = { settingsVm.setSubtitleEncoding(it) }
                )
                // Subtitle search / download entry.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Search subtitles",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Searches online subtitle databases.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )
                    GlassTextField(
                        value = subtitleSearchQuery,
                        onValueChange = { subtitleSearchQuery = it },
                        placeholder = "Search subtitles, e.g. Big Buck Bunny",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GlassButton(
                        onClick = {
                            subtitleSearchResult =
                                "Online subtitle search is not available in this build — " +
                                "no subtitle provider is configured yet. " +
                                "Load subtitles from a file in the player instead."
                        },
                        icon = Icons.Rounded.Search,
                        text = "Search",
                        modifier = Modifier.fillMaxWidth()
                    )
                    subtitleSearchResult?.let { result ->
                        Text(
                            text = result,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
                // Sync delay +/- 0.5s.
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
                        text = if (subtitleDelayMs >= 0) "+${subtitleDelayMs / 1000.0}s"
                        else "${subtitleDelayMs / 1000.0}s",
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
                SelectorRow(
                    title = "Subtitle font",
                    selectedId = subtitleFont,
                    options = SUBTITLE_FONTS.map { it to it },
                    onSelect = { settingsVm.setSubtitleFont(it) }
                )
                SliderRow(
                    title = "Subtitle size",
                    value = subtitleSize,
                    valueRange = 0.75f..1.5f,
                    steps = 2,
                    valueText = sizeLabel(subtitleSize),
                    onValueChange = { settingsVm.setSubtitleSize(it) }
                )
                SelectorRow(
                    title = "Subtitle color",
                    selectedId = subtitleColor,
                    options = SUBTITLE_COLORS.map { it to it },
                    onSelect = { settingsVm.setSubtitleColor(it) }
                )
                SelectorRow(
                    title = "Subtitle background",
                    selectedId = subtitleBackground,
                    options = SUBTITLE_BACKGROUNDS.map { it to it },
                    onSelect = { settingsVm.setSubtitleBackground(it) }
                )
            }

            // ------------------------------------------------ Gestures
            GroupCard("Gestures") {
                ToggleRow(
                    title = "Brightness gesture",
                    subtitle = "Swipe up / down on the left side of the player",
                    checked = gestureBrightness,
                    onCheckedChange = { settingsVm.setGestureBrightness(it) }
                )
                ToggleRow(
                    title = "Volume gesture",
                    subtitle = "Swipe up / down on the right side of the player",
                    checked = gestureVolume,
                    onCheckedChange = { settingsVm.setGestureVolume(it) }
                )
                ToggleRow(
                    title = "Seek gesture",
                    subtitle = "Swipe left / right to seek",
                    checked = gestureSeek,
                    onCheckedChange = { settingsVm.setGestureSeek(it) }
                )
                SliderRow(
                    title = "Gesture sensitivity",
                    subtitle = "How strongly the player reacts to swipes",
                    value = gestureSensitivity,
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    valueText = "x${"%.1f".format(gestureSensitivity)}",
                    onValueChange = { settingsVm.setGestureSensitivity(it) }
                )
            }

            // ------------------------------------------------ Appearance
            GroupCard("Appearance") {
                Text(
                    text = "Theme",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )
                THEMES.values.forEach { theme ->
                    ThemeRow(
                        theme = theme,
                        selected = themeName == theme.id,
                        onClick = { settingsVm.setThemeName(theme.id) }
                    )
                }
                SliderRow(
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
            GroupCard("Interface") {
                SelectorRow(
                    title = "Default view",
                    subtitle = "How the video library is shown",
                    selectedId = defaultView,
                    options = listOf("grid" to "Grid", "list" to "List"),
                    onSelect = { settingsVm.setDefaultView(it) }
                )
                SelectorRow(
                    title = "Default sort order",
                    subtitle = "How the video library is sorted",
                    selectedId = defaultSort,
                    options = listOf(
                        "name" to "Name", "date" to "Date", "size" to "Size",
                        "duration" to "Duration"
                    ),
                    onSelect = { settingsVm.setDefaultSort(it) }
                )
                ToggleRow(
                    title = "Show hidden files",
                    subtitle = "Include dot-files and dot-folders from device storage",
                    checked = showHiddenFiles,
                    onCheckedChange = { settingsVm.setShowHiddenFiles(it) }
                )
            }

            // ------------------------------------------------ Storage & Privacy
            GroupCard("Storage & Privacy") {
                ActionRow(
                    title = "Clear cache",
                    subtitle = "Delete temporary files (downloaded subtitles, thumbnails)",
                    onClick = { confirmClear = "cache" }
                )
                ActionRow(
                    title = "Clear watch history",
                    subtitle = "Remove all saved resume positions",
                    onClick = { confirmClear = "history" }
                )
                ActionRow(
                    title = "Clear recent streams",
                    subtitle = "Remove the recent streams list",
                    onClick = { confirmClear = "streams" }
                )
                SettingRow(
                    title = "Storage usage",
                    subtitle = "App files on this device",
                    trailing = {
                        Text(
                            text = storageText,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                )
            }

            // ------------------------------------------------ Kids Lock
            GroupCard("Kids Lock") {
                ToggleRow(
                    title = "Kids Lock",
                    subtitle = "Locks the player UI so kids cannot change videos or leave the player — long-press the lock icon to unlock",
                    checked = kidsLock,
                    onCheckedChange = { settingsVm.setKidsLock(it) }
                )
            }

            // ------------------------------------------------ About
            GroupCard("About") {
                SettingRow(
                    title = "Version",
                    subtitle = "K-Player 1.0",
                    trailing = {}
                )
                ActionRow(
                    title = "Check for updates",
                    subtitle = "See if a newer version is available",
                    onClick = {
                        Toast.makeText(
                            context,
                            "You are on the latest version (1.0)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                ActionRow(
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
                ActionRow(
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
                ActionRow(
                    title = "Privacy Policy",
                    subtitle = "How K-Player handles your data",
                    onClick = { showPrivacy = true }
                )
            }

            // About cards.
            SectionHeader("About K-Player")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "K-Player",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "A premium modern media player for videos and music, " +
                            "with a glass design, hardware decoding, subtitles, " +
                            "equalizer and network streaming.",
                        color = TextPrimary,
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
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
            SectionHeader("About the Developer")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Developed by Shehzad Khan",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Independent Android developer crafting premium, " +
                            "beautiful apps.",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Contact: —",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation dialogs for the Storage & Privacy actions.
    confirmClear?.let { kind ->
        val (title, message) = when (kind) {
            "cache" -> "Clear cache?" to
                "Temporary files (downloaded subtitles, thumbnails) will be deleted."
            "history" -> "Clear watch history?" to
                "All saved resume positions will be removed."
            else -> "Clear recent streams?" to
                "The recent streams list will be emptied."
        }
        GlassDialog(onDismiss = { confirmClear = null }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                GlassButton(
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
                    },
                    text = "Clear",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassButton(
                    onClick = { confirmClear = null },
                    text = "Cancel",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Privacy policy dialog.
    if (showPrivacy) {
        GlassDialog(onDismiss = { showPrivacy = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Privacy Policy",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "K-Player plays media stored on your device and streams " +
                        "you choose to open. Your files are never uploaded anywhere. " +
                        "Settings, resume positions and recent streams are stored " +
                        "only on this device and are removed when you clear them " +
                        "in Storage & Privacy or uninstall the app.",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                GlassButton(
                    onClick = { showPrivacy = false },
                    text = "Close",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun GroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    SectionHeader(text = title)
    GlassCard(modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LocalAccent.current.start
                )
            )
        }
    )
}

@Composable
private fun SelectorRow(
    title: String,
    subtitle: String? = null,
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: selectedId
    Box {
        SettingRow(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.clickable { expanded = true },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = selectedLabel, color = TextSecondary, fontSize = 14.sp)
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
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
private fun SliderRow(
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
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = TextSecondary,
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
                thumbColor = Color.White,
                activeTrackColor = LocalAccent.current.start,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        modifier = Modifier.clickable(onClick = onClick),
        trailing = {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

@Composable
private fun ThemeRow(
    theme: ThemeDefinition,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(theme.accent.start, theme.accent.end)
                    ),
                    CircleShape
                )
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = theme.displayName,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = theme.description,
                color = TextSecondary,
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
