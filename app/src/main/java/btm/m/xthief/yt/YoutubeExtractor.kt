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
 * The ANDROID client is used deliberately: it returns direct `googlevideo.com` URLs without the
 * `signatureCipher` that the web client needs, so no JavaScript interpretation is involved. Its
 * `formats` array carries progressive MP4s with audio already muxed; `adaptiveFormats` (video and
 * audio apart) is only a fallback.
 */
class YoutubeExtractor(private val settingsStore: XThiefSettingsStore) {

    suspend fun resolve(videoId: String): TweetMedia = withContext(Dispatchers.IO) {
        val cookie = settingsStore.current.youtubeCookie.trim().takeIf { it.isNotEmpty() }

        val connection = (URL("$PLAYER_URL?prettyPrint=false").openConnection() as HttpURLConnection)
            .apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 25_000
                doOutput = true
                instanceFollowRedirects = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                cookie?.let { setRequestProperty("Cookie", it) }
            }

        val body = JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", CLIENT_NAME)
                        .put("clientVersion", CLIENT_VERSION)
                        .put("androidSdkVersion", ANDROID_SDK)
                        .put("hl", "en")
                        .put("gl", "US"),
                ),
            )
            .put("videoId", videoId)
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)

        val response = try {
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            // A failed player call still returns a JSON error body on 4xx; 5xx has none.
            if (code >= 500) throw YoutubeException("YouTube 请求失败（HTTP $code）")
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } finally {
            connection.disconnect()
        }
        if (response.isBlank()) throw YoutubeException("YouTube 返回了空响应")

        val root = runCatching { JSONObject(response) }
            .getOrElse { throw YoutubeException("无法解析 YouTube 响应") }

        root.optJSONObject("playabilityStatus")?.let { status ->
            val state = status.optString("status")
            if (state.isNotEmpty() && state != "OK") {
                val reason = status.optString("reason").ifBlank { state }
                throw YoutubeException("YouTube 拒绝播放：$reason")
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

        TweetMedia(
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
        const val CLIENT_NAME = "ANDROID"
        const val CLIENT_VERSION = "19.35.36"
        const val ANDROID_SDK = 34
        const val USER_AGENT =
            "com.google.android.youtube/19.35.36 (Linux; U; Android 14) gzip"
    }
}
