// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.x

import java.net.URLDecoder
import java.util.Locale

/** Extracts a tweet id from any common X / Twitter share link. */
object TweetLink {
    private val hostPattern = Regex(
        """(?:^|//|\.)(?:mobile\.|www\.|m\.)?(?:x\.com|twitter\.com)(?::\d+)?/""",
        RegexOption.IGNORE_CASE,
    )
    private val idPatterns = listOf(
        Regex("""/status(?:es)?/(\d{6,25})""", RegexOption.IGNORE_CASE),
        Regex("""[?&](?:id|tweet_id)=(\d{6,25})""", RegexOption.IGNORE_CASE),
    )
    private val bareId = Regex("""^\d{6,25}$""")

    fun isXLink(raw: String): Boolean = hostPattern.containsMatchIn(raw)

    fun extractId(raw: String): String? {
        val input = raw.trim()
        if (input.isEmpty()) return null
        if (bareId.matches(input)) return input
        if (!isXLink(input)) return null
        val decoded = runCatching { URLDecoder.decode(input, "UTF-8") }.getOrDefault(input)
        idPatterns.forEach { pattern ->
            pattern.find(decoded)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return null
    }

    /** Finds the first X link inside arbitrary shared text. */
    fun findInText(text: String): String? {
        val candidate = Regex("""https?://\S+""", RegexOption.IGNORE_CASE).find(text)?.value ?: text
        return extractId(candidate)
    }

    fun canonicalUrl(tweetId: String): String = "https://x.com/i/status/$tweetId"

    fun normalizeHandle(handle: String): String {
        val trimmed = handle.trim().removePrefix("@")
        return if (trimmed.isEmpty()) "" else "@$trimmed"
    }

    fun displayAuthor(handle: String, fallback: String): String {
        val clean = normalizeHandle(handle)
        if (clean.isNotEmpty()) return clean
        return fallback.ifBlank { "未知作者" }
    }

    fun lower(value: String): String = value.lowercase(Locale.getDefault())
}
