// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.bili

import btm.m.xthief.data.TweetMedia
import btm.m.xthief.data.VideoVariant
import btm.m.xthief.data.XThiefSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Raised when a Bilibili video cannot be resolved. */
class BilibiliException(message: String) : IOException(message)

/**
 * Resolves Bilibili videos through the public web API.
 *
 * Two calls are needed: `web-interface/view` gives the metadata and the `cid` of each part, and
 * `player/playurl` with `fnval=4048` returns DASH streams where video and audio are separate URLs.
 * Without a login cookie only low qualities come back, so `SESSDATA` from the settings is sent when
 * present.
 */
class BilibiliExtractor(private val settingsStore: XThiefSettingsStore) {

    suspend fun resolve(videoId: String): TweetMedia = withContext(Dispatchers.IO) {
        val bvid = normalizeId(videoId)
        val cookie = settingsStore.current.bilibiliCookie.trim().takeIf { it.isNotEmpty() }

        val view = request("$VIEW_URL?bvid=$bvid", cookie)
        if (view.optInt("code", -1) != 0) {
            throw BilibiliException(
                "Bilibili 返回错误（${view.optInt("code")}）：" +
                    view.optString("message").ifBlank { "无法获取视频信息" },
            )
        }
        val data = view.optJSONObject("data")
            ?: throw BilibiliException("响应缺少视频信息")

        // First part only; multi-part videos would need a part picker.
        val cid = data.optJSONArray("pages")?.optJSONObject(0)?.optLong("cid", 0L)
            ?.takeIf { it > 0L }
            ?: data.optLong("cid", 0L)
        if (cid <= 0L) throw BilibiliException("无法获取视频 cid")

        val play = request(
            "$PLAYURL_URL?bvid=$bvid&cid=$cid&qn=$MAX_QN&fnval=$FNVAL_DASH&fourk=1",
            cookie,
        )
        if (play.optInt("code", -1) != 0) {
            throw BilibiliException(
                "Bilibili 播放地址请求失败（${play.optInt("code")}）：" +
                    play.optString("message").ifBlank { "可能需要登录 Cookie" },
            )
        }
        val dash = play.optJSONObject("data")?.optJSONObject("dash")
            ?: throw BilibiliException("该视频没有可下载的 DASH 流（可能是番剧或需要大会员）")

        // One audio track is muxed into every video rendition; the highest bitrate wins.
        val audioUrl = dash.optJSONArray("audio")
            ?.let { array ->
                (0 until array.length())
                    .mapNotNull { array.optJSONObject(it) }
                    .maxByOrNull { it.optLong("bandwidth", 0L) }
                    ?.optString("baseUrl")
                    ?.takeIf { it.isNotBlank() }
            }

        val videos = dash.optJSONArray("video") ?: throw BilibiliException("该视频没有视频流")
        val variants = (0 until videos.length()).mapNotNull { index ->
            val track = videos.optJSONObject(index) ?: return@mapNotNull null
            val url = track.optString("baseUrl").takeIf { it.isNotBlank() }
                ?: track.optString("base_url").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            VideoVariant(
                url = url,
                bitrate = (track.optLong("bandwidth", 0L) / 1000L).toInt(),
                contentType = "video/mp4",
                width = track.optInt("width"),
                height = track.optInt("height"),
                audioUrl = audioUrl,
                // Bilibili's media hosts reject requests without a matching Referer.
                referer = REFERER,
            )
        }.distinctBy { it.url }
        if (variants.isEmpty()) throw BilibiliException("没有解析到可下载的视频流")

        TweetMedia(
            tweetId = bvid,
            author = data.optJSONObject("owner")?.optString("name").orEmpty(),
            authorHandle = data.optJSONObject("owner")?.optString("name")?.let { "@$it" }.orEmpty(),
            text = data.optString("title"),
            createdAt = data.optLong("pubdate", 0L).toString(),
            videos = variants,
        )
    }

    /** Accepts a `BV…` id or a legacy `av…` number and returns something the API takes. */
    private fun normalizeId(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("av", ignoreCase = true)) {
            throw BilibiliException("暂不支持 av 号，请使用 BV 号链接")
        }
        return trimmed
    }

    private fun request(url: String, cookie: String?): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Referer", REFERER)
            setRequestProperty("Accept", "application/json")
            cookie?.let { setRequestProperty("Cookie", it) }
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw BilibiliException("请求失败（HTTP $code）")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val VIEW_URL = "https://api.bilibili.com/x/web-interface/view"
        const val PLAYURL_URL = "https://api.bilibili.com/x/player/playurl"

        /** 127 = 8K; the API clamps this to whatever the account may actually access. */
        const val MAX_QN = 127

        /** DASH output (`fnval` bits 4 + 16 + 64 + 128 + 256 + 512 + 1024 + 2048). */
        const val FNVAL_DASH = 4048

        const val REFERER = "https://www.bilibili.com"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/133.0.0.0 Mobile Safari/537.36"
    }
}
