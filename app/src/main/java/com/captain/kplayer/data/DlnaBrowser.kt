package com.captain.kplayer.data

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

/** A DLNA/UPnP media server found on the LAN. */
data class DlnaServer(
    val name: String,
    val location: String,
    val controlUrl: String
)

/** One container (folder) or playable item inside a DLNA server. */
data class DlnaItem(
    val id: String,
    val title: String,
    val isContainer: Boolean,
    val resUrl: String?,
    val mimeType: String?,
    val duration: String?
)

/**
 * Minimal DLNA/UPnP browsing with no new dependency: SSDP M-SEARCH over
 * UDP multicast for discovery, then ContentDirectory Browse over SOAP.
 * This is the same protocol every DLNA server (Windows Media Sharing,
 * Plex, Jellyfin, MiniDLNA, a NAS) speaks — no Cling-style stack needed.
 *
 * Needs (see report — manifest, not edited here):
 * CHANGE_WIFI_MULTICAST_STATE (multicast lock) and cleartext HTTP
 * (usesCleartextTraffic) for the http:// LAN URLs DLNA servers return.
 */
object DlnaDiscovery {

    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val ST_CONTENT_DIRECTORY =
        "urn:schemas-upnp-org:service:ContentDirectory:1"

    /** Broadcasts M-SEARCH and returns every responding media server. */
    suspend fun discover(context: Context, timeoutMs: Int = 4000): List<DlnaServer> =
        withContext(Dispatchers.IO) {
            val wifi = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val lock = wifi?.createMulticastLock("kplayer-dlna")?.apply {
                setReferenceCounted(true)
                acquire()
            }
            try {
                val socket = DatagramSocket().apply { soTimeout = 1000 }
                try {
                    val request = (
                        "M-SEARCH * HTTP/1.1\r\n" +
                            "HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n" +
                            "MAN: \"ns=01\"\r\n" +
                            "MX: 3\r\n" +
                            "ST: $ST_CONTENT_DIRECTORY\r\n" +
                            "USER-AGENT: K-Player/1.4 UPnP/1.0\r\n\r\n"
                        ).toByteArray(Charsets.UTF_8)
                    val packet = DatagramPacket(
                        request, request.size,
                        InetAddress.getByName(SSDP_ADDRESS), SSDP_PORT
                    )
                    socket.send(packet)
                    val locations = linkedSetOf<String>()
                    val buffer = ByteArray(8192)
                    val deadline = System.currentTimeMillis() + timeoutMs
                    while (System.currentTimeMillis() < deadline) {
                        val response = DatagramPacket(buffer, buffer.size)
                        try {
                            socket.receive(response)
                        } catch (_: java.net.SocketTimeoutException) {
                            continue
                        }
                        val text = String(
                            response.data, response.offset, response.length, Charsets.UTF_8
                        )
                        parseHeader(text, "LOCATION")?.let { locations.add(it.trim()) }
                    }
                    locations.mapNotNull { location ->
                        runCatching { describeDevice(location) }.getOrNull()
                    }
                } finally {
                    socket.close()
                }
            } finally {
                try {
                    lock?.release()
                } catch (_: Exception) {
                }
            }
        }

    private fun parseHeader(response: String, name: String): String? {
        return response.lineSequence()
            .firstOrNull { it.trim().startsWith("$name:", ignoreCase = true) }
            ?.substringAfter(":", "")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    /**
     * Fetches the device description XML at [location] and extracts the
     * friendly name plus the ContentDirectory control URL (resolved
     * against the description URL when it is relative).
     */
    private fun describeDevice(location: String): DlnaServer? {
        val xml = httpGet(location) ?: return null
        val parser = Xml.newPullParser().apply {
            setInput(StringReader(xml))
        }
        var name: String? = null
        var controlUrl: String? = null
        var inService = false
        var serviceType: String? = null
        var inFriendlyName = false
        var inServiceType = false
        var inControlUrl = false
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "service" -> {
                        inService = true
                        serviceType = null
                    }
                    "friendlyName" -> inFriendlyName = true
                    "serviceType" -> inServiceType = true
                    "controlURL" -> inControlUrl = true
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (inFriendlyName && text.isNotEmpty()) name = text
                    // The serviceType element comes before controlURL in
                    // the same <service> block, so by the time the
                    // controlURL text arrives we know whether this
                    // service is a ContentDirectory. (Matching on text
                    // alone would also catch SCPDURL, which precedes it.)
                    if (inService && inServiceType &&
                        text.startsWith("urn:schemas-upnp-org:service:ContentDirectory:")
                    ) {
                        serviceType = text
                    }
                    if (inService && inControlUrl && serviceType != null &&
                        text.isNotEmpty()
                    ) {
                        controlUrl = text
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "friendlyName" -> inFriendlyName = false
                    "serviceType" -> inServiceType = false
                    "controlURL" -> inControlUrl = false
                    "service" -> inService = false
                }
            }
            eventType = parser.next()
        }
        val control = controlUrl ?: return null
        val absolute = URL(URL(location), control).toString()
        return DlnaServer(
            name = name ?: location,
            location = location,
            controlUrl = absolute
        )
    }

    private fun httpGet(url: String): String? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Accept", "text/xml")
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }
}

/** ContentDirectory Browse (SOAP) against one [DlnaServer]. */
object DlnaBrowser {

    /**
     * Browses one container ([objectId], "0" = root) and returns its
     * direct children: sub-containers first, then playable items.
     * Only items with a res URL are returned; video items are listed
     * before audio, everything else is skipped.
     */
    suspend fun browse(server: DlnaServer, objectId: String = "0"): Result<List<DlnaItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val envelope = (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                        "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                        "<s:Body>" +
                        "<u:Browse xmlns:u=\"urn:schemas-upnp-org:service:ContentDirectory:1\">" +
                        "<ObjectID>$objectId</ObjectID>" +
                        "<BrowseFlag>BrowseDirectChildren</BrowseFlag>" +
                        "<Filter>*</Filter>" +
                        "<StartingIndex>0</StartingIndex>" +
                        "<RequestedCount>100</RequestedCount>" +
                        "<SortCriteria></SortCriteria>" +
                        "</u:Browse></s:Body></s:Envelope>"
                    )
                val connection = URL(server.controlUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 15000
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                connection.setRequestProperty(
                    "SOAPACTION",
                    "\"urn:schemas-upnp-org:service:ContentDirectory:1#Browse\""
                )
                try {
                    connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                        it.write(envelope)
                    }
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw IllegalStateException("DLNA server answered ${connection.responseCode}")
                    }
                    val soap = connection.inputStream.bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    parseDidl(extractResult(soap))
                } finally {
                    connection.disconnect()
                }
            }
        }

    /** The Browse response wraps the DIDL-Lite XML (escaped) in a <Result> tag. */
    private fun extractResult(soap: String): String {
        val start = soap.indexOf("<Result>")
        val end = soap.indexOf("</Result>")
        if (start < 0 || end < 0 || end <= start) return ""
        return soap.substring(start + "<Result>".length, end)
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }

    private fun parseDidl(didlXml: String): List<DlnaItem> {
        if (didlXml.isBlank()) return emptyList()
        val items = mutableListOf<DlnaItem>()
        val parser = Xml.newPullParser().apply {
            setInput(StringReader(didlXml))
        }
        var currentId: String? = null
        var currentTitle: String? = null
        var currentIsContainer: Boolean? = null
        var currentResUrl: String? = null
        var currentMime: String? = null
        var currentDuration: String? = null
        var inTitle = false
        var inRes = false
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "container" -> {
                        currentId = parser.getAttributeValue(null, "id")
                        currentIsContainer = true
                        currentTitle = null
                    }
                    "item" -> {
                        currentId = parser.getAttributeValue(null, "id")
                        currentIsContainer = false
                        currentTitle = null
                        currentResUrl = null
                        currentMime = null
                        currentDuration = null
                    }
                    "title" -> inTitle = true
                    "res" -> {
                        inRes = true
                        currentDuration = parser.getAttributeValue(null, "duration")
                        parser.getAttributeValue(null, "protocolInfo")
                            ?.split(":")
                            ?.getOrNull(2)
                            ?.let { currentMime = it }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isEmpty()) {
                        eventType = parser.next()
                        continue
                    }
                    if (inTitle) currentTitle = text
                    if (inRes) currentResUrl = text
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "title" -> inTitle = false
                    "res" -> inRes = false
                    "container", "item" -> {
                        val id = currentId
                        val title = currentTitle
                        val isContainer = currentIsContainer
                        if (id != null && title != null && isContainer != null) {
                            if (isContainer) {
                                items.add(
                                    DlnaItem(id, title, true, null, null, null)
                                )
                            } else if (!currentResUrl.isNullOrBlank()) {
                                // Only video/audio items are playable.
                                val mime = currentMime.orEmpty()
                                if (mime.startsWith("video/") || mime.startsWith("audio/") ||
                                    NetworkRepository.isMediaFile(currentResUrl!!)
                                ) {
                                    items.add(
                                        DlnaItem(
                                            id, title, false,
                                            currentResUrl, currentMime, currentDuration
                                        )
                                    )
                                }
                            }
                        }
                        currentId = null
                        currentTitle = null
                        currentIsContainer = null
                    }
                }
            }
            eventType = parser.next()
        }
        // Containers first, then items, each alphabetical.
        return items.sortedWith(
            compareBy({ !it.isContainer }, { it.title.lowercase() })
        )
    }
}
