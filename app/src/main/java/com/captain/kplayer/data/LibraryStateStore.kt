package com.captain.kplayer.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.libraryStateDataStore: DataStore<Preferences> by
preferencesDataStore("kplayer_library_state")

/**
 * v2.0 library state, backed by its own DataStore Preferences file
 * ("kplayer_library_state") so library ids can be used as dynamic keys
 * without polluting the settings or resume stores.
 *
 * - Watched flags: set of video ids the user marked as watched.
 * - Private ids: set of video ids moved into the PIN-locked Private folder.
 *   Membership is logical only — the file itself is never moved or renamed,
 *   so playback, resume positions and MediaStore rows keep working; the
 *   video is simply hidden from the main library until moved back out.
 * - Hidden folders: set of folder paths (FolderItem.path, e.g.
 *   "/storage/Camera") hidden from the Folders tab.
 * - Private PIN: a 4-digit PIN stored ONLY as a SHA-256 hash with a
 *   per-device random salt. The plaintext PIN is never stored.
 */
class LibraryStateStore(private val context: Context) {

    private object Keys {
        val WATCHED_IDS = stringSetPreferencesKey("watched_ids")
        val PRIVATE_IDS = stringSetPreferencesKey("private_ids")
        val HIDDEN_FOLDERS = stringSetPreferencesKey("hidden_folders")
        val PIN_HASH = stringPreferencesKey("private_pin_hash")
        val PIN_SALT = stringPreferencesKey("private_pin_salt")
    }

    val watchedIds: Flow<Set<String>> =
        context.libraryStateDataStore.data.map { it[Keys.WATCHED_IDS] ?: emptySet() }

    val privateIds: Flow<Set<String>> =
        context.libraryStateDataStore.data.map { it[Keys.PRIVATE_IDS] ?: emptySet() }

    val hiddenFolders: Flow<Set<String>> =
        context.libraryStateDataStore.data.map { it[Keys.HIDDEN_FOLDERS] ?: emptySet() }

    /** True once a Private-folder PIN has been set. */
    val hasPin: Flow<Boolean> =
        context.libraryStateDataStore.data.map { it[Keys.PIN_HASH] != null }

    suspend fun setWatched(videoId: String, watched: Boolean) {
        context.libraryStateDataStore.edit { prefs ->
            val current = (prefs[Keys.WATCHED_IDS] ?: emptySet()).toMutableSet()
            if (watched) current.add(videoId) else current.remove(videoId)
            prefs[Keys.WATCHED_IDS] = current
        }
    }

    /** Marks every id in [videoIds] as watched (used by "Mark all as watched"). */
    suspend fun markAllWatched(videoIds: Collection<String>) {
        if (videoIds.isEmpty()) return
        context.libraryStateDataStore.edit { prefs ->
            val current = (prefs[Keys.WATCHED_IDS] ?: emptySet()).toMutableSet()
            current.addAll(videoIds)
            prefs[Keys.WATCHED_IDS] = current
        }
    }

    suspend fun clearWatched() {
        context.libraryStateDataStore.edit { it[Keys.WATCHED_IDS] = emptySet() }
    }

    suspend fun setPrivate(videoId: String, isPrivate: Boolean) {
        context.libraryStateDataStore.edit { prefs ->
            val current = (prefs[Keys.PRIVATE_IDS] ?: emptySet()).toMutableSet()
            if (isPrivate) current.add(videoId) else current.remove(videoId)
            prefs[Keys.PRIVATE_IDS] = current
        }
    }

    suspend fun setFolderHidden(folderPath: String, hidden: Boolean) {
        context.libraryStateDataStore.edit { prefs ->
            val current = (prefs[Keys.HIDDEN_FOLDERS] ?: emptySet()).toMutableSet()
            if (hidden) current.add(folderPath) else current.remove(folderPath)
            prefs[Keys.HIDDEN_FOLDERS] = current
        }
    }

    /**
     * Sets (or replaces) the Private-folder PIN. A fresh random salt is
     * generated every time so the stored hash changes even for the same PIN.
     * Returns false when [pin] is not exactly 4 digits.
     */
    suspend fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        context.libraryStateDataStore.edit { prefs ->
            prefs[Keys.PIN_SALT] = salt
            prefs[Keys.PIN_HASH] = hashPin(pin, salt)
        }
        return true
    }

    /**
     * Verifies [pin] against the stored hash. Returns false when no PIN
     * has been set yet, or when the PIN is wrong. Runs in constant-ish time
     * for the comparison via [MessageDigest.isEqual].
     */
    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.libraryStateDataStore.data.first()
        val salt = prefs[Keys.PIN_SALT] ?: return false
        val storedHash = prefs[Keys.PIN_HASH] ?: return false
        val candidate = hashPin(pin, salt)
        return MessageDigest.isEqual(
            storedHash.toByteArray(Charsets.UTF_8),
            candidate.toByteArray(Charsets.UTF_8)
        )
    }

    /** A valid PIN is exactly 4 ASCII digits. */
    fun isValidPin(pin: String): Boolean =
        pin.length == 4 && pin.all { it in '0'..'9' }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
