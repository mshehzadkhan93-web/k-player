package com.captain.kplayer.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("kplayer_settings")

/**
 * All persistent user settings, backed by DataStore Preferences.
 * Resume positions live in a second DataStore so media ids can be used as
 * dynamic keys without polluting the settings file.
 */
class SettingsStore(private val context: Context) {

    private val Context.resumeDataStore: DataStore<Preferences> by preferencesDataStore("kplayer_resume")

    private object Keys {
        val DECODING_MODE = stringPreferencesKey("decoding_mode")
        val HARDWARE_ACCELERATION = booleanPreferencesKey("hardware_acceleration")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val RESUME_MODE = stringPreferencesKey("resume_mode")
        val DEFAULT_SPEED = stringPreferencesKey("default_speed")
        val REMEMBER_SPEED = booleanPreferencesKey("remember_speed")
        val BACKGROUND_PLAY = booleanPreferencesKey("background_play")
        val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
        val DOUBLE_TAP_SEEK = intPreferencesKey("double_tap_seek")
        val AB_REPEAT = booleanPreferencesKey("ab_repeat")
        val AUTO_ROTATE = booleanPreferencesKey("auto_rotate")
        val ORIENTATION_LOCK = stringPreferencesKey("orientation_lock")
        val KIDS_LOCK = booleanPreferencesKey("kids_lock")
        val GESTURE_BRIGHTNESS = booleanPreferencesKey("gesture_brightness")
        val GESTURE_VOLUME = booleanPreferencesKey("gesture_volume")
        val GESTURE_SEEK = booleanPreferencesKey("gesture_seek")
        val GESTURE_SENSITIVITY = floatPreferencesKey("gesture_sensitivity")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val SUBTITLE_SIZE = floatPreferencesKey("subtitle_size")
        val SUBTITLE_COLOR = stringPreferencesKey("subtitle_color")
        val SUBTITLE_BACKGROUND = stringPreferencesKey("subtitle_background")
        val SUBTITLE_ENCODING = stringPreferencesKey("subtitle_encoding")
        val SUBTITLE_FONT = stringPreferencesKey("subtitle_font")
        val AUDIO_LANGUAGE = stringPreferencesKey("audio_language")
        val AUDIO_BOOST = floatPreferencesKey("audio_boost")
        val AUDIO_PASSTHROUGH = booleanPreferencesKey("audio_passthrough")
        val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val THEME_NAME = stringPreferencesKey("theme_name")
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val DEFAULT_VIEW = stringPreferencesKey("default_view")
        val DEFAULT_SORT = stringPreferencesKey("default_sort")
        val SHOW_HIDDEN_FILES = booleanPreferencesKey("show_hidden_files")
    }

    /** Decoding mode: "hw" (hardware only), "hw_plus" (hardware with software fallback), "sw" (software only). */
    val decodingMode: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.DECODING_MODE] ?: "hw_plus" }
    val resumePlayback: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.RESUME_PLAYBACK] ?: true }
    val defaultSpeed: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.DEFAULT_SPEED] ?: "1.0" }
    val backgroundPlay: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.BACKGROUND_PLAY] ?: true }
    val pipEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.PIP_ENABLED] ?: true }
    val gestureBrightness: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.GESTURE_BRIGHTNESS] ?: true }
    val gestureVolume: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.GESTURE_VOLUME] ?: true }
    val gestureSeek: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.GESTURE_SEEK] ?: true }
    val gestureSensitivity: Flow<Float> =
        context.settingsDataStore.data.map { it[Keys.GESTURE_SENSITIVITY] ?: 1.0f }
    val subtitleLanguage: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.SUBTITLE_LANGUAGE] ?: "English" }
    val subtitleSize: Flow<Float> =
        context.settingsDataStore.data.map { it[Keys.SUBTITLE_SIZE] ?: 1.0f }
    val subtitleColor: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.SUBTITLE_COLOR] ?: "White" }
    val subtitleBackground: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.SUBTITLE_BACKGROUND] ?: "Semi-transparent" }
    val audioLanguage: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.AUDIO_LANGUAGE] ?: "English" }
    val equalizerEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.EQUALIZER_ENABLED] ?: false }
    /** Selected theme id: nebula, emerald, sunset, crimson or ocean. Default: nebula. */
    val themeName: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.THEME_NAME] ?: "nebula" }
    val blurIntensity: Flow<Float> =
        context.settingsDataStore.data.map { it[Keys.BLUR_INTENSITY] ?: 0.6f }
    val hardwareAcceleration: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.HARDWARE_ACCELERATION] ?: true }
    /** "always_ask" | "always_resume" | "never". Always resume is the default. */
    val resumeMode: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.RESUME_MODE] ?: "always_resume" }
    val rememberSpeed: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.REMEMBER_SPEED] ?: true }
    /** Double-tap seek step in seconds (5 / 10 / 15 / 30). */
    val doubleTapSeek: Flow<Int> =
        context.settingsDataStore.data.map { it[Keys.DOUBLE_TAP_SEEK] ?: 10 }
    val abRepeat: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.AB_REPEAT] ?: false }
    val autoRotate: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.AUTO_ROTATE] ?: true }
    /** "auto" | "portrait" | "landscape". */
    val orientationLock: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.ORIENTATION_LOCK] ?: "auto" }
    /** Kids Lock: when on, the player UI starts locked (long-press to unlock). */
    val kidsLock: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.KIDS_LOCK] ?: false }
    /** "UTF-8" | "Auto" — charset used when reading external subtitle files. */
    val subtitleEncoding: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.SUBTITLE_ENCODING] ?: "UTF-8" }
    /** "Sans Serif" | "Serif" | "Monospace". */
    val subtitleFont: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.SUBTITLE_FONT] ?: "Sans Serif" }
    /** Audio boost multiplier, 1.0 (100%) to 2.0 (200%), via LoudnessEnhancer. */
    val audioBoost: Flow<Float> =
        context.settingsDataStore.data.map { it[Keys.AUDIO_BOOST] ?: 1.0f }
    /**
     * Audio passthrough: when on, the raw bitstream (AC3/E-AC3) is sent straight
     * to the receiver with no DSP processing — equalizer and audio boost are
     * bypassed while it is enabled.
     */
    val audioPassthrough: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.AUDIO_PASSTHROUGH] ?: false }
    /** Library default view: "grid" | "list". */
    val defaultView: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.DEFAULT_VIEW] ?: "grid" }
    /** Library default sort: "name" | "date" | "size" | "duration". */
    val defaultSort: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.DEFAULT_SORT] ?: "name" }
    val showHiddenFiles: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.SHOW_HIDDEN_FILES] ?: false }

    suspend fun setDecodingMode(v: String) = edit { it[Keys.DECODING_MODE] = v }
    suspend fun setResumePlayback(v: Boolean) = edit { it[Keys.RESUME_PLAYBACK] = v }
    suspend fun setDefaultSpeed(v: String) = edit { it[Keys.DEFAULT_SPEED] = v }
    suspend fun setBackgroundPlay(v: Boolean) = edit { it[Keys.BACKGROUND_PLAY] = v }
    suspend fun setPipEnabled(v: Boolean) = edit { it[Keys.PIP_ENABLED] = v }
    suspend fun setGestureBrightness(v: Boolean) = edit { it[Keys.GESTURE_BRIGHTNESS] = v }
    suspend fun setGestureVolume(v: Boolean) = edit { it[Keys.GESTURE_VOLUME] = v }
    suspend fun setGestureSeek(v: Boolean) = edit { it[Keys.GESTURE_SEEK] = v }
    suspend fun setGestureSensitivity(v: Float) = edit { it[Keys.GESTURE_SENSITIVITY] = v }
    suspend fun setSubtitleLanguage(v: String) = edit { it[Keys.SUBTITLE_LANGUAGE] = v }
    suspend fun setSubtitleSize(v: Float) = edit { it[Keys.SUBTITLE_SIZE] = v }
    suspend fun setSubtitleColor(v: String) = edit { it[Keys.SUBTITLE_COLOR] = v }
    suspend fun setSubtitleBackground(v: String) = edit { it[Keys.SUBTITLE_BACKGROUND] = v }
    suspend fun setAudioLanguage(v: String) = edit { it[Keys.AUDIO_LANGUAGE] = v }
    suspend fun setEqualizerEnabled(v: Boolean) = edit { it[Keys.EQUALIZER_ENABLED] = v }
    suspend fun setThemeName(v: String) = edit { it[Keys.THEME_NAME] = v }
    suspend fun setBlurIntensity(v: Float) = edit { it[Keys.BLUR_INTENSITY] = v }
    suspend fun setHardwareAcceleration(v: Boolean) = edit { it[Keys.HARDWARE_ACCELERATION] = v }
    suspend fun setResumeMode(v: String) = edit { it[Keys.RESUME_MODE] = v }
    suspend fun setRememberSpeed(v: Boolean) = edit { it[Keys.REMEMBER_SPEED] = v }
    suspend fun setDoubleTapSeek(v: Int) = edit { it[Keys.DOUBLE_TAP_SEEK] = v }
    suspend fun setAbRepeat(v: Boolean) = edit { it[Keys.AB_REPEAT] = v }
    suspend fun setAutoRotate(v: Boolean) = edit { it[Keys.AUTO_ROTATE] = v }
    suspend fun setOrientationLock(v: String) = edit { it[Keys.ORIENTATION_LOCK] = v }
    suspend fun setKidsLock(v: Boolean) = edit { it[Keys.KIDS_LOCK] = v }
    suspend fun setSubtitleEncoding(v: String) = edit { it[Keys.SUBTITLE_ENCODING] = v }
    suspend fun setSubtitleFont(v: String) = edit { it[Keys.SUBTITLE_FONT] = v }
    suspend fun setAudioBoost(v: Float) = edit { it[Keys.AUDIO_BOOST] = v }
    suspend fun setAudioPassthrough(v: Boolean) = edit { it[Keys.AUDIO_PASSTHROUGH] = v }
    suspend fun setDefaultView(v: String) = edit { it[Keys.DEFAULT_VIEW] = v }
    suspend fun setDefaultSort(v: String) = edit { it[Keys.DEFAULT_SORT] = v }
    suspend fun setShowHiddenFiles(v: Boolean) = edit { it[Keys.SHOW_HIDDEN_FILES] = v }

    /** Clears every saved resume position (watch history). */
    suspend fun clearResumePositions() {
        context.resumeDataStore.edit { it.clear() }
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }

    fun resumePositionFlow(mediaId: String): Flow<Long> =
        context.resumeDataStore.data.map { it[longPreferencesKey("pos_$mediaId")] ?: 0L }

    suspend fun saveResumePosition(mediaId: String, positionMs: Long) {
        context.resumeDataStore.edit { it[longPreferencesKey("pos_$mediaId")] = positionMs }
    }

    /**
     * Last-played timestamps, persisted in the SAME kplayer_resume store
     * as the resume positions (keys "played_<mediaId>") so Recently
     * Played survives app restarts. No second store is created.
     */
    val allLastPlayed: Flow<Map<String, Long>> =
        context.resumeDataStore.data.map { prefs ->
            prefs.asMap().entries.mapNotNull { (key, value) ->
                val name = key.name
                if (name.startsWith("played_") && value is Long) {
                    name.removePrefix("played_") to value
                } else null
            }.toMap()
        }

    fun lastPlayedFlow(mediaId: String): Flow<Long> =
        context.resumeDataStore.data.map { it[longPreferencesKey("played_$mediaId")] ?: 0L }

    suspend fun saveLastPlayed(mediaId: String, timestampMs: Long) {
        context.resumeDataStore.edit { it[longPreferencesKey("played_$mediaId")] = timestampMs }
    }

    /** Removes only the played_<id> keys (resume positions are untouched). */
    suspend fun clearLastPlayed() {
        context.resumeDataStore.edit { prefs ->
            prefs.asMap().keys
                .filter { it.name.startsWith("played_") }
                .map { longPreferencesKey(it.name) }
                .forEach { prefs.remove(it) }
        }
    }
}

private typealias MutablePreferences = androidx.datastore.preferences.core.MutablePreferences

/** Exposes [SettingsStore] as StateFlows with simple setter functions. */
class SettingsViewModel(
    application: Application,
    val store: SettingsStore
) : AndroidViewModel(application) {

    private fun <T> state(flow: Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    val decodingMode = state(store.decodingMode, "hw_plus")
    val resumePlayback = state(store.resumePlayback, true)
    val defaultSpeed = state(store.defaultSpeed, "1.0")
    val backgroundPlay = state(store.backgroundPlay, true)
    val pipEnabled = state(store.pipEnabled, true)
    val gestureBrightness = state(store.gestureBrightness, true)
    val gestureVolume = state(store.gestureVolume, true)
    val gestureSeek = state(store.gestureSeek, true)
    val gestureSensitivity = state(store.gestureSensitivity, 1.0f)
    val subtitleLanguage = state(store.subtitleLanguage, "English")
    val subtitleSize = state(store.subtitleSize, 1.0f)
    val subtitleColor = state(store.subtitleColor, "White")
    val subtitleBackground = state(store.subtitleBackground, "Semi-transparent")
    val audioLanguage = state(store.audioLanguage, "English")
    val equalizerEnabled = state(store.equalizerEnabled, false)
    val themeName = state(store.themeName, "nebula")
    val blurIntensity = state(store.blurIntensity, 0.6f)
    val hardwareAcceleration = state(store.hardwareAcceleration, true)
    val resumeMode = state(store.resumeMode, "always_resume")
    val rememberSpeed = state(store.rememberSpeed, true)
    val doubleTapSeek = state(store.doubleTapSeek, 10)
    val abRepeat = state(store.abRepeat, false)
    val autoRotate = state(store.autoRotate, true)
    val orientationLock = state(store.orientationLock, "auto")
    val kidsLock = state(store.kidsLock, false)
    val subtitleEncoding = state(store.subtitleEncoding, "UTF-8")
    val subtitleFont = state(store.subtitleFont, "Sans Serif")
    val audioBoost = state(store.audioBoost, 1.0f)
    val audioPassthrough = state(store.audioPassthrough, false)
    val defaultView = state(store.defaultView, "grid")
    val defaultSort = state(store.defaultSort, "name")
    val showHiddenFiles = state(store.showHiddenFiles, false)

    fun setDecodingMode(v: String) = viewModelScope.launch { store.setDecodingMode(v) }
    fun setResumePlayback(v: Boolean) = viewModelScope.launch { store.setResumePlayback(v) }
    fun setDefaultSpeed(v: String) = viewModelScope.launch { store.setDefaultSpeed(v) }
    fun setBackgroundPlay(v: Boolean) = viewModelScope.launch { store.setBackgroundPlay(v) }
    fun setPipEnabled(v: Boolean) = viewModelScope.launch { store.setPipEnabled(v) }
    fun setGestureBrightness(v: Boolean) = viewModelScope.launch { store.setGestureBrightness(v) }
    fun setGestureVolume(v: Boolean) = viewModelScope.launch { store.setGestureVolume(v) }
    fun setGestureSeek(v: Boolean) = viewModelScope.launch { store.setGestureSeek(v) }
    fun setGestureSensitivity(v: Float) = viewModelScope.launch { store.setGestureSensitivity(v) }
    fun setSubtitleLanguage(v: String) = viewModelScope.launch { store.setSubtitleLanguage(v) }
    fun setSubtitleSize(v: Float) = viewModelScope.launch { store.setSubtitleSize(v) }
    fun setSubtitleColor(v: String) = viewModelScope.launch { store.setSubtitleColor(v) }
    fun setSubtitleBackground(v: String) = viewModelScope.launch { store.setSubtitleBackground(v) }
    fun setAudioLanguage(v: String) = viewModelScope.launch { store.setAudioLanguage(v) }
    fun setEqualizerEnabled(v: Boolean) = viewModelScope.launch { store.setEqualizerEnabled(v) }
    fun setThemeName(v: String) = viewModelScope.launch { store.setThemeName(v) }
    fun setBlurIntensity(v: Float) = viewModelScope.launch { store.setBlurIntensity(v) }
    fun setHardwareAcceleration(v: Boolean) = viewModelScope.launch { store.setHardwareAcceleration(v) }
    fun setResumeMode(v: String) = viewModelScope.launch { store.setResumeMode(v) }
    fun setRememberSpeed(v: Boolean) = viewModelScope.launch { store.setRememberSpeed(v) }
    fun setDoubleTapSeek(v: Int) = viewModelScope.launch { store.setDoubleTapSeek(v) }
    fun setAbRepeat(v: Boolean) = viewModelScope.launch { store.setAbRepeat(v) }
    fun setAutoRotate(v: Boolean) = viewModelScope.launch { store.setAutoRotate(v) }
    fun setOrientationLock(v: String) = viewModelScope.launch { store.setOrientationLock(v) }
    fun setKidsLock(v: Boolean) = viewModelScope.launch { store.setKidsLock(v) }
    fun setSubtitleEncoding(v: String) = viewModelScope.launch { store.setSubtitleEncoding(v) }
    fun setSubtitleFont(v: String) = viewModelScope.launch { store.setSubtitleFont(v) }
    fun setAudioBoost(v: Float) = viewModelScope.launch { store.setAudioBoost(v) }
    fun setAudioPassthrough(v: Boolean) = viewModelScope.launch { store.setAudioPassthrough(v) }
    fun setDefaultView(v: String) = viewModelScope.launch { store.setDefaultView(v) }
    fun setDefaultSort(v: String) = viewModelScope.launch { store.setDefaultSort(v) }
    fun setShowHiddenFiles(v: Boolean) = viewModelScope.launch { store.setShowHiddenFiles(v) }

    /** Clears every saved resume position (watch history). */
    fun clearWatchHistory() = viewModelScope.launch { store.clearResumePositions() }
}
