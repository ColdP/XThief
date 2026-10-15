// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.data

/** A single downloadable video rendition exposed by X (Twitter). */
data class VideoVariant(
    val url: String,
    val bitrate: Int,
    val contentType: String,
    /** Width parsed from the X URL pattern (`/<width>x<height>/`), 0 when unknown. */
    val width: Int = 0,
    val height: Int = 0,
    /**
     * For HLS renditions whose audio lives in a separate playlist, the audio media-playlist URL.
     * Non-null marks this variant as HLS to be assembled from fMP4 segments.
     */
    val hlsAudioUrl: String? = null,
) {
    /** True when this rendition must be downloaded as an HLS playlist rather than a plain MP4. */
    val isHls: Boolean get() = url.contains(".m3u8", ignoreCase = true)

    val resolutionLabel: String
        get() = when {
            width > 0 && height > 0 -> "${minOf(width, height)}p"
            height > 0 -> "${height}p"
            else -> "未知"
        }

    val fileSizeLabel: String get() = if (bitrate > 0) "$bitrate bps" else "未知码率"

    /** Best-effort ordering key: HLS ranks below progressive MP4 at the same resolution. */
    val sortKey: Int get() = bitrate * 1000 + height
}

/** Resolved metadata for one X post. */
data class TweetMedia(
    val tweetId: String,
    val author: String,
    val authorHandle: String,
    val text: String,
    val createdAt: String,
    val videos: List<VideoVariant>,
    /**
     * X flagged the post as sensitive (adult / graphic). Carried through to the download record and
     * the media library so those surfaces can label or filter it later.
     */
    val sensitive: Boolean = false,
) {
    /** Highest quality first. */
    val sortedVariants: List<VideoVariant> get() = videos.sortedByDescending { it.sortKey }
}

/** One row of the download history. */
data class DownloadRecord(
    val id: String,
    val tweetId: String,
    val author: String,
    val authorHandle: String,
    val text: String,
    val variantLabel: String,
    val bitrate: Int,
    val fileName: String,
    val uri: String,
    val byteCount: Long,
    val createdAt: Long,
    val completed: Boolean,
    /** The source post was flagged sensitive; drives library labelling / filtering. */
    val sensitive: Boolean = false,
    /** Starred by the user from the built-in player. */
    val favorite: Boolean = false,
)

/** A downloaded file surfaced by the media library. */
data class MediaEntry(
    val fileName: String,
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val addedAt: Long,
    val directory: String,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val source: MediaSource = MediaSource.LIBRARY,
    /** True when the matching history record came from a sensitive post. */
    val sensitive: Boolean = false,
    /** True when the matching history record was starred in the player. */
    val favorite: Boolean = false,
)

enum class MediaSource { LIBRARY, HISTORY }
