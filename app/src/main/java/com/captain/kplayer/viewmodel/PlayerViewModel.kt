package com.captain.kplayer.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.captain.kplayer.data.QueueEntry
import com.captain.kplayer.data.StreamItem
import com.captain.kplayer.data.TrackItem
import com.captain.kplayer.data.VideoItem
import com.captain.kplayer.data.toQueueEntry
import com.captain.kplayer.player.EqualizerManager
import com.captain.kplayer.player.PlayerFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

enum class VideoAspect { FIT, FILL, ZOOM }

data class UiAudioTrack(
    val label: String,
    val codec: String,
    val channels: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val selected: Boolean
)

data class UiSubtitleTrack(
    val label: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val selected: Boolean,
    /** True when the track came from an externally loaded subtitle file. */
    val isExternal: Boolean = false
)

/**
 * Owns the single ExoPlayer instance and all playback state: video and audio
 * queues, position, speed, aspect ratio, volume, brightness, audio/subtitle
 * tracks, sleep timer, equalizer and resume positions.
 */
class PlayerViewModel(
    application: Application,
    private val store: SettingsStore
) : AndroidViewModel(application) {

    private val appContext: Context get() = getApplication()
    private val audioManager: AudioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    lateinit var player: ExoPlayer
        private set

    /** True once the ExoPlayer instance has been built (async, from settings). */
    val isPlayerReady: Boolean get() = playerBuilt

    private var playerBuilt = false
    private var currentDecodingMode = "hw_plus"
    private var released = false

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onTracksChanged(tracks: Tracks) {
            refreshTrackLists()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (audioSessionId != 0 && _equalizerManager.value == null) {
                val manager = EqualizerManager(audioSessionId)
                manager.enabled = equalizerEnabled.value
                _equalizerManager.value = manager
                applyAudioEffects(audioBoost.value, audioPassthrough.value)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (::player.isInitialized) {
                val index = player.currentMediaItemIndex
                _currentIndex.value = index
                // Bug 1: the top-bar title, duration and queue position all
                // read from this entry, so every transition (next / prev /
                // auto-advance) refreshes the whole player UI.
                _currentEntry.value = _queue.value.getOrNull(index)
                _positionMs.value = 0L
                _durationMs.value = _queue.value.getOrNull(index)?.durationMs ?: 0L
                _resumePrompt.value = null
                softwareFallbackTried = false
                seekWatchdogJob?.cancel()
                bufferingWatchdogJob?.cancel()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            _isBuffering.value = buffering
            // A local file that stays in BUFFERING is almost always
            // decode-bound, not I/O-bound: arm the stall watchdog.
            if (buffering) armBufferingWatchdog() else bufferingWatchdogJob?.cancel()
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlayerError(error)
        }
    }

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _currentEntry = MutableStateFlow<QueueEntry?>(null)
    val currentEntry: StateFlow<QueueEntry?> = _currentEntry

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _bufferedMs = MutableStateFlow(0L)
    val bufferedMs: StateFlow<Long> = _bufferedMs

    /** True while the player is stalled waiting for data (shows a spinner). */
    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering

    /** Last user-facing playback error, if any. Cleared after it is shown. */
    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError

    fun clearPlayerError() {
        _playerError.value = null
    }

    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed

    private val _aspect = MutableStateFlow(VideoAspect.FIT)
    val aspect: StateFlow<VideoAspect> = _aspect

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle

    /** 0 = off, 1 = repeat all, 2 = repeat one. */
    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode

    private val _volumePercent = MutableStateFlow(50)
    val volumePercent: StateFlow<Int> = _volumePercent

    private val _brightnessPercent = MutableStateFlow(0.5f)
    val brightnessPercent: StateFlow<Float> = _brightnessPercent

    private val _audioTracks = MutableStateFlow<List<UiAudioTrack>>(emptyList())
    val audioTracks: StateFlow<List<UiAudioTrack>> = _audioTracks

    private val _subtitleTracks = MutableStateFlow<List<UiSubtitleTrack>>(emptyList())
    val subtitleTracks: StateFlow<List<UiSubtitleTrack>> = _subtitleTracks

    private val _audioDisabled = MutableStateFlow(false)
    val audioDisabled: StateFlow<Boolean> = _audioDisabled

    private val _subtitlesDisabled = MutableStateFlow(false)
    val subtitlesDisabled: StateFlow<Boolean> = _subtitlesDisabled

    /** Subtitle delay in ms, changed in +/- 500 ms steps. Applies to loaded files. */
    private val _subtitleDelayMs = MutableStateFlow(0L)
    val subtitleDelayMs: StateFlow<Long> = _subtitleDelayMs

    private val _sleepRemainingMs = MutableStateFlow<Long?>(null)
    val sleepRemainingMs: StateFlow<Long?> = _sleepRemainingMs

    private val _equalizerManager = MutableStateFlow<EqualizerManager?>(null)
    val equalizerManager: StateFlow<EqualizerManager?> = _equalizerManager

    private val _equalizerPreset = MutableStateFlow("Normal")
    val equalizerPreset: StateFlow<String> = _equalizerPreset

    val backgroundPlay: StateFlow<Boolean> =
        store.backgroundPlay.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val pipEnabled: StateFlow<Boolean> =
        store.pipEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val equalizerEnabled: StateFlow<Boolean> =
        store.equalizerEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val resumePlayback: StateFlow<Boolean> =
        store.resumePlayback.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    /** "always_ask" | "always_resume" | "never". */
    val resumeMode: StateFlow<String> =
        store.resumeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "always_resume")
    val rememberSpeed: StateFlow<Boolean> =
        store.rememberSpeed.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    /** Double-tap seek step in seconds (5 / 10 / 15 / 30). */
    val doubleTapSeekSeconds: StateFlow<Int> =
        store.doubleTapSeek.stateIn(viewModelScope, SharingStarted.Eagerly, 10)
    val abRepeatEnabled: StateFlow<Boolean> =
        store.abRepeat.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val autoRotate: StateFlow<Boolean> =
        store.autoRotate.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    /** "auto" | "portrait" | "landscape". */
    val orientationLock: StateFlow<String> =
        store.orientationLock.stateIn(viewModelScope, SharingStarted.Eagerly, "auto")
    /** Kids Lock: when on, the player UI starts locked (long-press to unlock). */
    val kidsLock: StateFlow<Boolean> =
        store.kidsLock.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    /** Audio boost multiplier 1.0 (100%) to 2.0 (200%). */
    val audioBoost: StateFlow<Float> =
        store.audioBoost.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    /** When on, DSP (equalizer + boost) is bypassed for raw bitstream output. */
    val audioPassthrough: StateFlow<Boolean> =
        store.audioPassthrough.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    /** "Sans Serif" | "Serif" | "Monospace". */
    val subtitleFont: StateFlow<String> =
        store.subtitleFont.stateIn(viewModelScope, SharingStarted.Eagerly, "Sans Serif")
    /** "UTF-8" | "Auto" — charset used when reading external subtitle files. */
    val subtitleEncoding: StateFlow<String> =
        store.subtitleEncoding.stateIn(viewModelScope, SharingStarted.Eagerly, "UTF-8")

    /** Shown when resume mode is "Always ask" and a saved position exists. */
    data class ResumePrompt(val mediaId: String, val positionMs: Long)
    private val _resumePrompt = MutableStateFlow<ResumePrompt?>(null)
    val resumePrompt: StateFlow<ResumePrompt?> = _resumePrompt

    fun confirmResumePrompt(resume: Boolean) {
        val prompt = _resumePrompt.value
        _resumePrompt.value = null
        if (resume && prompt != null && playerBuilt) {
            seekTo(prompt.positionMs)
        }
    }

    enum class ABState { OFF, A_SET, LOOPING }
    private val _abState = MutableStateFlow(ABState.OFF)
    val abState: StateFlow<ABState> = _abState
    private val _aPointMs = MutableStateFlow(0L)
    val aPointMs: StateFlow<Long> = _aPointMs
    private val _bPointMs = MutableStateFlow(0L)
    val bPointMs: StateFlow<Long> = _bPointMs

    /**
     * Cycles A-B repeat: first tap marks point A, second tap marks point B
     * and starts looping between them, third tap clears. Disabled entirely
     * while the A-B repeat setting is off.
     */
    fun cycleABRepeat() {
        if (!abRepeatEnabled.value) return
        when (_abState.value) {
            ABState.OFF -> {
                _aPointMs.value = _positionMs.value
                _abState.value = ABState.A_SET
            }
            ABState.A_SET -> {
                val b = _positionMs.value
                if (b > _aPointMs.value + 1000L) {
                    _bPointMs.value = b
                    _abState.value = ABState.LOOPING
                } else {
                    _aPointMs.value = b
                }
            }
            ABState.LOOPING -> _abState.value = ABState.OFF
        }
    }

    private var sleepJob: Job? = null
    private var tickerJob: Job? = null
    private var seekWatchdogJob: Job? = null
    /** Fires when a local file stays in BUFFERING too long (see below). */
    private var bufferingWatchdogJob: Job? = null
    private var lastSeekRequestMs = 0L
    private var lastSeekTargetMs = -1L
    private var seekRetryCount = 0
    /** True once the current item already fell back to software decoding. */
    private var softwareFallbackTried = false

    init {
        refreshVolume()
        viewModelScope.launch {
            store.decodingMode.collect { mode ->
                if (!playerBuilt) {
                    buildPlayer(mode)
                } else if (mode != currentDecodingMode) {
                    rebuildPlayer(mode)
                }
            }
        }
        viewModelScope.launch {
            combine(store.audioBoost, store.audioPassthrough) { boost, passthrough ->
                boost to passthrough
            }.collect { (boost, passthrough) ->
                applyAudioEffects(boost, passthrough)
            }
        }
        viewModelScope.launch {
            store.abRepeat.collect { enabled ->
                if (!enabled) _abState.value = ABState.OFF
            }
        }
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(400)
                if (playerBuilt && !released) {
                    _positionMs.value = player.currentPosition
                    _durationMs.value = if (player.duration > 0) player.duration else 0L
                    _bufferedMs.value = player.bufferedPosition
                    if (_abState.value == ABState.LOOPING &&
                        _positionMs.value >= _bPointMs.value
                    ) {
                        player.seekTo(_aPointMs.value)
                    }
                }
            }
        }
    }

    /**
     * Applies equalizer + audio boost, or bypasses all DSP when audio
     * passthrough is on (the raw bitstream goes straight to the receiver).
     */
    private fun applyAudioEffects(boost: Float, passthrough: Boolean) {
        val manager = _equalizerManager.value ?: return
        if (passthrough) {
            manager.enabled = false
            manager.setAudioBoost(1f)
        } else {
            manager.enabled = equalizerEnabled.value
            manager.setAudioBoost(boost)
        }
    }

    fun setAudioBoost(boost: Float) {
        viewModelScope.launch { store.setAudioBoost(boost.coerceIn(1f, 2f)) }
    }

    fun setAudioPassthrough(on: Boolean) {
        viewModelScope.launch { store.setAudioPassthrough(on) }
    }

    private fun buildPlayer(decodingMode: String) {
        currentDecodingMode = decodingMode
        player = PlayerFactory.buildPlayer(appContext, decodingMode)
        player.addListener(listener)
        playerBuilt = true
        applyDefaultSpeed()
    }

    private fun rebuildPlayer(decodingMode: String) {
        if (!playerBuilt) return
        val wasPlaying = player.playWhenReady
        val items = queue.value.map { PlayerFactory.mediaItem(it.url, it.title, it.subtitle) }
        val index = _currentIndex.value
        val position = player.currentPosition
        player.removeListener(listener)
        player.release()
        _equalizerManager.value?.release()
        _equalizerManager.value = null
        buildPlayer(decodingMode)
        if (items.isNotEmpty()) {
            player.setMediaItems(items, index, position)
            player.prepare()
            player.playWhenReady = wasPlaying
        }
    }

    private fun applyDefaultSpeed() {
        viewModelScope.launch {
            val speedText = store.defaultSpeed.first()
            val parsed = speedText.toFloatOrNull() ?: 1.0f
            _speed.value = parsed
            if (playerBuilt) player.setPlaybackSpeed(parsed)
        }
    }

    /**
     * Bug 2: decoder failure (e.g. a codec the hardware decoder cannot
     * handle) — rebuild once with software decoding. Any other failure
     * surfaces a user-facing message instead of a stuck frame or a crash.
     */
    private fun handlePlayerError(error: PlaybackException) {
        val isDecoderError = error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
        if (isDecoderError && currentDecodingMode != "sw" && !softwareFallbackTried) {
            softwareFallbackTried = true
            _playerError.value = "Hardware decoder failed — retrying with software decoding…"
            rebuildPlayer("sw")
            return
        }
        if (isDecoderError) {
            // Software decoding was already tried (or is the current
            // mode) and still failed: say so plainly instead of
            // pretending a retry will fix it. A video track the phone's
            // MediaCodec cannot decode (e.g. 4K HEVC Main 10 without
            // hardware support) cannot be fixed by the FFmpeg audio
            // extension, which only decodes audio.
            _playerError.value =
                "This phone can't decode this video's format. " +
                "Try a lower-quality copy."
            return
        }
        _playerError.value = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "Could not load this video. Check the link or file and try again."
            else -> "Playback error: ${error.message ?: "unknown error"}"
        }
    }

    /** Bug 4: a stream URL must be a well-formed http(s) URL with a host. */
    fun isValidStreamUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        return try {
            val uri = Uri.parse(trimmed)
            (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Bug: a heavy local file (e.g. a large MKV) can sit in BUFFERING
     * forever without ever raising an error — a silent stuck spinner.
     * If a LOCAL file (not a stream) is still buffering after 15s, say
     * so plainly instead of spinning forever: retry once with software
     * decoding, and if that was already tried, tell the user the phone
     * cannot decode this file.
     */
    private fun armBufferingWatchdog() {
        bufferingWatchdogJob?.cancel()
        bufferingWatchdogJob = viewModelScope.launch {
            delay(15_000)
            if (!playerBuilt || released) return@launch
            if (!_isBuffering.value) return@launch
            val url = _currentEntry.value?.url.orEmpty()
            if (url.startsWith("http")) return@launch // streams: network, not decode
            if (!softwareFallbackTried && currentDecodingMode != "sw") {
                softwareFallbackTried = true
                _playerError.value = "Still loading — retrying with software decoding…"
                rebuildPlayer("sw")
            } else {
                _playerError.value =
                    "This video is too heavy for this phone to decode. " +
                    "Try a lower-quality copy."
            }
        }
    }

    /**
     * Bug 2: watches a seek — if the position has not settled near the
     * target after 3s, re-seeks once; if it is still stuck, re-prepares
     * the player instead of leaving the video frozen.
     */
    private fun armSeekWatchdog(target: Long) {
        seekWatchdogJob?.cancel()
        seekWatchdogJob = viewModelScope.launch {
            delay(3000)
            if (!playerBuilt || released) return@launch
            if (kotlin.math.abs(player.currentPosition - target) > 500L) {
                if (seekRetryCount < 1) {
                    seekRetryCount++
                    player.seekTo(target)
                    delay(3000)
                    if (!playerBuilt || released) return@launch
                }
                if (kotlin.math.abs(player.currentPosition - target) > 500L) {
                    player.prepare()
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Starting playback
    // ------------------------------------------------------------------

    fun playVideo(video: VideoItem, allVideos: List<VideoItem>) {
        val entries = listOf(video.toQueueEntry()) +
            allVideos.filter { it.id != video.id }.map { it.toQueueEntry() }
        startQueue(entries, 0)
        viewModelScope.launch {
            val saved = store.resumePositionFlow(video.id).first()
            if (saved <= 5_000L || saved >= video.durationMs - 10_000L) return@launch
            when (resumeMode.value) {
                "always_resume" -> player.seekTo(saved)
                "always_ask" -> _resumePrompt.value = ResumePrompt(video.id, saved)
                "never" -> Unit
            }
        }
    }

    fun playTrack(track: TrackItem, allTracks: List<TrackItem>) {
        val entries = allTracks.map { it.toQueueEntry() }
        val index = entries.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        startQueue(entries, index)
    }

    /** Returns false (and surfaces an error) when the URL cannot be played. */
    fun playStream(stream: StreamItem): Boolean {
        if (!isValidStreamUrl(stream.url)) {
            _playerError.value = "That stream URL doesn't look valid. Use a full http(s) link."
            return false
        }
        return try {
            startQueue(listOf(stream.toQueueEntry()), 0)
            playerBuilt
        } catch (e: Exception) {
            _playerError.value = "Couldn't start this stream: ${e.message ?: "unknown error"}"
            false
        }
    }

    fun playQueueEntry(index: Int) {
        if (!playerBuilt || index !in queue.value.indices) return
        _currentIndex.value = index
        _currentEntry.value = queue.value[index]
        player.seekToDefaultPosition(index)
        player.playWhenReady = true
        player.prepare()
    }

    private fun startQueue(entries: List<QueueEntry>, startIndex: Int) {
        if (!playerBuilt) {
            _playerError.value = "Player is still starting — try again in a moment."
            return
        }
        val items = entries.map { PlayerFactory.mediaItem(it.url, it.title, it.subtitle) }
        // Queue state is only published AFTER setMediaItems succeeds: if it
        // throws, the UI must not claim the new queue while ExoPlayer still
        // holds the previous (e.g. device-video) playlist.
        try {
            player.setMediaItems(items, startIndex, 0L)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            _playerError.value = "Couldn't start playback: ${e.message ?: "unknown error"}"
            return
        }
        _queue.value = entries
        _currentIndex.value = startIndex
        _currentEntry.value = entries.getOrNull(startIndex)
    }

    // ------------------------------------------------------------------
    // Transport controls
    // ------------------------------------------------------------------

    fun togglePlay() {
        if (!playerBuilt) return
        if (player.isPlaying) {
            player.pause()
            saveCurrentResumePosition()
        } else {
            player.play()
        }
    }

    fun play() {
        if (playerBuilt) player.play()
    }

    fun pause() {
        if (playerBuilt) {
            player.pause()
            saveCurrentResumePosition()
        }
    }

    /**
     * Bug 2: seeks on the player thread with a guard against repeated seeks
     * to the same position, an optimistic UI update so scrubbing feels
     * instant, and a watchdog that re-seeks once (then re-prepares) if the
     * seek never lands.
     */
    fun seekTo(positionMs: Long) {
        if (!playerBuilt) return
        val target = if (_durationMs.value > 0) {
            positionMs.coerceIn(0L, _durationMs.value)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        val now = System.currentTimeMillis()
        if (target == lastSeekTargetMs && now - lastSeekRequestMs < 150L) return
        lastSeekTargetMs = target
        lastSeekRequestMs = now
        seekRetryCount = 0
        player.seekTo(target)
        _positionMs.value = target
        armSeekWatchdog(target)
    }

    fun seekRelative(deltaMs: Long) {
        if (playerBuilt) seekTo(player.currentPosition + deltaMs)
    }

    fun playNext() {
        if (playerBuilt && player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    fun playPrevious() {
        if (playerBuilt && player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
    }

    fun setSpeed(speed: Float) {
        _speed.value = speed
        if (playerBuilt) player.setPlaybackSpeed(speed)
        // Remember playback speed: persist the chosen speed so the next
        // video starts at it. When the setting is off, the stored default
        // speed is left untouched.
        if (rememberSpeed.value) {
            viewModelScope.launch { store.setDefaultSpeed(speed.toString()) }
        }
    }

    fun setSpeedTemporarily(speed: Float) {
        if (playerBuilt) player.setPlaybackSpeed(speed)
    }

    fun restoreSpeed() {
        if (playerBuilt) player.setPlaybackSpeed(_speed.value)
    }

    fun cycleAspect() {
        _aspect.value = when (_aspect.value) {
            VideoAspect.FIT -> VideoAspect.FILL
            VideoAspect.FILL -> VideoAspect.ZOOM
            VideoAspect.ZOOM -> VideoAspect.FIT
        }
    }

    fun setAspect(aspect: VideoAspect) {
        _aspect.value = aspect
    }

    fun setLocked(locked: Boolean) {
        _locked.value = locked
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
        if (playerBuilt) player.shuffleModeEnabled = _shuffle.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
        if (playerBuilt) {
            player.repeatMode = when (_repeatMode.value) {
                1 -> Player.REPEAT_MODE_ALL
                2 -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val list = _queue.value.toMutableList()
        if (from !in list.indices || to !in list.indices || from == to) return
        val item = list.removeAt(from)
        list.add(to, item)
        _queue.value = list
        if (playerBuilt) player.moveMediaItem(from, to)
        _currentIndex.value = player.currentMediaItemIndex
    }

    fun removeQueueItem(index: Int) {
        if (index !in _queue.value.indices) return
        val list = _queue.value.toMutableList()
        list.removeAt(index)
        _queue.value = list
        if (playerBuilt) player.removeMediaItem(index)
        _currentIndex.value = player.currentMediaItemIndex
        _currentEntry.value = list.getOrNull(_currentIndex.value)
    }

    // ------------------------------------------------------------------
    // Volume and brightness
    // ------------------------------------------------------------------

    fun refreshVolume() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        _volumePercent.value = (current * 100 / max).coerceIn(0, 100)
    }

    fun setVolumePercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        _volumePercent.value = clamped
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (clamped * max / 100f).roundToInt().coerceIn(0, max),
            0
        )
    }

    fun setBrightnessPercent(percent: Float) {
        _brightnessPercent.value = percent.coerceIn(0.05f, 1f)
    }

    // ------------------------------------------------------------------
    // Audio and subtitle tracks
    // ------------------------------------------------------------------

    private fun refreshTrackLists() {
        if (!playerBuilt) return
        val audio = mutableListOf<UiAudioTrack>()
        val subs = mutableListOf<UiSubtitleTrack>()
        player.currentTracks.groups.forEachIndexed { groupIndex, group ->
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        audio.add(
                            UiAudioTrack(
                                label = languageName(format.language),
                                codec = codecName(format.sampleMimeType),
                                channels = channelLabel(format.channelCount),
                                groupIndex = groupIndex,
                                trackIndex = i,
                                selected = group.isTrackSelected(i)
                            )
                        )
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        subs.add(
                            UiSubtitleTrack(
                                label = languageName(format.language),
                                groupIndex = groupIndex,
                                trackIndex = i,
                                selected = group.isTrackSelected(i),
                                // Externally loaded files carry the id
                                // stamped on in PlayerFactory; embedded
                                // tracks have no such id.
                                isExternal = format.id == PlayerFactory.EXTERNAL_SUBTITLE_ID
                            )
                        )
                    }
                }
            }
        }
        _audioTracks.value = audio
        _subtitleTracks.value = subs
        _audioDisabled.value =
            player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO)
        _subtitlesDisabled.value =
            player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
    }

    /**
     * Bug 3: audio-track switching applies via the track selector without
     * blocking the UI. If a track fails, fall back to automatic selection
     * and surface an error instead of freezing on a stuck frame.
     */
    fun selectAudioTrack(track: UiAudioTrack) {
        if (!playerBuilt) return
        try {
            val group = player.currentTracks.groups.getOrNull(track.groupIndex) ?: return
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setOverrideForType(
                    TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
                )
                .build()
            _audioDisabled.value = false
        } catch (e: Exception) {
            try {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .clearOverrides()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .build()
            } catch (_: Exception) {
            }
            _playerError.value = "Couldn't switch to that audio track — kept the current one."
        }
    }

    fun disableAudio() {
        if (!playerBuilt) return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
        _audioDisabled.value = true
    }

    fun selectSubtitleTrack(track: UiSubtitleTrack) {
        if (!playerBuilt) return
        try {
            val group = player.currentTracks.groups.getOrNull(track.groupIndex) ?: return
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(
                    TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
                )
                .build()
            _subtitlesDisabled.value = false
        } catch (_: Exception) {
            _playerError.value = "Couldn't switch to that subtitle track."
        }
    }

    fun disableSubtitles() {
        if (!playerBuilt) return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        _subtitlesDisabled.value = true
    }

    fun setPreferredAudioLanguage(languageName: String) {
        if (!playerBuilt) return
        val code = languageCode(languageName)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setPreferredAudioLanguage(code)
            .build()
        viewModelScope.launch { store.setAudioLanguage(languageName) }
    }

    fun setPreferredSubtitleLanguage(languageName: String) {
        if (!playerBuilt) return
        val code = languageCode(languageName)
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setPreferredTextLanguage(code)
            .build()
        viewModelScope.launch { store.setSubtitleLanguage(languageName) }
    }

    fun adjustSubtitleDelay(deltaMs: Long) {
        _subtitleDelayMs.value = (_subtitleDelayMs.value + deltaMs).coerceIn(-10_000L, 10_000L)
    }

    /**
     * Loads an external subtitle file. The file is copied into the app cache
     * with cue timestamps shifted by the current subtitle delay, then the
     * current item is rebuilt with the subtitle attached and playback
     * resumes from the same position.
     */
    fun addExternalSubtitle(uri: Uri, fileName: String) {
        if (!playerBuilt) return
        val entry = _currentEntry.value ?: return
        val extension = fileName.substringAfterLast(".", "").lowercase()
        val mimeType = when (extension) {
            "srt" -> "application/x-subrip"
            "vtt" -> "text/vtt"
            "ass" -> "text/x-ssa"
            else -> return
        }
        viewModelScope.launch {
            val position = player.currentPosition
            val wasPlaying = player.isPlaying
            val subtitleUri = try {
                val shifted = shiftSubtitleFile(uri, mimeType)
                shifted ?: uri
            } catch (_: Exception) {
                uri
            }
            val item = PlayerFactory.mediaItemWithSubtitles(
                entry.url, entry.title, subtitleUri.toString(), "eng", mimeType
            )
            player.setMediaItem(item, position)
            player.prepare()
            player.playWhenReady = wasPlaying
        }
    }

    /** Reads a subtitle file, shifts every cue timestamp by [delayMs], writes a cache copy. */
    private fun shiftSubtitleFile(uri: Uri, mimeType: String): Uri? {
        val delay = _subtitleDelayMs.value
        val input = appContext.contentResolver.openInputStream(uri) ?: return null
        val bytes = input.use { it.readBytes() }
        val charset = when (subtitleEncoding.value) {
            "Auto" -> detectCharset(bytes)
            else -> Charsets.UTF_8
        }
        val text = String(bytes, charset).removePrefix("\uFEFF")
        val shifted = if (delay == 0L) {
            text
        } else {
            shiftTimestamps(text, delay)
        }
        val file = File(appContext.cacheDir, "subtitle_${System.currentTimeMillis()}.srt")
        file.writeText(shifted)
        return Uri.fromFile(file)
    }

    /** Detects UTF-8 / UTF-16LE / UTF-16BE by BOM, defaulting to UTF-8. */
    private fun detectCharset(bytes: ByteArray): java.nio.charset.Charset {
        return when {
            bytes.size >= 3 && bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> Charsets.UTF_8
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xFE.toByte() -> Charsets.UTF_16LE
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() &&
                bytes[1] == 0xFF.toByte() -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
    }

    private fun shiftTimestamps(text: String, delayMs: Long): String {
        val pattern = Regex("""(\d{2}):(\d{2}):(\d{2})[,.](\d{3})""")
        return pattern.replace(text) { match ->
            val (h, m, s, ms) = match.destructured
            val total = (h.toLong() * 3_600_000L + m.toLong() * 60_000L +
                s.toLong() * 1_000L + ms.toLong() + delayMs).coerceAtLeast(0L)
            val nh = total / 3_600_000L
            val nm = (total % 3_600_000L) / 60_000L
            val ns = (total % 60_000L) / 1_000L
            val nms = total % 1_000L
            "%02d:%02d:%02d,%03d".format(nh, nm, ns, nms)
        }
    }

    private fun languageName(code: String?): String {
        return when (code?.lowercase()) {
            "en", "eng" -> "English"
            "hi", "hin" -> "Hindi"
            "ur", "urd" -> "Urdu"
            "ar", "ara" -> "Arabic"
            "es", "spa" -> "Spanish"
            "fr", "fra" -> "French"
            "de", "deu" -> "German"
            null -> "Unknown"
            else -> code
        }
    }

    private fun languageCode(name: String): String {
        return when (name) {
            "English" -> "en"
            "Hindi" -> "hi"
            "Urdu" -> "ur"
            "Arabic" -> "ar"
            "Spanish" -> "es"
            "French" -> "fr"
            "German" -> "de"
            else -> "en"
        }
    }

    private fun codecName(mimeType: String?): String {
        return when (mimeType) {
            "audio/mp4a-latm" -> "AAC"
            "audio/ac3" -> "AC3"
            "audio/eac3" -> "E-AC3"
            "audio/mpeg" -> "MP3"
            "audio/vorbis" -> "Vorbis"
            "audio/opus" -> "Opus"
            "audio/flac" -> "FLAC"
            else -> mimeType?.substringAfter("/")?.uppercase() ?: "Unknown"
        }
    }

    private fun channelLabel(channelCount: Int): String {
        return when (channelCount) {
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1"
            8 -> "7.1"
            Format.NO_VALUE -> "Unknown"
            else -> "$channelCount ch"
        }
    }

    // ------------------------------------------------------------------
    // Equalizer
    // ------------------------------------------------------------------

    fun setEqualizerEnabled(enabled: Boolean) {
        // Passthrough bypasses all DSP, so the equalizer stays off while it is on.
        if (!audioPassthrough.value) {
            _equalizerManager.value?.enabled = enabled
        }
        viewModelScope.launch { store.setEqualizerEnabled(enabled) }
    }

    fun setEqualizerBand(freqHz: Int, levelDb: Float) {
        _equalizerManager.value?.setBandLevel(freqHz, levelDb)
    }

    fun applyEqualizerPreset(name: String) {
        _equalizerPreset.value = name
        val levels = EqualizerManager.PRESETS[name] ?: return
        _equalizerManager.value?.applyPreset(levels)
    }

    fun setBassBoost(strength: Int) {
        _equalizerManager.value?.setBassBoost(strength)
    }

    // ------------------------------------------------------------------
    // Sleep timer
    // ------------------------------------------------------------------

    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _sleepRemainingMs.value = null
            return
        }
        sleepJob = viewModelScope.launch {
            var remaining = minutes * 60_000L
            _sleepRemainingMs.value = remaining
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1_000
                _sleepRemainingMs.value = remaining
            }
            _sleepRemainingMs.value = null
            if (playerBuilt) player.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        _sleepRemainingMs.value = null
    }

    // ------------------------------------------------------------------
    // Lifecycle helpers
    // ------------------------------------------------------------------

    /** Called when the app goes to background. */
    fun onAppBackgrounded() {
        if (!backgroundPlay.value && playerBuilt) {
            player.pause()
        }
    }

    /** Called when leaving the player screen. */
    fun onLeavePlayer() {
        saveCurrentResumePosition()
    }

    fun saveCurrentResumePosition() {
        val entry = _currentEntry.value ?: return
        if (!playerBuilt) return
        val position = player.currentPosition
        val duration = player.duration
        if (position > 5_000L && (duration <= 0L || position < duration - 10_000L)) {
            viewModelScope.launch { store.saveResumePosition(entry.id, position) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        released = true
        tickerJob?.cancel()
        sleepJob?.cancel()
        seekWatchdogJob?.cancel()
        saveCurrentResumePosition()
        _equalizerManager.value?.release()
        if (playerBuilt) {
            player.removeListener(listener)
            player.release()
        }
    }
}
