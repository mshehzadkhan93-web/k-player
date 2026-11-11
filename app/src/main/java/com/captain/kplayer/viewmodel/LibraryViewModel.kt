package com.captain.kplayer.viewmodel

import android.content.Context
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.captain.kplayer.data.FolderItem
import com.captain.kplayer.data.LibraryStateStore
import com.captain.kplayer.data.MediaStoreRepository
import com.captain.kplayer.data.SampleData
import com.captain.kplayer.data.StreamItem
import com.captain.kplayer.data.TrackItem
import com.captain.kplayer.data.VideoItem
import com.captain.kplayer.ui.components.HomeTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VideoSort { NAME, DATE, SIZE, DURATION, RESOLUTION }
enum class VideoViewMode { GRID, LIST }

/** Library state: videos, music, folders and network streams. */
class LibraryViewModel : ViewModel() {

    private val allVideos = MutableStateFlow(SampleData.videos)
    private val allTracks = MutableStateFlow(SampleData.tracks)
    private val allFolders = MutableStateFlow(SampleData.folders)
    private val _streams = MutableStateFlow(SampleData.recentStreams)

    /** True when the library shows real device files instead of samples. */
    private val _usingDeviceMedia = MutableStateFlow(false)
    val usingDeviceMedia: StateFlow<Boolean> = _usingDeviceMedia

    /**
     * The resume store (same kplayer_resume DataStore as the resume
     * positions), attached once from Home so last-played stamps are
     * persisted and reloaded across restarts.
     */
    private var settingsStore: SettingsStore? = null
    private var lastPlayedById: Map<String, Long> = emptyMap()

    // ------------------------------------------------------------------
    // v2.0 library state (watched flags, Private folder, hidden folders,
    // PIN) — persisted in LibraryStateStore, attached once from Home.
    // ------------------------------------------------------------------

    private var libraryStore: LibraryStateStore? = null
    private val privateIds = MutableStateFlow<Set<String>>(emptySet())
    private val hiddenFolderPaths = MutableStateFlow<Set<String>>(emptySet())
    private var lastWatchedIds: Set<String> = emptySet()

    /** Videos in the PIN-locked Private folder (hidden from the main library). */
    val privateVideos: StateFlow<List<VideoItem>> =
        combine(allVideos, privateIds) { videos, private ->
            videos.filter { it.id in private }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** All videos except the ones moved into the Private folder. */
    val allVisibleVideos: StateFlow<List<VideoItem>> =
        combine(allVideos, privateIds) { videos, private ->
            videos.filter { it.id !in private }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SampleData.videos)

    /** Folder paths the user hid from the Folders tab. */
    val hiddenFolders: StateFlow<Set<String>> = hiddenFolderPaths

    /** True once a Private-folder PIN has been set. */
    private val _hasPin = MutableStateFlow(false)
    val hasPin: StateFlow<Boolean> = _hasPin

    /**
     * True while the Private folder is unlocked for this session. Starts
     * locked on every process start; unlocking never persists, so the
     * folder re-locks when the app is killed.
     */
    private val _privateUnlocked = MutableStateFlow(false)
    val privateUnlocked: StateFlow<Boolean> = _privateUnlocked

    fun unlockPrivate() {
        _privateUnlocked.value = true
    }

    fun lockPrivate() {
        _privateUnlocked.value = false
    }

    /**
     * Attaches the v2.0 library store and starts collecting watched flags,
     * Private membership and hidden folders into the in-memory library.
     * Idempotent — later calls are ignored.
     */
    fun attachLibraryStore(context: Context) {
        if (libraryStore != null) return
        val store = LibraryStateStore(context.applicationContext)
        libraryStore = store
        viewModelScope.launch {
            store.watchedIds.collect { ids -> applyWatched(ids) }
        }
        viewModelScope.launch {
            store.privateIds.collect { privateIds.value = it }
        }
        viewModelScope.launch {
            store.hiddenFolders.collect { hiddenFolderPaths.value = it }
        }
        viewModelScope.launch {
            store.hasPin.collect { _hasPin.value = it }
        }
    }

    /** Applies persisted watched flags to the videos currently in the library. */
    private fun applyWatched(watched: Set<String>) {
        lastWatchedIds = watched
        if (watched.isEmpty() && allVideos.value.none { it.isWatched }) return
        allVideos.value = allVideos.value.map { video ->
            val shouldBe = video.id in watched
            if (video.isWatched != shouldBe) video.copy(isWatched = shouldBe) else video
        }
    }

    /** Toggles the watched flag for one video (persisted). */
    fun toggleWatched(video: VideoItem) {
        val store = libraryStore ?: return
        viewModelScope.launch { store.setWatched(video.id, !video.isWatched) }
    }

    /** Marks one video watched / unwatched (persisted). */
    fun setWatched(video: VideoItem, watched: Boolean) {
        val store = libraryStore ?: return
        viewModelScope.launch { store.setWatched(video.id, watched) }
    }

    /** Marks every currently visible video as watched (persisted). */
    fun markAllWatched() {
        val store = libraryStore ?: return
        val ids = allVisibleVideos.value.map { it.id }
        viewModelScope.launch { store.markAllWatched(ids) }
    }

    /** Moves a video into the Private folder (hidden from the main library). */
    fun moveToPrivate(video: VideoItem) {
        val store = libraryStore ?: return
        viewModelScope.launch { store.setPrivate(video.id, true) }
    }

    /** Moves a video out of the Private folder back into the main library. */
    fun moveOutOfPrivate(video: VideoItem) {
        val store = libraryStore ?: return
        viewModelScope.launch { store.setPrivate(video.id, false) }
    }

    /** Hides a folder (by FolderItem.path) from the Folders tab. */
    fun hideFolder(folderPath: String) {
        val store = libraryStore ?: return
        viewModelScope.launch { store.setFolderHidden(folderPath, true) }
    }

    /** Unhides a previously hidden folder. */
    fun unhideFolder(folderPath: String) {
        val store = libraryStore ?: return
        viewModelScope.launch { store.setFolderHidden(folderPath, false) }
    }

    /**
     * Sets the Private-folder PIN (first use, or after verifying the old
     * one via [changePin]). The PIN is stored only as a salted SHA-256
     * hash — see [LibraryStateStore]. [onResult] gets false when the PIN
     * is not exactly 4 digits.
     */
    fun setPin(pin: String, onResult: (Boolean) -> Unit) {
        val store = libraryStore
        if (store == null) {
            onResult(false)
            return
        }
        viewModelScope.launch { onResult(store.setPin(pin)) }
    }

    /**
     * Verifies the PIN for unlocking the Private folder. On success the
     * folder is unlocked for this session.
     */
    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        val store = libraryStore
        if (store == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val ok = store.verifyPin(pin)
            if (ok) _privateUnlocked.value = true
            onResult(ok)
        }
    }

    /**
     * Changes the PIN: the old PIN must verify first, then the new one is
     * stored (with a fresh salt). [onResult] gets false when the old PIN
     * is wrong or the new PIN is not exactly 4 digits.
     */
    fun changePin(oldPin: String, newPin: String, onResult: (Boolean) -> Unit) {
        val store = libraryStore
        if (store == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            if (!store.verifyPin(oldPin)) {
                onResult(false)
                return@launch
            }
            onResult(store.setPin(newPin))
        }
    }

    // ------------------------------------------------------------------
    // Delete / rename (long-press menu actions)
    // ------------------------------------------------------------------

    /** A delete that is waiting on the user's confirmation tap (Android 10+). */
    private var pendingDelete: VideoItem? = null

    /**
     * Deletes a video: MediaStore row (+ file) for on-device files, or
     * just the in-memory entry for sample/stream items. When Android
     * requires explicit user consent, [onNeedsConfirmation] receives the
     * IntentSender to launch (StartIntentSenderForResult) and the delete
     * is retried via [retryPendingDelete] when the user confirms.
     */
    fun deleteVideo(
        context: Context,
        video: VideoItem,
        onNeedsConfirmation: (IntentSender) -> Unit,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = MediaStoreRepository.deleteVideo(context.applicationContext, video)) {
                is MediaStoreRepository.DeleteResult.Deleted -> {
                    removeFromLibrary(video.id)
                    onResult(true, "Video deleted")
                }
                is MediaStoreRepository.DeleteResult.NeedsUserConfirmation -> {
                    pendingDelete = video
                    onNeedsConfirmation(result.intentSender)
                }
                is MediaStoreRepository.DeleteResult.Failed -> {
                    onResult(false, result.message)
                }
            }
        }
    }

    /**
     * Retries the pending delete after the user confirmed the system
     * delete dialog (RESULT_OK). Call with the same callbacks as
     * [deleteVideo]; does nothing when there is no pending delete.
     */
    fun retryPendingDelete(
        context: Context,
        onNeedsConfirmation: (IntentSender) -> Unit,
        onResult: (Boolean, String) -> Unit
    ) {
        val video = pendingDelete ?: return
        pendingDelete = null
        deleteVideo(context, video, onNeedsConfirmation, onResult)
    }

    /**
     * Renames a video. On-device files are renamed in MediaStore
     * (extension preserved); sample/stream items are renamed in memory
     * only. [onResult] gets false only when a real file rename failed.
     */
    fun renameVideo(
        context: Context,
        video: VideoItem,
        newName: String,
        onResult: (Boolean) -> Unit
    ) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == video.title) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val isOnDevice = video.url.startsWith("content://")
            val renamed = if (isOnDevice) {
                MediaStoreRepository.renameVideo(context.applicationContext, video, trimmed)
            } else {
                true // nothing on disk — in-memory rename only
            }
            if (renamed) {
                allVideos.value = allVideos.value.map {
                    if (it.id == video.id) it.copy(title = trimmed) else it
                }
            }
            onResult(renamed)
        }
    }

    private fun removeFromLibrary(videoId: String) {
        allVideos.value = allVideos.value.filter { it.id != videoId }
    }

    /**
     * Attaches the store and loads every saved played_<id> timestamp into
     * the in-memory library. Idempotent — later calls are ignored.
     */
    fun attachStore(store: SettingsStore) {
        if (settingsStore != null) return
        settingsStore = store
        viewModelScope.launch {
            store.allLastPlayed.collect { played ->
                applyLastPlayed(played)
            }
        }
    }

    /** Applies persisted timestamps to the videos currently in the library. */
    private fun applyLastPlayed(played: Map<String, Long>) {
        lastPlayedById = played
        if (played.isEmpty()) return
        allVideos.value = allVideos.value.map { video ->
            val ts = played[video.id]
            if (ts != null && ts > video.lastPlayedMs) video.copy(lastPlayedMs = ts) else video
        }
    }

    /**
     * Loads the saved resume positions (the same kplayer_resume DataStore
     * [PlayerViewModel] writes via SettingsStore) into the in-memory
     * library, so Continue Watching and the per-thumbnail progress bars
     * show real positions instead of only the sample data's. Call after a
     * scan and when returning to Home — playback saves positions while
     * this ViewModel is not observing them. Positions at/below 5s or
     * within 10s of the end are treated as "not started / finished" and
     * cleared, matching the player's own save rule.
     */
    fun refreshResumePositions() {
        val store = settingsStore ?: return
        viewModelScope.launch {
            val positions = mutableMapOf<String, Long>()
            for (video in allVideos.value) {
                val saved = runCatching { store.resumePositionFlow(video.id).first() }
                    .getOrDefault(0L)
                val cleaned =
                    if (saved <= 5_000L || saved >= video.durationMs - 10_000L) 0L else saved
                if (cleaned != video.resumeMs) positions[video.id] = cleaned
            }
            if (positions.isNotEmpty()) {
                allVideos.value = allVideos.value.map { video ->
                    val pos = positions[video.id]
                    if (pos != null) video.copy(resumeMs = pos) else video
                }
            }
        }
    }

    /**
     * Scans the device with MediaStore. When real videos or songs are found
     * they replace the sample library; otherwise the samples stay as fallback.
     * Hidden (dot) files are skipped unless [showHiddenFiles] is on.
     */
    fun scanDeviceMedia(context: Context, showHiddenFiles: Boolean = false) {
        viewModelScope.launch {
            val result = MediaStoreRepository.scan(context.applicationContext, showHiddenFiles)
            if (result.videos.isNotEmpty()) {
                allVideos.value = result.videos
                // A fresh scan replaces the list — re-apply the persisted
                // last-played stamps and watched flags so Recently Played
                // and watched badges survive the scan.
                applyLastPlayed(lastPlayedById)
                applyWatched(lastWatchedIds)
                refreshResumePositions()
            }
            if (result.tracks.isNotEmpty()) {
                allTracks.value = result.tracks
            }
            if (result.folders.isNotEmpty()) {
                allFolders.value = result.folders
            }
            _usingDeviceMedia.value = result.videos.isNotEmpty() || result.tracks.isNotEmpty()
        }
    }

    val streams: StateFlow<List<StreamItem>> = _streams

    /**
     * Task C: the selected home tab lives here, not in a composable's
     * remember — HOME leaves composition on every forward navigation
     * (player, folder, music player, Settings), which wiped a remembered
     * tab and always dropped the user back on Videos. Kept in the
     * ViewModel, Back returns to the tab the user was on (Folders after a
     * folder, Music after the music player, Network after a stream,
     * whatever tab Settings was opened from).
     */
    private val _selectedTab = MutableStateFlow(HomeTab.VIDEOS)
    val selectedTab: StateFlow<HomeTab> = _selectedTab

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    val searchQuery = MutableStateFlow("")
    val sortMode = MutableStateFlow(VideoSort.NAME)
    val viewMode = MutableStateFlow(VideoViewMode.GRID)
    val resolutionFilter = MutableStateFlow("All")

    /** Videos the user has started watching, for the Continue Watching row. */
    val continueWatching: StateFlow<List<VideoItem>> =
        allVisibleVideos
            .map { videos -> videos.filter { it.resumeMs > 0 } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Recently Played: videos the user has played, most recently played
     * first, max 10. A video counts as played when it has a last-played
     * stamp ([markVideoPlayed]) or a saved resume position. The row needs
     * title + last-played time, both on [VideoItem].
     */
    val recentlyPlayed: StateFlow<List<VideoItem>> =
        allVisibleVideos
            .map { videos ->
                videos.filter { it.lastPlayedMs > 0L || it.resumeMs > 0L }
                    .sortedByDescending { it.lastPlayedMs }
                    .take(10)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Videos after search text, resolution filter and sort are applied.
     * Private-folder videos and videos in hidden folders are excluded.
     */
    val visibleVideos: StateFlow<List<VideoItem>> = combine(
        allVisibleVideos, searchQuery, sortMode, resolutionFilter, hiddenFolderPaths
    ) { videos, query, sort, filter, hidden ->
        var list = videos
        if (hidden.isNotEmpty()) {
            list = list.filter { "/storage/${it.genre}" !in hidden }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.genre.contains(query, ignoreCase = true)
            }
        }
        if (filter != "All") {
            list = list.filter { it.resolution == filter }
        }
        when (sort) {
            VideoSort.NAME -> list.sortedBy { it.title }
            VideoSort.DATE -> list.sortedByDescending { it.dateAddedMs }
            VideoSort.SIZE -> list.sortedByDescending { it.sizeBytes }
            VideoSort.DURATION -> list.sortedByDescending { it.durationMs }
            VideoSort.RESOLUTION -> list.sortedByDescending { it.resolution }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SampleData.videos)

    val visibleTracks: StateFlow<List<TrackItem>> = combine(
        allTracks, searchQuery
    ) { tracks, query ->
        if (query.isBlank()) tracks else tracks.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SampleData.tracks)

    /** Folders after hidden folders are removed (hidden ones live in the overflow menu). */
    val folders: StateFlow<List<FolderItem>> = combine(
        allFolders, hiddenFolderPaths
    ) { folders, hidden ->
        folders.filter { it.path !in hidden }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SampleData.folders)

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortMode(sort: VideoSort) {
        sortMode.value = sort
    }

    /** Applies the persisted Interface defaults. Safe to call repeatedly. */
    fun applyDefaults(defaultView: String, defaultSort: String) {
        viewMode.value = if (defaultView == "list") VideoViewMode.LIST else VideoViewMode.GRID
        sortMode.value = when (defaultSort) {
            "date" -> VideoSort.DATE
            "size" -> VideoSort.SIZE
            "duration" -> VideoSort.DURATION
            "name" -> VideoSort.NAME
            else -> VideoSort.NAME
        }
    }

    /** Clears in-memory resume progress (watch history) for the session. */
    fun clearWatchHistory() {
        allVideos.value = allVideos.value.map {
            it.copy(resumeMs = 0L, lastPlayedMs = 0L)
        }
        settingsStore?.let { store ->
            viewModelScope.launch { store.clearLastPlayed() }
        }
    }

    /** Empties the recent streams list. */
    fun clearRecentStreams() {
        _streams.value = emptyList()
    }

    fun toggleViewMode() {
        viewMode.value =
            if (viewMode.value == VideoViewMode.GRID) VideoViewMode.LIST else VideoViewMode.GRID
    }

    fun setResolutionFilter(filter: String) {
        resolutionFilter.value = filter
    }

    /** Adds a stream URL to the recent list when it looks like a URL. */
    fun addStream(url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false
        val title = trimmed.substringAfterLast("/").ifBlank { trimmed }
        _streams.value = listOf(
            StreamItem(trimmed, title, "Just now")
        ) + _streams.value.filter { it.url != trimmed }
        return true
    }

    fun videoById(id: String): VideoItem? = allVideos.value.firstOrNull { it.id == id }

    /**
     * Records that a video was just played, stamping it with the current
     * time so it surfaces in [recentlyPlayed]. Call when playback starts.
     * Builds on the existing in-memory watch-history state (resumeMs);
     * no second store is introduced.
     */
    fun markVideoPlayed(videoId: String) {
        val now = System.currentTimeMillis()
        allVideos.value = allVideos.value.map {
            if (it.id == videoId) it.copy(lastPlayedMs = now) else it
        }
        // Persist the stamp in the same resume store so Recently Played
        // survives app restarts.
        settingsStore?.let { store ->
            viewModelScope.launch { store.saveLastPlayed(videoId, now) }
        }
    }

    /**
     * Videos in one folder, for the folder-detail screen. [folderPath] is
     * a [FolderItem.path] ("/storage/<bucket>") or a plain bucket name.
     * Folders are grouped by the MediaStore bucket, which is carried on
     * [VideoItem.genre] (see MediaStoreRepository). Private-folder videos
     * are excluded unless [includePrivate] is true.
     */
    fun videosInFolder(folderPath: String, includePrivate: Boolean = false): List<VideoItem> {
        val bucket = folderPath.substringAfterLast("/").ifBlank { folderPath }
        val source = if (includePrivate) allVideos.value else allVisibleVideos.value
        return source.filter { it.genre == bucket || it.genre == folderPath }
    }
}
