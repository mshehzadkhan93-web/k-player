package com.captain.kplayer.data

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import java.io.IOException
import java.io.InputStream

/**
 * A Media3 [DataSource] that reads straight from an SMB share — true
 * streaming with seeking, no download needed.
 *
 * NOT WIRED IN YET (deliberate): the app's [com.captain.kplayer.player.PlayerFactory]
 * builds ExoPlayer with the default data-source factory, and that file is
 * outside this package's scope. To enable SMB streaming, the coordinator
 * applies this one change in PlayerFactory.buildPlayer:
 *
 *     val mediaSourceFactory = DefaultMediaSourceFactory(context)
 *         .setDataSourceFactory(
 *             DefaultDataSource.Factory(
 *                 context,
 *                 SmbDataSourceFactory(smbContext) // handles smb://, falls back otherwise
 *             )
 *         )
 *     return ExoPlayer.Builder(context, renderersFactory)
 *         .setLoadControl(loadControl)
 *         .setMediaSourceFactory(mediaSourceFactory)
 *         .build()
 *
 * (DefaultDataSource.Factory takes a fallback base factory; an smb:// URI
 * must be routed to this factory — the simplest correct wiring is a
 * small delegating factory that picks [SmbDataSourceFactory] for the
 * "smb" scheme and DefaultDataSource.Factory otherwise.)
 *
 * Until then, SMB playback uses [SmbBrowser.downloadToCache]. This class
 * compiles once eu.agno3.jcifs:jcifs-ng:2.1.9 is added (see report).
 */
class SmbDataSource(
    private val cifsContext: CIFSContext
) : DataSource {

    private var input: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0L
    private var opened = false
    private val transferListeners = mutableSetOf<TransferListener>()

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        val smbUri = dataSpec.uri
        try {
            val file = SmbFile(smbUri.toString(), cifsContext)
            val stream = file.inputStream
            var toSkip = dataSpec.position
            while (toSkip > 0) {
                val skipped = stream.skip(toSkip)
                if (skipped <= 0L) break
                toSkip -= skipped
            }
            input = stream
            uri = smbUri
            val totalLength = file.length()
            bytesRemaining = when {
                dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
                totalLength > 0 -> (totalLength - dataSpec.position).coerceAtLeast(0L)
                else -> C.LENGTH_UNSET.toLong()
            }
            opened = true
            return bytesRemaining
        } catch (e: Exception) {
            throw IOException("Could not open SMB file: ${smbUri}", e)
        }
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val stream = input ?: return C.RESULT_END_OF_INPUT
        val toRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length
        } else {
            minOf(length.toLong(), bytesRemaining).toInt()
        }
        val read = try {
            stream.read(buffer, offset, toRead)
        } catch (e: Exception) {
            throw IOException("Could not read SMB file", e)
        }
        if (read == -1) return C.RESULT_END_OF_INPUT
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
        return read
    }

    override fun getUri(): Uri? = uri

    @Throws(IOException::class)
    override fun close() {
        if (!opened) return
        opened = false
        try {
            input?.close()
        } catch (_: Exception) {
        } finally {
            input = null
            uri = null
        }
    }

    // Transfer listeners are accepted (the DataSource contract) but not
    // notified: their callback signatures changed across Media3 versions
    // and bandwidth tracking is not needed for LAN playback. There is
    // no removeTransferListener in Media3 1.2.1's DataSource interface
    // (only addTransferListener), so none is declared here.

    override fun addTransferListener(transferListener: TransferListener) {
        transferListeners.add(transferListener)
    }
}

/** Factory for [SmbDataSource]; one instance per SMB credential context. */
class SmbDataSourceFactory(
    private val cifsContext: CIFSContext
) : DataSource.Factory {
    override fun createDataSource(): DataSource = SmbDataSource(cifsContext)
}
