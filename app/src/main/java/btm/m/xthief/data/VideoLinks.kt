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
        Regex("""(BV[0-9A-Za-z]{10})"""),
        Regex("""/video/av(\d+)""", RegexOption.IGNORE_CASE),
        Regex("""[?&]aid=(\d+)""", RegexOption.IGNORE_CASE),
    )

    fun platformOf(raw: String): VideoPlatform {
        val input = raw.trim()
        if (input.isEmpty()) return VideoPlatform.UNKNOWN
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
        val decoded = runCatching { URLDecoder.decode(input, "UTF-8") }.getOrDefault(input)

        if (bareXId.matches(input)) {
            return VideoTarget(VideoPlatform.X, input, "https://x.com/i/status/$input")
        }
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

    /** True for links this app can handle, used to gate the share-sheet import. */
    fun isSupported(raw: String): Boolean = parse(raw) != null

    fun lower(value: String): String = value.lowercase(Locale.getDefault())
}
