// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.yt

import btm.m.xthief.data.TweetMedia
import btm.m.xthief.data.VideoVariant
import btm.m.xthief.data.XThiefSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Raised when a YouTube video cannot be resolved. */
class YoutubeException(message: String) : IOException(message)

/**
 * Resolves YouTube videos through the InnerTube player endpoint.
 *
 * Google rotates which clients are usable and retires old client versions, so a single hard-coded
 * client always breaks eventually — that is exactly why yt-dlp keeps a list of them and their
 * versions, updating them as they die. The same approach is used here: every client in [CLIENTS] is
 * tried in turn and the first one that returns streams wins.
 *
 * Clients that need `signatureCipher` descrambling (the plain `WEB` one) only work when the
 * response already carries a usable `url`, which is why the app-facing clients come first.
 */
class YoutubeExtractor(private val settingsStore: XThiefSettingsStore) {

    /** One InnerTube client identity: name, version, numeric id, User-Agent and extra context. */
    private data class Client(
        val name: String,
        val version: String,
        val numericId: String,
        val userAgent: String,
        val extraContext: Map<String, String> = emptyMap(),
    )

    suspend fun resolve(videoId: String): TweetMedia = withContext(Dispatchers.IO) {
        val cookie = settingsStore.current.youtubeCookie.trim().takeIf { it.isNotEmpty() }

        // Collect the failures so the message can say what each client complained about.
        val attempts = mutableListOf<String>()
        for (client in CLIENTS) {
            val result = runCatching { player(videoId, client, cookie) }
            result.onSuccess { return@withContext it }
            val reason = result.exceptionOrNull()?.message ?: "未知错误"
            attempts += "${client.name}: $reason"
        }
        throw YoutubeException(
            "所有 YouTube 客户端都无法解析：\n" + attempts.joinToString("\n"),
        )
    }

    /** Runs one player call and turns its response into [TweetMedia]. */
    private fun player(videoId: String, client: Client, cookie: String?): TweetMedia {
        val connection =
            (URL("$PLAYER_URL?key=$API_KEY&prettyPrint=false").openConnection()
                as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 25_000
                doOutput = true
                instanceFollowRedirects = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", client.userAgent)
                // The client identity headers are mandatory; without them InnerTube answers with a
                // generic error instead of a player response.
                setRequestProperty("X-Goog-Api-Key", API_KEY)
                setRequestProperty("X-YouTube-Client-Name", client.numericId)
                setRequestProperty("X-YouTube-Client-Version", client.version)
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                cookie?.let { setRequestProperty("Cookie", it) }
            }

        val clientContext = JSONObject()
            .put("clientName", client.name)
            .put("clientVersion", client.version)
            .put("hl", "en")
            .put("gl", "US")
        client.extraContext.forEach { (key, value) -> clientContext.put(key, value) }

        val body = JSONObject()
            .put("context", JSONObject().put("client", clientContext))
            .put("videoId", videoId)
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)

        val response = try {
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            if (code >= 500) throw YoutubeException("HTTP $code")
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } finally {
            connection.disconnect()
        }
        if (response.isBlank()) throw YoutubeException("空响应")

        val root = runCatching { JSONObject(response) }
            .getOrElse { throw YoutubeException("响应不是合法 JSON") }

        root.optJSONObject("playabilityStatus")?.let { status ->
            val state = status.optString("status")
            if (state.isNotEmpty() && state != "OK") {
                val reason = status.optString("reason").ifBlank { state }
                throw YoutubeException("$state${if (reason == state) "" else "：$reason"}")
            }
        }

        val details = root.optJSONObject("videoDetails")
            ?: throw YoutubeException("响应缺少视频信息")
        val streaming = root.optJSONObject("streamingData")
            ?: throw YoutubeException("该视频没有可用的视频流（可能是直播或受保护内容）")

        val variants = mutableListOf<VideoVariant>()

        // Progressive MP4s: video and audio in one file, so nothing has to be muxed.
        streaming.optJSONArray("formats")?.let { formats ->
            for (index in 0 until formats.length()) {
                val format = formats.optJSONObject(index) ?: continue
                val url = format.optString("url").takeIf { it.isNotBlank() } ?: continue
                val mime = format.optString("mimeType")
                if (!mime.startsWith("video/")) continue
                variants += VideoVariant(
                    url = url,
                    bitrate = (format.optLong("bitrate", 0L) / 1000L).toInt(),
                    contentType = mime.substringBefore(';'),
                    width = format.optInt("width"),
                    height = format.optInt("height"),
                )
            }
        }

        // Adaptive fallback: pick the best video track and pair it with the best audio track.
        if (variants.isEmpty()) {
            val adaptive = streaming.optJSONArray("adaptiveFormats")
            val bestAudio = adaptive?.let { array ->
                (0 until array.length())
                    .mapNotNull { array.optJSONObject(it) }
                    .filter { it.optString("mimeType").startsWith("audio/") }
                    .maxByOrNull { it.optLong("bitrate", 0L) }
                    ?.optString("url")
                    ?.takeIf { it.isNotBlank() }
            }
            adaptive?.let { array ->
                for (index in 0 until array.length()) {
                    val format = array.optJSONObject(index) ?: continue
                    val mime = format.optString("mimeType")
                    if (!mime.startsWith("video/")) continue
                    val url = format.optString("url").takeIf { it.isNotBlank() } ?: continue
                    variants += VideoVariant(
                        url = url,
                        bitrate = (format.optLong("bitrate", 0L) / 1000L).toInt(),
                        contentType = mime.substringBefore(';'),
                        width = format.optInt("width"),
                        height = format.optInt("height"),
                        audioUrl = bestAudio,
                    )
                }
            }
        }

        val distinct = variants.distinctBy { it.url }
        if (distinct.isEmpty()) {
            throw YoutubeException("该视频没有可直接下载的流（可能需要在设置里填入 Cookie）")
        }

        return TweetMedia(
            tweetId = videoId,
            author = details.optString("author"),
            authorHandle = details.optString("author")?.let { "@$it" }.orEmpty(),
            text = details.optString("title"),
            createdAt = "",
            videos = distinct,
        )
    }

    private companion object {
        const val PLAYER_URL = "https://www.youtube.com/youtubei/v1/player"

        /** Public InnerTube key that ships inside YouTube's own web player; not a user secret. */
        const val API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

        /**
         * Tried in order, mirroring yt-dlp's `_DEFAULT_CLIENTS`.
         *
         * `tv_samsung` and `visionos` carry no `GvsPoTokenPolicy` in yt-dlp, i.e. they do **not**
         * demand a PO Token — the `web` client does, and obtaining one requires running YouTube's
         * BotGuard JavaScript, which is not something this app can do. `tv_samsung` is first
         * because it is yt-dlp's own primary client.
         *
         * `web` is kept last purely as a long shot: it only works when the response happens to
         * carry ready-made `url` fields instead of `signatureCipher`.
         */
        val CLIENTS = listOf(
            Client(
                name = "TVHTML5",
                version = "5.20260707",
                numericId = "7",
                userAgent = "Mozilla/5.0 (SMART-TV; Linux; Tizen 2.4.0) AppleWebKit/538.1 " +
                    "(KHTML, like Gecko) Version/2.4.0 TV Safari/538.1",
                extraContext = mapOf(
                    "deviceMake" to "Samsung",
                    "deviceModel" to "UKS9800",
                    "osName" to "Tizen",
                    "osVersion" to "2.4.0",
                ),
            ),
            Client(
                name = "VISIONOS",
                version = "1.02",
                numericId = "101",
                userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 " +
                    "(KHTML, like Gecko) Version/26.0 Safari/605.1.15",
                extraContext = mapOf(
                    "deviceMake" to "Apple",
                    "deviceModel" to "RealityDevice17,1",
                    "osName" to "visionOS",
                    "osVersion" to "26.5.23O471",
                ),
            ),
            Client(
                name = "WEB",
                version = "2.20260708.00.00",
                numericId = "1",
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
                extraContext = emptyMap(),
            ),
        )
    }
}
