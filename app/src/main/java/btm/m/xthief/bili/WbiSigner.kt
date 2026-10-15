// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.bili

import org.json.JSONObject
import java.security.MessageDigest
import java.net.URLEncoder

/**
 * Bilibili's WBI request signing.
 *
 * Since 2023 the web API rejects unsigned calls to several endpoints by quietly degrading the
 * response — `player/playurl` in particular returns only low qualities no matter what `qn` asks
 * for. A valid signature needs two keys from `web-interface/nav`, mixed through a fixed permutation
 * table, then an MD5 over the sorted query plus `wts`.
 *
 * The keys rotate daily, so [WbiKeys] is fetched once per extraction rather than cached forever.
 */
object WbiSigner {

    /** Subscript order Bilibili uses when mixing the two keys. */
    private val MIXIN_KEY_ENC_TAB = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52,
    )

    /** Characters Bilibili strips from parameter values before signing. */
    private const val FORBIDDEN = "!'()*"

    /** The two daily keys, derived from the `nav` response. */
    data class WbiKeys(val imgKey: String, val subKey: String) {
        val mixinKey: String = buildMixinKey(imgKey + subKey)
    }

    /** Pulls the keys out of a `web-interface/nav` payload; returns null when absent. */
    fun keysFromNav(nav: JSONObject): WbiKeys? {
        val wbi = nav.optJSONObject("data")?.optJSONObject("wbi_img") ?: return null
        val imgKey = keyFromUrl(wbi.optString("img_url")) ?: return null
        val subKey = keyFromUrl(wbi.optString("sub_url")) ?: return null
        if (imgKey.isBlank() || subKey.isBlank()) return null
        return WbiKeys(imgKey, subKey)
    }

    /** `https://i0.hdslb.com/bfs/wbi/7cd084941338484aae1ad9425b84077c.png` -> the file stem. */
    private fun keyFromUrl(url: String): String? =
        url.substringAfterLast('/', "")
            .substringBefore('.')
            .takeIf { it.isNotBlank() }

    private fun buildMixinKey(origin: String): String = buildString {
        MIXIN_KEY_ENC_TAB.forEach { index -> origin.getOrNull(index)?.let { append(it) } }
    }.take(32)

    /**
     * Returns [params] plus `wts` and `w_rid`, ready to be encoded into a query string.
     *
     * @param timestamp seconds since the epoch; injectable so the result can be tested.
     */
    fun sign(
        params: Map<String, String>,
        keys: WbiKeys,
        timestamp: Long = System.currentTimeMillis() / 1000L,
    ): Map<String, String> {
        val cleaned = params
            .mapValues { (_, value) -> value.filterNot { it in FORBIDDEN } }
            .toMutableMap()
        cleaned["wts"] = timestamp.toString()

        val sorted = cleaned.toSortedMap()
        // Values must be percent-encoded the same way on both sides, but spaces stay as spaces.
        val query = sorted.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val rid = md5(query + keys.mixinKey)
        return sorted + ("w_rid" to rid)
    }

    /**
     * Encodes exactly like Python's `urlencode` (i.e. `quote_plus`), which is what the reference
     * implementation hashes. The parameters involved here are all alphanumeric, but staying
     * byte-identical keeps the signature valid if more are added later.
     */
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun md5(input: String): String =
        MessageDigest.getInstance("MD5")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
