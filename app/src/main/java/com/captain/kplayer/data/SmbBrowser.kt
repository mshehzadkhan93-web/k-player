package com.captain.kplayer.data

import android.content.Context
import jcifs.CIFSException
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Properties

/**
 * One entry in an SMB share listing: a folder, a media file, or a share.
 * [smbUrl] is the full "smb://host/share/path" URL used to open it.
 */
data class SmbEntry(
    val name: String,
    val smbUrl: String,
    val isDirectory: Boolean,
    val sizeBytes: Long
)

/**
 * SMB (Windows / NAS share) browsing via jcifs-ng 2.1.9
 * (eu.agno3.jcifs:jcifs-ng:2.1.9 — NEW dependency, see report).
 *
 * Requires SMB2 or newer on the server: minVersion is forced to SMB202,
 * so the insecure SMB1 dialect is never negotiated. All network I/O runs
 * on Dispatchers.IO and every call is wrapped in [Result] — a bad host,
 * wrong password or unreachable server surfaces as a message, never a
 * crash. Guest (anonymous) login is used when user and password are blank.
 *
 * PLAYBACK NOTE: ExoPlayer cannot read "smb://" URIs with its default
 * data sources, and the app's PlayerFactory cannot be edited by this
 * package. So playback here is download-to-cache: the file is copied
 * into the app cache (see [downloadToCache]) and the local file is
 * played with the existing player. [SmbDataSource] is the true-streaming
 * alternative — it is ready to be plugged into PlayerFactory with a
 * one-line change (see its header) so SMB can stream without downloading.
 */
class SmbBrowser {

    private fun buildContext(user: String, password: String): CIFSContext {
        val props = Properties()
        // SMB2 minimum: SMB1 (CIFS) is insecure and disabled on modern
        // Windows/NAS boxes; never negotiate down to it.
        props.setProperty("jcifs.smb.client.minVersion", "SMB202")
        props.setProperty("jcifs.smb.client.maxVersion", "SMB311")
        val base = BaseContext(PropertyConfiguration(props))
        return if (user.isBlank() && password.isBlank()) {
            // No credentials attached: a context without credentials
            // logs on anonymously (guest). jcifs-ng 2.x has no
            // NtlmPasswordAuthenticator.ANONYMOUS constant — the
            // default (credential-less) context is the anonymous one.
            base
        } else {
            base.withCredentials(NtlmPasswordAuthenticator(null, user, password))
        }
    }

    private fun listSmb(url: String, ctx: CIFSContext): List<SmbEntry> {
        val dir = SmbFile(url, ctx)
        val children = try {
            dir.listFiles() ?: emptyArray()
        } catch (e: CIFSException) {
            throw e
        }
        return children.mapNotNull { child ->
            runCatching {
                val isDir = child.isDirectory
                // Only folders and playable media are listed; stray
                // documents / images on a share are hidden.
                if (!isDir && !NetworkRepository.isMediaFile(child.name)) return@mapNotNull null
                SmbEntry(
                    name = child.name.trimEnd('/'),
                    smbUrl = child.url.toString(),
                    isDirectory = isDir,
                    sizeBytes = if (isDir) 0L else child.length()
                )
            }.getOrNull()
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    /** Lists the shares on [host] ("smb://host/"). */
    suspend fun listShares(host: String, user: String, password: String): Result<List<SmbEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                listSmb("smb://${host.trim()}/", buildContext(user, password))
            }
        }

    /** Lists one folder (or share) by its full smb:// URL. */
    suspend fun listPath(smbUrl: String, user: String, password: String): Result<List<SmbEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                listSmb(smbUrl, buildContext(user, password))
            }
        }

    /**
     * Copies an SMB file into the app cache ("smb-cache/") so the
     * existing player can play it as a local file. [onProgress] reports
     * 0..1. Returns the local file. Callers should delete old cache
     * files when they are done (the OS may also purge the cache dir).
     */
    suspend fun downloadToCache(
        context: Context,
        entry: SmbEntry,
        user: String,
        password: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val ctx = buildContext(user, password)
            val remote = SmbFile(entry.smbUrl, ctx)
            val total = remote.length().coerceAtLeast(1L)
            val dir = File(context.cacheDir, "smb-cache").apply { mkdirs() }
            val safeName = entry.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val local = File(dir, "${entry.smbUrl.hashCode()}_$safeName")
            remote.inputStream.use { input ->
                FileOutputStream(local).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (isActive) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        onProgress((copied.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            onProgress(1f)
            local
        }
    }
}
