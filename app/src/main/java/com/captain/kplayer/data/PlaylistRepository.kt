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

private val Context.playlistDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kplayer_playlists"
)

/** A user-named playlist: a name plus an ordered list of queue entries. */
data class NamedPlaylist(
    val name: String,
    val entries: List<QueueEntry>
)

/**
 * Named playlists persisted in DataStore (file "kplayer_playlists").
 *
 * Encoding (no new serialization dependency): one preference string.
 * Playlists are separated by '\n'; inside a playlist the name and the
 * entries are separated by '|'; entry fields are separated by '\u001F'
 * (unit separator). Every user-supplied field is Uri-encoded first, so
 * titles containing '|', newlines or control characters cannot corrupt
 * the format.
 */
class PlaylistRepository(private val context: Context) {

    private val playlistsKey = stringPreferencesKey("playlists_v1")

    val playlists: Flow<List<NamedPlaylist>> =
        context.playlistDataStore.data.map { prefs ->
            decode(prefs[playlistsKey].orEmpty())
        }

    /** Creates an empty playlist. Returns false when the name is blank or taken. */
    suspend fun createPlaylist(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        var created = false
        context.playlistDataStore.edit { prefs ->
            val current = decode(prefs[playlistsKey].orEmpty()).toMutableList()
            if (current.none { it.name.equals(trimmed, ignoreCase = true) }) {
                current.add(NamedPlaylist(trimmed, emptyList()))
                prefs[playlistsKey] = encode(current)
                created = true
            }
        }
        return created
    }

    /** Saves (creates or replaces) a playlist with these entries. */
    suspend fun savePlaylist(name: String, entries: List<QueueEntry>) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        context.playlistDataStore.edit { prefs ->
            val current = decode(prefs[playlistsKey].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.name.equals(trimmed, ignoreCase = true) }
            val playlist = NamedPlaylist(trimmed, entries)
            if (index >= 0) current[index] = playlist else current.add(playlist)
            prefs[playlistsKey] = encode(current)
        }
    }

    suspend fun deletePlaylist(name: String) {
        context.playlistDataStore.edit { prefs ->
            val current = decode(prefs[playlistsKey].orEmpty())
                .filterNot { it.name.equals(name, ignoreCase = true) }
            prefs[playlistsKey] = encode(current)
        }
    }

    /** Returns false when the new name is blank or already taken. */
    suspend fun renamePlaylist(oldName: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        var renamed = false
        context.playlistDataStore.edit { prefs ->
            val current = decode(prefs[playlistsKey].orEmpty()).toMutableList()
            if (current.any { it.name.equals(trimmed, ignoreCase = true) }) return@edit
            val index = current.indexOfFirst { it.name.equals(oldName, ignoreCase = true) }
            if (index >= 0) {
                current[index] = current[index].copy(name = trimmed)
                prefs[playlistsKey] = encode(current)
                renamed = true
            }
        }
        return renamed
    }

    /** Appends one entry; returns false when the playlist does not exist. */
    suspend fun addToPlaylist(name: String, entry: QueueEntry): Boolean {
        var added = false
        context.playlistDataStore.edit { prefs ->
            val current = decode(prefs[playlistsKey].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.name.equals(name, ignoreCase = true) }
            if (index >= 0) {
                current[index] = current[index].copy(
                    entries = current[index].entries + entry
                )
                prefs[playlistsKey] = encode(current)
                added = true
            }
        }
        return added
    }

    suspend fun removeFromPlaylist(name: String, entryId: String) {
        context.playlistDataStore.edit { prefs ->
            val current = decode(prefs[playlistsKey].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.name.equals(name, ignoreCase = true) }
            if (index >= 0) {
                current[index] = current[index].copy(
                    entries = current[index].entries.filterNot { it.id == entryId }
                )
                prefs[playlistsKey] = encode(current)
            }
        }
    }

    /** Moves an entry inside a playlist (drag-to-reorder target). */
    suspend fun moveInPlaylist(name: String, from: Int, to: Int) {
        context.playlistDataStore.edit { prefs ->
            val current = decode(prefs[playlistsKey].orEmpty()).toMutableList()
            val index = current.indexOfFirst { it.name.equals(name, ignoreCase = true) }
            if (index < 0) return@edit
            val entries = current[index].entries.toMutableList()
            if (from !in entries.indices || to !in entries.indices || from == to) return@edit
            val item = entries.removeAt(from)
            entries.add(to, item)
            current[index] = current[index].copy(entries = entries)
            prefs[playlistsKey] = encode(current)
        }
    }

    private fun encode(playlists: List<NamedPlaylist>): String {
        return playlists.joinToString("\n") { playlist ->
            val entries = playlist.entries.joinToString(";") { entry ->
                listOf(
                    entry.id,
                    entry.title,
                    entry.subtitle,
                    entry.durationMs.toString(),
                    entry.isVideo.toString(),
                    entry.url,
                    entry.artKey
                ).joinToString("\u001F") { Uri.encode(it) }
            }
            Uri.encode(playlist.name) + "|" + entries
        }
    }

    private fun decode(raw: String): List<NamedPlaylist> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n").mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val bar = line.indexOf('|')
            if (bar < 0) return@mapNotNull null
            val name = runCatching { Uri.decode(line.substring(0, bar)) }.getOrNull()
                ?: return@mapNotNull null
            val entriesRaw = line.substring(bar + 1)
            val entries = if (entriesRaw.isBlank()) {
                emptyList()
            } else {
                entriesRaw.split(";").mapNotNull { encoded ->
                    val fields = encoded.split("\u001F")
                    if (fields.size != 7) return@mapNotNull null
                    val decoded = fields.map {
                        runCatching { Uri.decode(it) }.getOrNull() ?: return@mapNotNull null
                    }
                    QueueEntry(
                        id = decoded[0],
                        title = decoded[1],
                        subtitle = decoded[2],
                        durationMs = decoded[3].toLongOrNull() ?: 0L,
                        isVideo = decoded[4].toBooleanStrictOrNull() ?: true,
                        url = decoded[5],
                        artKey = decoded[6]
                    )
                }
            }
            NamedPlaylist(name, entries)
        }
    }
}
