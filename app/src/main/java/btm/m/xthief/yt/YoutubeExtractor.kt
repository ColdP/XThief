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
        val cookie = settingsStore.current.youtubeCookieHeader

        // Every client is queried and the results are merged rather than stopping at the first
        // success, because the clients do not offer the same thing: measured here, `ANDROID`
        // returns a 360p progressive MP4 while `VISIONOS` is the one that exposes direct URLs for
        // the 1080p/1440p/4K adaptive renditions. Taking only the first success capped every
        // download at 360p.
        val attempts = mutableListOf<String>()
        val merged = LinkedHashMap<String, VideoVariant>()
        var meta: TweetMedia? = null

        for (client in ALL_CLIENTS) {
            val result = runCatching { player(videoId, client, cookie) }
            result.onSuccess { media ->
                if (meta == null) meta = media
                media.videos.forEach { variant ->
                    // Key on resolution + bitrate: the same rendition from two clients is identical.
                    val key = "${variant.height}:${variant.bitrate}:${variant.audioUrl != null}"
                    val existing = merged[key]
                    // Prefer a copy that has a separate audio track, since adaptive video is silent.
                    if (existing == null || (existing.audioUrl == null && variant.audioUrl != null)) {
                        merged[key] = variant
                    }
                }
            }
            result.exceptionOrNull()?.let {
                attempts += "${client.name}: ${it.message ?: "未知错误"}"
            }
        }

        val base = meta
        if (base == null || merged.isEmpty()) {
            throw YoutubeException(
                "所有 YouTube 客户端都无法解析：\n" + attempts.joinToString("\n"),
            )
        }
        // Best quality first, which `sortedVariants` already does via sortKey.
        base.copy(videos = merged.values.toList())
    }

    /** Runs one player call and turns its response into [TweetMedia]. */
    private fun player(videoId: String, client: Client, cookie: String?): TweetMedia {
        val poToken = settingsStore.current.youtubePoToken.trim().takeIf { it.isNotEmpty() }
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
        // A user-supplied integrity token, when the environment demands one.
        poToken?.let {
            body.put("serviceIntegrityDimensions", JSONObject().put("poToken", it))
        }

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

        // Progressive MP4s: video and audio in one file, so nothing has to be muxed. These are
        // usually capped at 360p, but they are a useful fallback.
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

        // Adaptive renditions carry the real quality (up to 4K) but no audio, so each one is paired
        // with the best audio track. Collected unconditionally: with `formats` present these used to
        // be skipped, which capped every YouTube download at 360p.
        val adaptive = streaming.optJSONArray("adaptiveFormats")
        if (adaptive != null) {
            val bestAudio = (0 until adaptive.length())
                .mapNotNull { adaptive.optJSONObject(it) }
                .filter { it.optString("mimeType").startsWith("audio/") }
                .filter { it.optString("url").isNotBlank() }
                .maxByOrNull { it.optLong("bitrate", 0L) }
                ?.optString("url")
                ?.takeIf { it.isNotBlank() }

            for (index in 0 until adaptive.length()) {
                val format = adaptive.optJSONObject(index) ?: continue
                val mime = format.optString("mimeType")
                if (!mime.startsWith("video/")) continue
                // Only usable when the response carries a direct URL; some clients return formats
                // that would need `signatureCipher` descrambling, which needs a JS engine.
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

        private val TV_SAMSUNG = Client(
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
        )

        private val VISIONOS = Client(
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
        )

        private val WEB_EMBEDDED = Client(
            name = "WEB_EMBEDDED_PLAYER",
            version = "2.20260708.00.00",
            numericId = "56",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/15.5 Safari/605.1.15,gzip(gfe)",
            extraContext = emptyMap(),
        )

        private val WEB = Client(
            name = "WEB",
            version = "2.20260708.00.00",
            numericId = "1",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            extraContext = emptyMap(),
        )

        private val IOS = Client(
            name = "IOS",
            version = "21.26.4",
            numericId = "5",
            userAgent = "com.google.ios.youtube/21.26.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
            extraContext = mapOf(
                "deviceMake" to "Apple",
                "deviceModel" to "iPhone16,2",
                "osName" to "iPhone",
                "osVersion" to "18.3.2.22D82",
            ),
        )

        private val ANDROID = Client(
            name = "ANDROID",
            version = "21.26.364",
            numericId = "3",
            userAgent = "com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip",
            extraContext = mapOf(
                "androidSdkVersion" to "30",
                "osName" to "Android",
                "osVersion" to "11",
            ),
        )

        private val MWEB = Client(
            name = "MWEB",
            version = "2.20260708.05.00",
            numericId = "2",
            userAgent = "Mozilla/5.0 (iPad; CPU OS 16_7_10 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1,gzip(gfe)",
            extraContext = emptyMap(),
        )

        /**
         * Every client worth attempting, best first.
         *
         * Which clients Google accepts varies by IP, region and even per video — measured here,
         * `VISIONOS` answered OK for one video and `LOGIN_REQUIRED` for the next, while `ANDROID`
         * and `IOS` kept working. So the pool is tried in turn and the first that returns streams
         * wins; the TV and web clients are kept only as last resorts because they were refused
         * outright in testing.
         *
         * `ANDROID` leads because it was the most consistent and also returns a progressive MP4
         * (video and audio in one file), which avoids a mux step.
         */
        val ALL_CLIENTS = listOf(
            ANDROID,
            IOS,
            VISIONOS,
            TV_SAMSUNG,
            WEB_EMBEDDED,
            MWEB,
            WEB,
        )
    }
}
