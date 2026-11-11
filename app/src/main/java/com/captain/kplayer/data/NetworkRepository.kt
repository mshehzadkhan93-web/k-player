package com.captain.kplayer.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.networkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kplayer_network"
)

/**
 * A saved SMB/LAN server. Only host, share and username are persisted —
 * the password is NEVER stored: the user types it each time they connect
 * and it lives only in the Network screen's in-memory state.
 */
data class SmbServer(
    val host: String,
    val share: String,
    val user: String
)

/**
 * Network state persisted in DataStore (file "kplayer_network"):
 * the stream-URL history and the saved SMB servers.
 * Same Uri-encoding scheme as [PlaylistRepository] — no new dependency.
 */
class NetworkRepository(private val context: Context) {

    private val historyKey = stringPreferencesKey("stream_history_v1")
    private val serversKey = stringPreferencesKey("smb_servers_v1")

    /** Stream history, most recent first, max 20 entries. */
    val streamHistory: Flow<List<StreamItem>> =
        context.networkDataStore.data.map { prefs ->
            decodeHistory(prefs[historyKey].orEmpty())
        }

    suspend fun addStreamToHistory(url: String, title: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        context.networkDataStore.edit { prefs ->
            val current = decodeHistory(prefs[historyKey].orEmpty())
                .filterNot { it.url == trimmed }
            val updated = listOf(
                StreamItem(trimmed, title.ifBlank { trimmed }, "Just now")
            ) + current
            prefs[historyKey] = encodeHistory(updated.take(MAX_HISTORY))
        }
    }

    suspend fun removeStreamFromHistory(url: String) {
        context.networkDataStore.edit { prefs ->
            val current = decodeHistory(prefs[historyKey].orEmpty())
                .filterNot { it.url == url }
            prefs[historyKey] = encodeHistory(current)
        }
    }

    suspend fun clearStreamHistory() {
        context.networkDataStore.edit { prefs -> prefs[historyKey] = "" }
    }

    /** Saved SMB servers (host/share/user only — never passwords). */
    val smbServers: Flow<List<SmbServer>> =
        context.networkDataStore.data.map { prefs ->
            decodeServers(prefs[serversKey].orEmpty())
        }

    suspend fun saveSmbServer(server: SmbServer) {
        val host = server.host.trim()
        if (host.isEmpty()) return
        val clean = server.copy(host = host, share = server.share.trim(), user = server.user.trim())
        context.networkDataStore.edit { prefs ->
            val current = decodeServers(prefs[serversKey].orEmpty())
                .filterNot { it.host.equals(clean.host, ignoreCase = true) && it.share == clean.share }
            prefs[serversKey] = encodeServers(listOf(clean) + current)
        }
    }

    suspend fun removeSmbServer(server: SmbServer) {
        context.networkDataStore.edit { prefs ->
            val current = decodeServers(prefs[serversKey].orEmpty())
                .filterNot { it.host == server.host && it.share == server.share }
            prefs[serversKey] = encodeServers(current)
        }
    }

    private fun encodeHistory(items: List<StreamItem>): String =
        items.joinToString("\n") {
            listOf(it.url, it.title, it.addedLabel).joinToString("|") { field -> Uri.encode(field) }
        }

    private fun decodeHistory(raw: String): List<StreamItem> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val fields = line.split("|")
            if (fields.size != 3) return@mapNotNull null
            val decoded = fields.map {
                runCatching { Uri.decode(it) }.getOrNull() ?: return@mapNotNull null
            }
            StreamItem(decoded[0], decoded[1], decoded[2])
        }
    }

    private fun encodeServers(servers: List<SmbServer>): String =
        servers.joinToString("\n") {
            listOf(it.host, it.share, it.user).joinToString("|") { field -> Uri.encode(field) }
        }

    private fun decodeServers(raw: String): List<SmbServer> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            val fields = line.split("|")
            if (fields.size != 3) return@mapNotNull null
            val decoded = fields.map {
                runCatching { Uri.decode(it) }.getOrNull() ?: return@mapNotNull null
            }
            SmbServer(decoded[0], decoded[1], decoded[2])
        }
    }

    companion object {
        const val MAX_HISTORY = 20

        /** Video extensions browsable/playable from SMB and DLNA. */
        val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm",
            "m4v", "3gp", "ts", "m2ts", "mpg", "mpeg", "vob"
        )

        /** Audio extensions browsable/playable from SMB. */
        val AUDIO_EXTENSIONS = setOf(
            "mp3", "m4a", "flac", "wav", "ogg", "opus", "aac", "wma", "aiff"
        )

        /** True for video or audio files K-Player can play. */
        fun isMediaFile(name: String): Boolean {
            val ext = name.substringAfterLast('.', "").lowercase()
            return ext in VIDEO_EXTENSIONS || ext in AUDIO_EXTENSIONS
        }

        fun isVideoFile(name: String): Boolean {
            val ext = name.substringAfterLast('.', "").lowercase()
            return ext in VIDEO_EXTENSIONS
        }
    }
}
