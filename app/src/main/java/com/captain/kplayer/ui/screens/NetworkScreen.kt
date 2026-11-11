package com.captain.kplayer.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.captain.kplayer.data.DlnaBrowser
import com.captain.kplayer.data.DlnaDiscovery
import com.captain.kplayer.data.DlnaItem
import com.captain.kplayer.data.DlnaServer
import com.captain.kplayer.data.NetworkRepository
import com.captain.kplayer.data.SampleData
import com.captain.kplayer.data.SmbBrowser
import com.captain.kplayer.data.SmbEntry
import com.captain.kplayer.data.SmbServer
import com.captain.kplayer.data.StreamItem
import com.captain.kplayer.data.TrackItem
import com.captain.kplayer.data.VideoItem
import com.captain.kplayer.ui.components.AccentGradientButton
import com.captain.kplayer.ui.components.GlassButton
import com.captain.kplayer.ui.components.GlassCard
import com.captain.kplayer.ui.components.GlassTextField
import com.captain.kplayer.ui.navigation.Routes
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.PinkAccent
import com.captain.kplayer.ui.theme.LocalTextMuted
import com.captain.kplayer.ui.theme.LocalTextPrimary
import com.captain.kplayer.ui.theme.LocalTextSecondary
import com.captain.kplayer.viewmodel.LibraryViewModel
import com.captain.kplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

private enum class NetworkMode { STREAM, SMB, DLNA }

/**
 * Network screen: stream a URL, browse a Windows/NAS shared folder
 * (SMB) and browse DLNA/UPnP media servers on the LAN.
 *
 * - Stream URLs and SMB servers are persisted in DataStore
 *   ([NetworkRepository]); SMB passwords are NEVER stored — the user
 *   types the password each time and it lives only in this screen's
 *   in-memory state.
 * - DLNA discovery + browsing need no extra library (SSDP + SOAP).
 * - SMB browsing needs eu.agno3.jcifs:jcifs-ng:2.1.9 (not yet added —
 *   see report). Until it is added, tapping Connect shows a plain
 *   message instead of crashing.
 * - Chromecast is deliberately NOT here: it needs manifest entries and
 *   the Cast SDK, which this package cannot add (see report).
 */
@Composable
fun NetworkScreen(
    navController: NavHostController,
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { NetworkRepository(context.applicationContext) }
    val smbBrowser = remember { SmbBrowser() }

    var mode by remember { mutableStateOf(NetworkMode.STREAM) }

    // --- Stream by URL ---
    var url by remember { mutableStateOf("") }
    var streamError by remember { mutableStateOf<String?>(null) }
    val history by repo.streamHistory.collectAsState(initial = emptyList())

    // --- SMB ---
    var host by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var smbUser by remember { mutableStateOf("") }
    var smbPassword by remember { mutableStateOf("") }
    var smbEntries by remember { mutableStateOf<List<SmbEntry>>(emptyList()) }
    var smbPath by remember { mutableStateOf("") }
    var smbError by remember { mutableStateOf<String?>(null) }
    var smbBusy by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var downloadName by remember { mutableStateOf("") }
    val savedServers by repo.smbServers.collectAsState(initial = emptyList())

    // --- DLNA ---
    var dlnaServers by remember { mutableStateOf<List<DlnaServer>>(emptyList()) }
    var dlnaServer by remember { mutableStateOf<DlnaServer?>(null) }
    var dlnaStack by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var dlnaItems by remember { mutableStateOf<List<DlnaItem>>(emptyList()) }
    var dlnaBusy by remember { mutableStateOf(false) }
    var dlnaStatus by remember { mutableStateOf<String?>(null) }

    fun playStreamUrl(streamUrl: String, title: String) {
        val trimmed = streamUrl.trim()
        if (!playerVm.isValidStreamUrl(trimmed)) {
            streamError = "Enter a valid http(s) URL."
            return
        }
        streamError = null
        scope.launch {
            repo.addStreamToHistory(trimmed, title.ifBlank { trimmed })
            // Keep the Home tab's recent-streams list in sync.
            libraryVm.addStream(trimmed)
        }
        if (playerVm.playStream(StreamItem(trimmed, title.ifBlank { "Stream" }, "Just now"))) {
            navController.navigate(Routes.PLAYER)
        } else {
            streamError = if (!playerVm.isPlayerReady) {
                "Player is still starting — try again in a moment."
            } else {
                "Couldn't start that stream."
            }
        }
    }

    fun friendlySmbError(e: Throwable): String = when (e) {
        is NoClassDefFoundError ->
            "SMB support needs the jcifs-ng library, which isn't added to the app yet."
        else -> {
            val message = e.message.orEmpty()
            when {
                message.contains("Access denied", ignoreCase = true) ||
                    message.contains("Logon failure", ignoreCase = true) ||
                    message.contains("STATUS_LOGON_FAILURE", ignoreCase = true) ->
                    "Access denied — check the username and password."
                message.contains("Unknown host", ignoreCase = true) ||
                    message.contains("Connection refused", ignoreCase = true) ||
                    message.contains("Timed out", ignoreCase = true) ||
                    message.contains("Network is unreachable", ignoreCase = true) ->
                    "Can't reach that address — check the IP/hostname and Wi-Fi."
                else -> "Couldn't connect: ${message.ifBlank { "unknown error" }.take(120)}"
            }
        }
    }

    fun connectSmb(targetHost: String, targetShare: String) {
        if (targetHost.isBlank()) {
            smbError = "Enter the PC / NAS address (e.g. 192.168.1.10)."
            return
        }
        smbBusy = true
        smbError = null
        scope.launch {
            val result = if (targetShare.isBlank()) {
                smbBrowser.listShares(targetHost, smbUser, smbPassword)
            } else {
                smbBrowser.listPath("smb://$targetHost/$targetShare/", smbUser, smbPassword)
            }
            result.onSuccess { entries ->
                smbEntries = entries
                smbPath = if (targetShare.isBlank()) {
                    "smb://$targetHost/"
                } else {
                    "smb://$targetHost/$targetShare/"
                }
                repo.saveSmbServer(SmbServer(targetHost, targetShare, smbUser))
            }.onFailure { e ->
                smbError = friendlySmbError(e)
            }
            smbBusy = false
        }
    }

    fun openSmbEntry(entry: SmbEntry) {
        if (entry.isDirectory) {
            smbBusy = true
            smbError = null
            scope.launch {
                smbBrowser.listPath(entry.smbUrl, smbUser, smbPassword)
                    .onSuccess { entries ->
                        smbEntries = entries
                        smbPath = entry.smbUrl
                    }
                    .onFailure { e -> smbError = friendlySmbError(e) }
                smbBusy = false
            }
            return
        }
        // A media file: download to cache, then play it as a local file
        // (ExoPlayer cannot read smb:// directly — see SmbDataSource).
        downloadName = entry.name
        downloadProgress = 0f
        smbError = null
        scope.launch {
            val result = smbBrowser.downloadToCache(
                context.applicationContext, entry, smbUser, smbPassword
            ) { progress -> downloadProgress = progress }
            downloadProgress = null
            result.onSuccess { file ->
                val uri = Uri.fromFile(file).toString()
                if (NetworkRepository.isVideoFile(entry.name)) {
                    val video = VideoItem(
                        id = "smb_${entry.smbUrl.hashCode()}",
                        title = entry.name,
                        genre = "Network",
                        durationMs = 0L,
                        resolution = "",
                        url = uri,
                        thumbKey = "smb",
                        sizeText = formatSize(entry.sizeBytes),
                        codec = "",
                        bitrateText = "",
                        fps = 0,
                        audioTracks = emptyList(),
                        subtitleTracks = emptyList(),
                        isLocal = true
                    )
                    playerVm.playVideo(video, listOf(video))
                    navController.navigate(Routes.PLAYER)
                } else {
                    val track = TrackItem(
                        id = "smb_${entry.smbUrl.hashCode()}",
                        title = entry.name,
                        artist = "Network",
                        album = "",
                        durationMs = 0L,
                        url = uri,
                        artKey = "smb",
                        isLocal = true
                    )
                    playerVm.playTrack(track, listOf(track))
                    navController.navigate(Routes.MUSIC)
                }
            }.onFailure { e ->
                smbError = "Download failed: ${friendlySmbError(e)}"
            }
        }
    }

    fun smbGoUp() {
        val path = smbPath.trimEnd('/')
        val parent = path.substringBeforeLast('/', "")
        scope.launch {
            smbBusy = true
            if (parent.isBlank() || parent == "smb:") {
                smbEntries = emptyList()
                smbPath = ""
            } else if (parent.count { it == '/' } <= 2) {
                // Back at "smb://host" — list its shares again.
                smbBrowser.listShares(host, smbUser, smbPassword)
                    .onSuccess { entries ->
                        smbEntries = entries
                        smbPath = "smb://$host/"
                    }
                    .onFailure { e -> smbError = friendlySmbError(e) }
            } else {
                smbBrowser.listPath("$parent/", smbUser, smbPassword)
                    .onSuccess { entries ->
                        smbEntries = entries
                        smbPath = "$parent/"
                    }
                    .onFailure { e -> smbError = friendlySmbError(e) }
            }
            smbBusy = false
        }
    }

    fun discoverDlna() {
        dlnaBusy = true
        dlnaStatus = "Searching your Wi-Fi network…"
        dlnaServers = emptyList()
        dlnaServer = null
        dlnaItems = emptyList()
        dlnaStack = emptyList()
        scope.launch {
            val found = DlnaDiscovery.discover(context.applicationContext)
            dlnaServers = found
            dlnaStatus = if (found.isEmpty()) {
                "No DLNA servers found. Make sure media sharing is on and you're on the same Wi-Fi."
            } else {
                null
            }
            dlnaBusy = false
        }
    }

    fun browseDlna(
        server: DlnaServer,
        objectId: String,
        title: String,
        newStack: List<Pair<String, String>>
    ) {
        dlnaBusy = true
        dlnaStatus = null
        scope.launch {
            DlnaBrowser.browse(server, objectId)
                .onSuccess { items ->
                    dlnaServer = server
                    dlnaItems = items
                    dlnaStack = newStack
                }
                .onFailure { e ->
                    dlnaStatus = "Couldn't browse that server: ${e.message ?: "unknown error"}"
                }
            dlnaBusy = false
        }
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = LocalTextPrimary.current
                    )
                }
                Text(
                    text = "Network",
                    color = LocalTextPrimary.current,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val streamSelected = mode == NetworkMode.STREAM
                val smbSelected = mode == NetworkMode.SMB
                val dlnaSelected = mode == NetworkMode.DLNA
                if (streamSelected) {
                    AccentGradientButton(
                        onClick = {},
                        text = "Stream",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    GlassButton(
                        onClick = { mode = NetworkMode.STREAM },
                        text = "Stream",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (smbSelected) {
                    AccentGradientButton(
                        onClick = {},
                        text = "LAN (SMB)",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    GlassButton(
                        onClick = { mode = NetworkMode.SMB },
                        text = "LAN (SMB)",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (dlnaSelected) {
                    AccentGradientButton(
                        onClick = {},
                        text = "DLNA",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    GlassButton(
                        onClick = { mode = NetworkMode.DLNA },
                        text = "DLNA",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (mode) {
                    NetworkMode.STREAM -> {
                        item {
                            Text(
                                text = "Stream URL",
                                color = LocalTextPrimary.current,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        item {
                            GlassTextField(
                                value = url,
                                onValueChange = { url = it; streamError = null },
                                placeholder = "Paste a stream URL (HLS, DASH, MP4…)",
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = LocalTextMuted.current
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            AccentGradientButton(
                                text = "Play Stream",
                                icon = Icons.Rounded.PlayArrow,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    val title = url.trim()
                                        .substringAfterLast("/")
                                        .substringBefore("?")
                                        .ifBlank { "Stream" }
                                    playStreamUrl(url, title)
                                }
                            )
                        }
                        streamError?.let { message ->
                            item {
                                Text(
                                    text = message,
                                    color = PinkAccent,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        item {
                            Text(
                                text = "Sample streams",
                                color = LocalTextPrimary.current,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(SampleData.recentStreams, key = { it.url }) { stream ->
                            StreamRow(stream = stream) {
                                playStreamUrl(stream.url, stream.title)
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "History",
                                    color = LocalTextPrimary.current,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(top = 8.dp)
                                )
                                if (history.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            scope.launch { repo.clearStreamHistory() }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Clear history",
                                            tint = LocalTextSecondary.current
                                        )
                                    }
                                }
                            }
                        }
                        if (history.isEmpty()) {
                            item {
                                Text(
                                    text = "No streams yet — played URLs show up here.",
                                    color = LocalTextSecondary.current,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        items(history, key = { it.url }) { stream ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { playStreamUrl(stream.url, stream.title) },
                                radius = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = LocalAccent.current.start,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stream.title,
                                            color = LocalTextPrimary.current,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = stream.url,
                                            color = LocalTextSecondary.current,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repo.removeStreamFromHistory(stream.url)
                                            }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Remove",
                                            tint = LocalTextSecondary.current,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    NetworkMode.SMB -> {
                        item {
                            Text(
                                text = "Windows / NAS shared folder",
                                color = LocalTextPrimary.current,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        item {
                            GlassTextField(
                                value = host,
                                onValueChange = { host = it; smbError = null },
                                placeholder = "PC / NAS address (192.168.1.10)",
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Dns,
                                        contentDescription = null,
                                        tint = LocalTextMuted.current
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            GlassTextField(
                                value = share,
                                onValueChange = { share = it; smbError = null },
                                placeholder = "Share name (blank = list all shares)",
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Storage,
                                        contentDescription = null,
                                        tint = LocalTextMuted.current
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            GlassTextField(
                                value = smbUser,
                                onValueChange = { smbUser = it; smbError = null },
                                placeholder = "Username (blank = guest)",
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = LocalTextMuted.current
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = smbPassword,
                                onValueChange = { smbPassword = it; smbError = null },
                                placeholder = {
                                    Text(
                                        text = "Password (never saved)",
                                        color = LocalTextMuted.current,
                                        fontSize = 15.sp
                                    )
                                },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                    focusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                                    focusedTextColor = LocalTextPrimary.current,
                                    unfocusedTextColor = LocalTextPrimary.current,
                                    cursorColor = LocalAccent.current.start
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            )
                        }
                        item {
                            AccentGradientButton(
                                text = if (smbBusy) "Connecting…" else "Connect",
                                icon = Icons.Rounded.PlayArrow,
                                enabled = !smbBusy,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { connectSmb(host.trim(), share.trim()) }
                            )
                        }
                        smbError?.let { message ->
                            item {
                                Text(
                                    text = message,
                                    color = PinkAccent,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        if (savedServers.isNotEmpty() && smbPath.isEmpty()) {
                            item {
                                Text(
                                    text = "Saved servers",
                                    color = LocalTextPrimary.current,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(savedServers, key = { it.host + "/" + it.share }) { server ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        host = server.host
                                        share = server.share
                                        smbUser = server.user
                                        connectSmb(server.host, server.share)
                                    },
                                    radius = 16.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Storage,
                                            contentDescription = null,
                                            tint = LocalAccent.current.start,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (server.share.isBlank()) {
                                                    server.host
                                                } else {
                                                    "${server.host} / ${server.share}"
                                                },
                                                color = LocalTextPrimary.current,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (server.user.isBlank()) {
                                                    "Guest"
                                                } else {
                                                    server.user
                                                },
                                                color = LocalTextSecondary.current,
                                                fontSize = 12.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    repo.removeSmbServer(server)
                                                }
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Forget server",
                                                tint = LocalTextSecondary.current,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (smbPath.isNotEmpty()) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { smbGoUp() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowBack,
                                            contentDescription = "Up one level",
                                            tint = LocalTextPrimary.current
                                        )
                                    }
                                    Text(
                                        text = smbPath,
                                        color = LocalTextSecondary.current,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (smbBusy) {
                                        CircularProgressIndicator(
                                            color = LocalTextPrimary.current,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            items(smbEntries, key = { it.smbUrl }) { entry ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { openSmbEntry(entry) },
                                    radius = 16.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                entry.isDirectory -> Icons.Rounded.Folder
                                                NetworkRepository.isVideoFile(entry.name) ->
                                                    Icons.Rounded.Movie
                                                else -> Icons.Rounded.MusicNote
                                            },
                                            contentDescription = null,
                                            tint = LocalAccent.current.start,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.name,
                                                color = LocalTextPrimary.current,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!entry.isDirectory) {
                                                Text(
                                                    text = formatSize(entry.sizeBytes),
                                                    color = LocalTextSecondary.current,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        if (entry.isDirectory) {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = LocalTextSecondary.current
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Download,
                                                contentDescription = "Download and play",
                                                tint = LocalTextSecondary.current,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        downloadProgress?.let { progress ->
                            item {
                                Column {
                                    Text(
                                        text = "Downloading $downloadName… ${(progress * 100).toInt()}%",
                                        color = LocalTextSecondary.current,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        item {
                            Text(
                                text = "Videos play after a quick download to the app cache " +
                                    "(SMB can't stream directly yet). Your password is " +
                                    "asked every time and never saved.",
                                color = LocalTextMuted.current,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    NetworkMode.DLNA -> {
                        item {
                            Text(
                                text = "DLNA media servers on your Wi-Fi",
                                color = LocalTextPrimary.current,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        item {
                            AccentGradientButton(
                                text = if (dlnaBusy) "Searching…" else "Search for servers",
                                icon = Icons.Rounded.Refresh,
                                enabled = !dlnaBusy,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { discoverDlna() }
                            )
                        }
                        dlnaStatus?.let { message ->
                            item {
                                Text(
                                    text = message,
                                    color = LocalTextSecondary.current,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        if (dlnaServer == null) {
                            items(dlnaServers, key = { it.location }) { server ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        browseDlna(
                                            server, "0", server.name,
                                            listOf("0" to server.name)
                                        )
                                    },
                                    radius = 16.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Tv,
                                            contentDescription = null,
                                            tint = LocalAccent.current.start,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = server.name,
                                                color = LocalTextPrimary.current,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = server.location,
                                                color = LocalTextSecondary.current,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Rounded.ChevronRight,
                                            contentDescription = null,
                                            tint = LocalTextSecondary.current
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (dlnaStack.size <= 1) {
                                                dlnaServer = null
                                                dlnaItems = emptyList()
                                                dlnaStack = emptyList()
                                            } else {
                                                val parent = dlnaStack[dlnaStack.size - 2]
                                                browseDlna(
                                                    dlnaServer!!, parent.first, parent.second,
                                                    dlnaStack.dropLast(1)
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowBack,
                                            contentDescription = "Back",
                                            tint = LocalTextPrimary.current
                                        )
                                    }
                                    Text(
                                        text = dlnaStack.lastOrNull()?.second
                                            ?: dlnaServer!!.name,
                                        color = LocalTextPrimary.current,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (dlnaBusy) {
                                        CircularProgressIndicator(
                                            color = LocalTextPrimary.current,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            if (dlnaItems.isEmpty() && !dlnaBusy) {
                                item {
                                    Text(
                                        text = "This folder is empty.",
                                        color = LocalTextSecondary.current,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            items(dlnaItems, key = { it.id }) { item ->
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        if (item.isContainer) {
                                            browseDlna(
                                                dlnaServer!!, item.id, item.title,
                                                dlnaStack + (item.id to item.title)
                                            )
                                        } else if (item.resUrl != null) {
                                            playStreamUrl(item.resUrl, item.title)
                                        }
                                    },
                                    radius = 16.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                item.isContainer -> Icons.Rounded.Folder
                                                (item.mimeType ?: "").startsWith("audio/") ->
                                                    Icons.Rounded.MusicNote
                                                else -> Icons.Rounded.Movie
                                            },
                                            contentDescription = null,
                                            tint = LocalAccent.current.start,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                color = LocalTextPrimary.current,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!item.isContainer && item.duration != null) {
                                                Text(
                                                    text = item.duration,
                                                    color = LocalTextSecondary.current,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        if (item.isContainer) {
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = LocalTextSecondary.current
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = "Play",
                                                tint = LocalTextSecondary.current
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Wifi,
                                    contentDescription = null,
                                    tint = LocalTextMuted.current,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Works with Windows media sharing, Plex, Jellyfin, " +
                                        "MiniDLNA and most NAS boxes — same Wi-Fi required.",
                                    color = LocalTextMuted.current,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun StreamRow(stream: StreamItem, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        radius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Public,
                contentDescription = null,
                tint = LocalAccent.current.start,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stream.title,
                    color = LocalTextPrimary.current,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stream.url,
                    color = LocalTextSecondary.current,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = stream.addedLabel,
                color = LocalTextMuted.current,
                fontSize = 12.sp
            )
        }
    }
}

/** Human-readable file size ("238 MB", "1.2 GB", "45 KB"). */
private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "Unknown size"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.toInt()} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
