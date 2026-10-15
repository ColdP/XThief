// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.data

import java.net.URLDecoder
import java.util.Locale

/** Sites XThief can resolve a video from. */
enum class VideoPlatform(val label: String, val host: String) {
    X("X", "x.com"),
    YOUTUBE("YouTube", "youtube.com"),
    BILIBILI("Bilibili", "bilibili.com"),
    UNKNOWN("未知", ""),
}

/** A resolved link: which site it belongs to and the id that site uses. */
data class VideoTarget(
    val platform: VideoPlatform,
    val id: String,
    /** The canonical watch URL for [id], used when re-fetching or sharing. */
    val canonicalUrl: String,
)

/**
 * Recognises share links for every supported platform.
 *
 * X is the app's original target and keeps its numeric status id; YouTube uses an 11-character
 * video id and Bilibili either a `BV…` id or an `av` number.
 */
object VideoLinks {

    private val xHost = Regex(
        """(?:^|//|\.)(?:mobile\.|www\.|m\.)?(?:x\.com|twitter\.com)(?::\d+)?/""",
        RegexOption.IGNORE_CASE,
    )
    private val youtubeHost = Regex(
        """(?:^|//|\.)(?:m\.|www\.|music\.)?(?:youtube\.com|youtu\.be|youtube-nocookie\.com)(?::\d+)?/""",
        RegexOption.IGNORE_CASE,
    )
    private val bilibiliHost = Regex(
        """(?:^|//|\.)(?:m\.|www\.|m\.)?(?:bilibili\.com|b23\.tv)(?::\d+)?/""",
        RegexOption.IGNORE_CASE,
    )

    private val xId = listOf(
        Regex("""/status(?:es)?/(\d{6,25})""", RegexOption.IGNORE_CASE),
        Regex("""[?&](?:id|tweet_id)=(\d{6,25})""", RegexOption.IGNORE_CASE),
    )
    private val bareXId = Regex("""^\d{6,25}$""")

    /** `watch?v=…`, `youtu.be/…`, `/shorts/…`, `/embed/…` and `/live/…`. */
    private val youtubeId = listOf(
        Regex("""[?&]v=([A-Za-z0-9_\-]{11})"""),
        Regex("""youtu\.be/([A-Za-z0-9_\-]{11})""", RegexOption.IGNORE_CASE),
        Regex("""/(?:shorts|embed|live|v)/([A-Za-z0-9_\-]{11})""", RegexOption.IGNORE_CASE),
    )

    /** `BV…` ids and legacy `av…` numbers. */
    private val bilibiliId = listOf(
        Regex("""(BV[0-9A-Za-z]{10})""", RegexOption.IGNORE_CASE),
        Regex("""/video/av(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""[?&]aid=(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""[?&]bvid=(BV[0-9A-Za-z]{10})""", RegexOption.IGNORE_CASE),
        // `av` with a word boundary in front, so a path segment like "have123" is not mistaken.
        Regex("""(?:^|[^0-9A-Za-z])av(\d+)""", RegexOption.IGNORE_CASE),
    )

    /** A bare `BV…` id pasted on its own. */
    private val bareBvId = Regex("""^BV[0-9A-Za-z]{10}$""", RegexOption.IGNORE_CASE)

    /** A bare `av…` id pasted on its own. */
    private val bareAvId = Regex("""^av(\d+)$""", RegexOption.IGNORE_CASE)

    /** Short links that have to be expanded over the network before an id can be read out. */
    private val shortLinkHost = Regex(
        """(?:^|//|\.)b23\.tv/""",
        RegexOption.IGNORE_CASE,
    )

    /** True when [raw] is a `b23.tv` short link, which only a redirect reveals the id of. */
    fun isShortLink(raw: String): Boolean = shortLinkHost.containsMatchIn(raw)

    /** `bv…` is accepted as input but the canonical form always starts with an uppercase `BV`. */
    private fun canonicalBv(raw: String): String = "BV" + raw.drop(2)

    fun platformOf(raw: String): VideoPlatform {
        val input = raw.trim()
        if (input.isEmpty()) return VideoPlatform.UNKNOWN
        // Order matters: a bare number is an X status id, so the Bilibili prefixes come first.
        if (bareBvId.matches(input)) return VideoPlatform.BILIBILI
        if (bareAvId.matches(input)) return VideoPlatform.BILIBILI
        if (bareXId.matches(input)) return VideoPlatform.X
        return when {
            xHost.containsMatchIn(input) -> VideoPlatform.X
            youtubeHost.containsMatchIn(input) -> VideoPlatform.YOUTUBE
            bilibiliHost.containsMatchIn(input) -> VideoPlatform.BILIBILI
            else -> VideoPlatform.UNKNOWN
        }
    }

    /** Parses a link (or bare id) into a target, or null when nothing recognisable is present. */
    fun parse(raw: String): VideoTarget? {
        val input = raw.trim()
        if (input.isEmpty()) return null

        // Bare ids first: a share sheet or a manual paste usually yields one of these, with no URL
        // for the host patterns to match.
        if (bareXId.matches(input)) {
            return VideoTarget(VideoPlatform.X, input, "https://x.com/i/status/$input")
        }
        bareBvId.matchEntire(input)?.let {
            val bv = canonicalBv(it.value)
            return VideoTarget(
                VideoPlatform.BILIBILI,
                bv,
                "https://www.bilibili.com/video/$bv",
            )
        }
        bareAvId.matchEntire(input)?.let {
            val aid = it.groupValues[1]
            return VideoTarget(
                VideoPlatform.BILIBILI,
                "av$aid",
                "https://www.bilibili.com/video/av$aid",
            )
        }

        val decoded = runCatching { URLDecoder.decode(input, "UTF-8") }.getOrDefault(input)
        return when (platformOf(decoded)) {
            VideoPlatform.X -> {
                xId.firstNotNullOfOrNull { it.find(decoded)?.groupValues?.getOrNull(1) }
                    ?.let { VideoTarget(VideoPlatform.X, it, "https://x.com/i/status/$it") }
            }

            VideoPlatform.YOUTUBE -> {
                youtubeId.firstNotNullOfOrNull { it.find(decoded)?.groupValues?.getOrNull(1) }
                    ?.let {
                        VideoTarget(
                            VideoPlatform.YOUTUBE,
                            it,
                            "https://www.youtube.com/watch?v=$it",
                        )
                    }
            }

            VideoPlatform.BILIBILI -> {
                val bv = bilibiliId[0].find(decoded)?.groupValues?.getOrNull(1)
                if (bv != null) {
                    VideoTarget(
                        VideoPlatform.BILIBILI,
                        bv,
                        "https://www.bilibili.com/video/$bv",
                    )
                } else {
                    // Legacy av number: keep the "av" prefix so downstream code can tell them apart.
                    bilibiliId.drop(1)
                        .firstNotNullOfOrNull { it.find(decoded)?.groupValues?.getOrNull(1) }
                        ?.let {
                            VideoTarget(
                                VideoPlatform.BILIBILI,
                                "av$it",
                                "https://www.bilibili.com/video/av$it",
                            )
                        }
                }
            }

            VideoPlatform.UNKNOWN -> null
        }
    }

    /** Finds the first supported link inside arbitrary shared text. */
    fun findInText(text: String): VideoTarget? {
        val candidate = Regex("""https?://\S+""", RegexOption.IGNORE_CASE).find(text)?.value
        return parse(candidate ?: text)
    }

    /**
     * True for links this app can handle, used to gate the share-sheet import.
     *
     * Short links count even though [parse] cannot read an id out of them: they are expanded over
     * the network during resolution.
     */
    fun isSupported(raw: String): Boolean = parse(raw) != null || isShortLink(raw)

    fun lower(value: String): String = value.lowercase(Locale.getDefault())
}
