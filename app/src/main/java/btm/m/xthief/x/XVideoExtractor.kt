// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.x

import btm.m.xthief.data.TweetMedia
import btm.m.xthief.data.VideoVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.zip.GZIPInputStream

class XExtractionException(message: String) : IOException(message)

/**
 * Resolves X/Twitter video renditions.
 *
 * The primary route is X's public syndication endpoint, which returns every MP4
 * rendition (bitrate + resolution) without authentication and without depending on
 * the frequently rotated GraphQL query ids. A guest-token GraphQL call and a plain
 * page scrape are kept as fallbacks.
 */
class XVideoExtractor {

    suspend fun resolve(tweetId: String): TweetMedia = withContext(Dispatchers.IO) {
        val syndication = runCatching { resolveThroughSyndication(tweetId) }
        syndication.getOrNull()?.let { return@withContext it }

        val graphQl = runCatching { resolveThroughApi(tweetId) }
        graphQl.getOrNull()?.let { return@withContext it }

        val scrape = runCatching { resolveThroughOpenGraph(tweetId) }
        scrape.getOrNull()?.let { return@withContext it }

        val errors = listOfNotNull(
            syndication.exceptionOrNull(),
            graphQl.exceptionOrNull(),
            scrape.exceptionOrNull(),
        )
        val message = errors.firstNotNullOfOrNull { it.message?.takeIf(String::isNotBlank) }
            ?: "无法解析该推文"
        throw XExtractionException(message)
    }

    // --------------------------------------------------------- syndication route

    private fun resolveThroughSyndication(tweetId: String): TweetMedia? {
        val url = "$SYNDICATION_URL?id=$tweetId&token=$SYNDICATION_TOKEN"
        val payload = runCatching { httpGet(url, bearer = null, guestToken = null, accept = JSON_ACCEPT) }
            .getOrElse { throw XExtractionException("推文不存在、受保护或已删除") }
        val root = runCatching { JSONObject(payload) }
            .getOrElse { throw XExtractionException("接口返回了无法解析的数据") }
        if (root.has("errors") && !root.has("mediaDetails") && !root.has("id_str")) {
            throw XExtractionException("推文不存在、受保护或已删除")
        }

        val user = root.optJSONObject("user")
        val handle = user?.optString("screen_name").orEmpty()
        val displayName = user?.optString("name").orEmpty()
        val text = root.optString("text").replace('\n', ' ').trim()
        val createdAt = root.optString("created_at")

        val media = root.optJSONArray("mediaDetails")
            ?: root.optJSONArray("media_details")
        val variants = media?.let(::parseSyndicationMedia).orEmpty().toMutableList()
        // Long and sensitive posts often ship HLS only (no progressive MP4), so expand the
        // master playlist into per-resolution renditions before giving up.
        if (variants.isEmpty()) {
            media?.let(::findHlsMasterUrl)?.let { master ->
                variants += runCatching { resolveHlsVariants(master) }.getOrDefault(emptyList())
            }
        }
        if (variants.isEmpty()) {
            // Sensitive posts are frequently withheld from logged-out callers entirely, which
            // otherwise looks identical to "this post has no video".
            val sensitive = root.optBoolean("possibly_sensitive", false)
            throw XExtractionException(
                if (sensitive) {
                    "该推文被标记为敏感内容，X 未向未登录请求返回媒体"
                } else {
                    "该推文不包含可下载的视频"
                },
            )
        }

        return TweetMedia(
            tweetId = root.optString("id_str").ifBlank { tweetId },
            author = displayName,
            authorHandle = TweetLink.normalizeHandle(handle),
            text = text,
            createdAt = createdAt,
            videos = variants,
        )
    }

    private fun parseSyndicationMedia(media: JSONArray): List<VideoVariant> {
        val variants = mutableListOf<VideoVariant>()
        for (index in 0 until media.length()) {
            val item = media.optJSONObject(index) ?: continue
            val type = item.optString("type")
            if (type == "photo") continue
            val info = item.optJSONObject("video_info") ?: item.optJSONObject("videoInfo")
            val original = item.optJSONObject("original_info")
            val sourceWidth = original?.optInt("width") ?: 0
            val sourceHeight = original?.optInt("height") ?: 0
            val list = info?.optJSONArray("variants")
            if (list == null) {
                // Progressive fallback: some posts expose a single MP4 URL.
                val direct = item.optString("media_url_https")
                if (direct.contains(".mp4")) {
                    variants += VideoVariant(
                        url = direct,
                        bitrate = 0,
                        contentType = "video/mp4",
                        width = sourceWidth,
                        height = sourceHeight,
                    )
                }
                continue
            }
            for (variantIndex in 0 until list.length()) {
                val variant = list.optJSONObject(variantIndex) ?: continue
                val contentType = variant.optString("content_type")
                val source = variant.optString("url")
                if (source.isBlank()) continue
                if (contentType.contains("mpegURL", ignoreCase = true) || source.contains(".m3u8")) continue
                val resolution = resolutionOf(source).let { (width, height) ->
                    if (width > 0 && height > 0) width to height else sourceWidth to sourceHeight
                }
                variants += VideoVariant(
                    url = source,
                    bitrate = variant.optInt("bitrate", 0),
                    contentType = contentType.ifBlank { "video/mp4" },
                    width = resolution.first,
                    height = resolution.second,
                )
            }
        }
        return dedupe(variants)
    }

    // ------------------------------------------------------------ GraphQL route

    private fun resolveThroughApi(tweetId: String): TweetMedia? {
        val token = fetchGuestToken()
        val response = requestTweet(token, tweetId)
            ?: throw XExtractionException("推文不存在、已删除或仅限登录可见")
        val media = parseApiResponse(tweetId, response)
        if (media.videos.isEmpty()) {
            throw XExtractionException("该推文不包含可下载的视频")
        }
        return media
    }

    private fun fetchGuestToken(): String {
        val body = "{\"guest_token\":null}"
        val response = httpPostJson(ACTIVATE_URL, body, bearer = BEARER, guestToken = null)
        val token = runCatching { JSONObject(response).optString("guest_token") }.getOrNull()
        if (token.isNullOrBlank()) {
            throw XExtractionException("无法获取 X 访客令牌，请稍后重试或检查网络")
        }
        return token
    }

    private fun requestTweet(token: String, tweetId: String): String? {
        val variables = JSONObject()
            .put("tweetId", tweetId)
            .put("withCommunity", false)
            .put("includePromotedContent", false)
            .put("withVoice", true)
            .put("withBirdwatchNotes", true)
        val features = JSONObject(TWEET_FEATURES)
        val fieldToggles = JSONObject(FIELD_TOGGLES)

        val queryUrl = buildString {
            append(TWEET_RESULT_URL)
            append("?variables=").append(encode(variables.toString()))
            append("&features=").append(encode(features.toString()))
            append("&fieldToggles=").append(encode(fieldToggles.toString()))
        }
        val first = runCatching {
            httpGet(queryUrl, bearer = BEARER, guestToken = token, accept = JSON_ACCEPT)
        }
        first.getOrNull()?.let { body ->
            if (JSONObject(body).optJSONObject("data") != null) return body
        }
        return null
    }

    private fun parseApiResponse(tweetId: String, response: String): TweetMedia {
        val root = JSONObject(response)
        val tweet = root.optJSONObject("data")
            ?.optJSONObject("tweetResult")
            ?.optJSONObject("result")
            ?: throw XExtractionException("响应缺少推文数据")
        val actualId = tweet.optString("rest_id").ifBlank { tweetId }
        val legacy = tweet.optJSONObject("legacy") ?: JSONObject()

        val variants = mutableListOf<VideoVariant>()
        val media = legacy.optJSONObject("extended_entities")?.optJSONArray("media")
            ?: legacy.optJSONObject("entities")?.optJSONArray("media")
        if (media != null) {
            for (index in 0 until media.length()) {
                val item = media.optJSONObject(index) ?: continue
                val type = item.optString("type")
                if (type != "video" && type != "animated_gif") continue
                val list = item.optJSONObject("video_info")?.optJSONArray("variants") ?: continue
                for (variantIndex in 0 until list.length()) {
                    val variant = list.optJSONObject(variantIndex) ?: continue
                    val contentType = variant.optString("content_type")
                    val url = variant.optString("url")
                    if (url.isBlank()) continue
                    if (contentType.contains("m3u8") || url.contains(".m3u8")) continue
                    val resolution = resolutionOf(url)
                    variants += VideoVariant(
                        url = url,
                        bitrate = variant.optInt("bitrate", 0),
                        contentType = contentType.ifBlank { "video/mp4" },
                        width = resolution.first,
                        height = resolution.second,
                    )
                }
            }
        }

        val user = tweet.optJSONObject("core")
            ?.optJSONObject("user_results")
            ?.optJSONObject("result")
        val handle = user?.optJSONObject("core")?.optString("screen_name")
            ?: user?.optJSONObject("legacy")?.optString("screen_name")
            ?: ""
        val displayName = user?.optJSONObject("core")?.optString("name")
            ?: user?.optJSONObject("legacy")?.optString("name")
            ?: ""

        return TweetMedia(
            tweetId = actualId,
            author = displayName,
            authorHandle = TweetLink.normalizeHandle(handle.orEmpty()),
            text = legacy.optString("full_text").replace('\n', ' ').trim(),
            createdAt = legacy.optString("created_at"),
            videos = dedupe(variants),
        )
    }

    // ------------------------------------------------------------ OpenGraph route

    private fun resolveThroughOpenGraph(tweetId: String): TweetMedia {
        val url = "https://x.com/i/status/$tweetId"
        val html = httpGet(url, bearer = null, guestToken = null, accept = HTML_ACCEPT)
        val variants = mutableListOf<VideoVariant>()
        VIDEO_META_PATTERNS.forEach { pattern ->
            pattern.findAll(html).forEach { match ->
                val link = decodeHtml(match.groupValues[1])
                if (link.isBlank()) return@forEach
                if (link.contains("amplify_video_thumb")) return@forEach
                if (link.contains(".m3u8")) return@forEach
                val resolution = resolutionOf(link)
                variants += VideoVariant(
                    url = link,
                    bitrate = if (resolution.second > 0) resolution.second * 800 else 0,
                    contentType = "video/mp4",
                    width = resolution.first,
                    height = resolution.second,
                )
            }
        }
        if (variants.isEmpty()) {
            throw XExtractionException("未能在页面中找到视频直链，X 可能调整了页面结构")
        }
        val title = Regex("""<meta[^>]+property="og:title"[^>]+content="([^"]*)"""")
            .find(html)?.groupValues?.getOrNull(1)?.let(::decodeHtml).orEmpty()
        val handleMatch = Regex("""@([A-Za-z0-9_]{2,15})""").find(title)
        return TweetMedia(
            tweetId = tweetId,
            author = title.substringBefore("(@").trim(),
            authorHandle = TweetLink.normalizeHandle(handleMatch?.groupValues?.getOrNull(1).orEmpty()),
            text = title,
            createdAt = "",
            videos = dedupe(variants),
        )
    }

    // ------------------------------------------------------------------ helpers

    /** X encodes renditions as `/<width>x<height>/` inside the path. */
    private fun resolutionOf(url: String): Pair<Int, Int> {
        val match = Regex("""/(\d{2,5})x(\d{2,5})/""").find(url) ?: return 0 to 0
        val width = match.groupValues[1].toIntOrNull() ?: 0
        val height = match.groupValues[2].toIntOrNull() ?: 0
        return width to height
    }

    // ------------------------------------------------------------- HLS fallback

    /** Finds the HLS master playlist URL when a post exposes no progressive MP4 variant. */
    private fun findHlsMasterUrl(media: JSONArray): String? {
        for (index in 0 until media.length()) {
            val item = media.optJSONObject(index) ?: continue
            val info = item.optJSONObject("video_info")
                ?: item.optJSONObject("videoInfo")
                ?: continue
            val list = info.optJSONArray("variants") ?: continue
            for (variantIndex in 0 until list.length()) {
                val variant = list.optJSONObject(variantIndex) ?: continue
                val source = variant.optString("url")
                val type = variant.optString("content_type")
                if (type.contains("mpegURL", ignoreCase = true) ||
                    source.contains(".m3u8", ignoreCase = true)
                ) {
                    return source
                }
            }
        }
        return null
    }

    /**
     * Expands an HLS master playlist into one [VideoVariant] per advertised resolution.
     *
     * X serves fMP4: each media playlist starts with an `#EXT-X-MAP` init segment (carrying the
     * `moov` box) followed by `.m4s` parts, so concatenating them yields a playable MP4. Audio
     * lives on its own playlist, whose URL is carried along for the downloader to mux back in.
     */
    private fun resolveHlsVariants(masterUrl: String): List<VideoVariant> {
        val text = httpGet(masterUrl, bearer = null, guestToken = null, accept = "*/*")
        val origin = originOf(masterUrl)
        // Audio group id -> (bitrate, playlist url); the highest bitrate per group wins.
        val audioGroups = mutableMapOf<String, Pair<Int, String>>()
        val variants = mutableListOf<VideoVariant>()
        var resolution = 0 to 0
        var bandwidth = 0
        var audioGroup = ""

        text.lineSequence().map(String::trim).forEach { line ->
            when {
                line.startsWith("#EXT-X-MEDIA:") && line.contains("TYPE=AUDIO", ignoreCase = true) -> {
                    val group = attr(line, "GROUP-ID")
                    val uri = attr(line, "URI")
                    if (group.isNotBlank() && uri.isNotBlank()) {
                        val bitrate = group.substringAfterLast('-').toIntOrNull() ?: 0
                        val existing = audioGroups[group]
                        if (existing == null || existing.first < bitrate) {
                            audioGroups[group] = bitrate to absoluteUrl(origin, uri)
                        }
                    }
                }

                line.startsWith("#EXT-X-STREAM-INF:") -> {
                    resolution = parseResolution(attr(line, "RESOLUTION"))
                    bandwidth = attr(line, "BANDWIDTH").toIntOrNull() ?: 0
                    audioGroup = attr(line, "AUDIO")
                }

                line.isBlank() || line.startsWith("#") -> Unit

                else -> {
                    variants += VideoVariant(
                        url = absoluteUrl(origin, line),
                        bitrate = bandwidth,
                        // The assembled result is an MP4 even though the source is a playlist.
                        contentType = "video/mp4",
                        width = resolution.first,
                        height = resolution.second,
                        hlsAudioUrl = audioGroups[audioGroup]?.second,
                    )
                    resolution = 0 to 0
                    bandwidth = 0
                    audioGroup = ""
                }
            }
        }
        return dedupe(variants)
    }

    /** Reads `NAME=value` or `NAME="value"` from an m3u8 tag line. */
    private fun attr(line: String, name: String): String {
        val marker = "$name="
        val start = line.indexOf(marker)
        if (start < 0) return ""
        val rest = line.substring(start + marker.length)
        return if (rest.startsWith("\"")) {
            rest.substringAfter('"').substringBefore('"')
        } else {
            rest.substringBefore(',')
        }
    }

    /** Parses a `RESOLUTION=1920x1080` attribute. */
    private fun parseResolution(value: String): Pair<Int, Int> {
        val parts = value.split('x')
        if (parts.size != 2) return 0 to 0
        return (parts[0].toIntOrNull() ?: 0) to (parts[1].toIntOrNull() ?: 0)
    }

    private fun originOf(url: String): String = runCatching {
        val uri = URI(url)
        "${uri.scheme}://${uri.host}"
    }.getOrDefault("https://video.twimg.com")

    /** Playlist entries are root-relative (`/amplify_video/...`). */
    private fun absoluteUrl(origin: String, value: String): String = when {
        value.startsWith("http", ignoreCase = true) -> value
        value.startsWith("/") -> origin + value
        else -> "$origin/$value"
    }

    private fun dedupe(variants: List<VideoVariant>): List<VideoVariant> {
        val seen = LinkedHashMap<String, VideoVariant>()
        variants.forEach { variant ->
            val key = variant.url.substringBefore('?')
            val existing = seen[key]
            if (existing == null || existing.bitrate < variant.bitrate) {
                seen[key] = variant
            }
        }
        return seen.values.sortedByDescending { it.sortKey }
    }

    private fun decodeHtml(value: String): String = value
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("\\/", "/")

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun httpGet(
        url: String,
        bearer: String?,
        guestToken: String?,
        accept: String,
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", accept)
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            setRequestProperty("Accept-Encoding", "gzip")
            bearer?.let { setRequestProperty("Authorization", "Bearer $it") }
            guestToken?.let { setRequestProperty("x-guest-token", it) }
        }
        return connection.useConnection()
    }

    private fun httpPostJson(
        url: String,
        body: String,
        bearer: String?,
        guestToken: String?,
        contentType: String = "application/json",
        extraQuery: String = "",
    ): String {
        val connection = (URL(url + extraQuery).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", JSON_ACCEPT)
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("Accept-Encoding", "gzip")
            bearer?.let { setRequestProperty("Authorization", "Bearer $it") }
            guestToken?.let { setRequestProperty("x-guest-token", it) }
        }
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        return connection.useConnection()
    }

    private fun HttpURLConnection.useConnection(): String {
        return try {
            val code = responseCode
            val stream = if (code in 200..299) inputStream else errorStream
            val raw = stream?.let { input ->
                val decoded = if (contentEncoding?.contains("gzip", ignoreCase = true) == true) {
                    GZIPInputStream(input)
                } else {
                    input
                }
                decoded.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(raw).optJSONArray("errors") }
                    .getOrNull()
                    ?.optJSONObject(0)
                    ?.optString("message")
                    .orEmpty()
                throw XExtractionException(
                    message.ifBlank { "请求失败（HTTP $code）" },
                )
            }
            raw
        } finally {
            disconnect()
        }
    }

    private companion object {
        const val BEARER =
            "AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D" +
                "1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA"
        const val ACTIVATE_URL = "https://api.x.com/1.1/guest/activate.json"
        const val TWEET_RESULT_URL =
            "https://api.x.com/graphql/5GOHgZe-8nzEMqKZS4DaEg/TweetResultByRestId"
        const val SYNDICATION_URL = "https://cdn.syndication.twimg.com/tweet-result"
        const val SYNDICATION_TOKEN = "a"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15; 23127PN0CC Build/AP3A.240905.015) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"
        const val JSON_ACCEPT = "application/json"
        const val HTML_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"

        val VIDEO_META_PATTERNS = listOf(
            Regex("""<meta[^>]+property="og:video(?::secure_url|:url)?"[^>]+content="([^"]+)"""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+content="([^"]+)"[^>]+property="og:video(?::secure_url|:url)?"""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]+name="twitter:player:stream"[^>]+content="([^"]+)"""", RegexOption.IGNORE_CASE),
            Regex(""""(https://video\.twimg\.com/[^"]+\.mp4[^"]*)"""", RegexOption.IGNORE_CASE),
        )
    }
}

private const val TWEET_FEATURES = """
{
  "creator_subscriptions_tweet_preview_api_enabled": true,
  "premium_content_api_read_enabled": false,
  "communities_web_enable_tweet_community_results_fetch": true,
  "c9s_tweet_anatomy_moderator_badge_enabled": true,
  "responsive_web_grok_analyze_button_fetch_trends_enabled": false,
  "responsive_web_grok_analyze_post_followups_enabled": false,
  "responsive_web_jetfuel_frame": true,
  "responsive_web_grok_share_attachment_enabled": true,
  "articles_preview_enabled": true,
  "responsive_web_edit_tweet_api_enabled": true,
  "graphql_is_translatable_rweb_tweet_is_translatable_enabled": true,
  "view_counts_everywhere_api_enabled": true,
  "longform_notetweets_consumption_enabled": true,
  "responsive_web_twitter_article_tweet_consumption_enabled": true,
  "tweet_awards_web_tipping_enabled": false,
  "responsive_web_grok_show_grok_translated_post": false,
  "responsive_web_grok_analysis_button_from_backend": false,
  "creator_subscriptions_quote_tweet_preview_enabled": false,
  "freedom_of_speech_not_reach_fetch_enabled": true,
  "standardized_nudges_misinfo": true,
  "tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled": true,
  "longform_notetweets_rich_text_read_enabled": true,
  "longform_notetweets_inline_media_enabled": true,
  "responsive_web_grok_image_annotation_enabled": true,
  "responsive_web_grok_imagine_annotation_enabled": true,
  "responsive_web_grok_community_note_auto_translation_is_enabled": false,
  "responsive_web_enhance_cards_enabled": false
}
"""

private const val FIELD_TOGGLES = """
{
  "withArticleRichContentState": true,
  "withArticlePlainText": false,
  "withGrokAnalyze": false,
  "withDisallowedReplyControls": false
}
"""
