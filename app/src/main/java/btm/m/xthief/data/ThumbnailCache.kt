// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Extracts and caches a cover frame for each downloaded video.
 *
 * The frame is grabbed with [MediaMetadataRetriever] slightly into the clip (to avoid the black
 * opening frame), scaled down and stored as a JPEG in the app cache, with a small in-memory LRU in
 * front of it. Covers are therefore generated on demand and reused afterwards.
 */
class ThumbnailCache(context: Context) {

    private val appContext = context.applicationContext
    private val directory = File(appContext.cacheDir, "covers").apply { mkdirs() }
    private val memory = LruCache<String, Bitmap>(MEMORY_ENTRIES)

    /** Cached cover for [entry], extracting one from the video the first time it is asked for. */
    suspend fun cover(entry: MediaEntry): Bitmap? = withContext(Dispatchers.IO) {
        val key = keyOf(entry)
        memory.get(key)?.let { return@withContext it }
        val file = File(directory, "$key.jpg")
        val bitmap = if (file.exists()) decode(file) else extract(entry, file)
        bitmap?.also { memory.put(key, it) }
    }

    /** Drops cached covers for entries that no longer exist. */
    suspend fun prune(entries: List<MediaEntry>) = withContext(Dispatchers.IO) {
        val alive = entries.mapTo(hashSetOf()) { keyOf(it) }
        directory.listFiles()?.forEach { file ->
            val name = file.nameWithoutExtension
            if (name !in alive) file.delete()
        }
    }

    private fun keyOf(entry: MediaEntry): String = entry.uri.hashCode().toString()

    private fun decode(file: File): Bitmap? = runCatching {
        BitmapFactory.decodeFile(file.absolutePath)
    }.getOrNull()

    private fun extract(entry: MediaEntry, file: File): Bitmap? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, Uri.parse(entry.uri))
            val frame = retriever.getFrameAtTime(
                FRAME_POSITION_US,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            ) ?: retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: return null
            val scaled = scaleDown(frame, MAX_WIDTH)
            file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            scaled
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull()

    private fun scaleDown(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, maxWidth, height, true)
    }

    /**
     * Average colour of a cover's lower half, used as the base of the metadata gradient.
     * Sampling the artwork keeps the scrim in tune with each thumbnail instead of forcing one
     * fixed tint onto every card.
     */
    fun scrimColorOf(bitmap: Bitmap): Int {
        val top = (bitmap.height * SCRIM_SAMPLE_TOP).toInt().coerceIn(0, bitmap.height - 1)
        val height = (bitmap.height - top).coerceAtLeast(1)
        val width = bitmap.width
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, top, width, height)

        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0
        var index = 0
        // Every 37th pixel is plenty for a stable average and far cheaper than all of them.
        while (index < pixels.size) {
            val pixel = pixels[index]
            red += (pixel shr 16) and 0xFF
            green += (pixel shr 8) and 0xFF
            blue += pixel and 0xFF
            count++
            index += 37
        }
        if (count == 0) return FALLBACK_SCRIM
        val r = (red / count).toInt().coerceIn(0, 255)
        val g = (green / count).toInt().coerceIn(0, 255)
        val b = (blue / count).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private companion object {
        const val MEMORY_ENTRIES = 24
        const val MAX_WIDTH = 720
        /** One second in, which skips the usual black lead-in frame. */
        const val FRAME_POSITION_US = 1_000_000L
        /** The scrim averages the bottom 45% of the cover. */
        const val SCRIM_SAMPLE_TOP = 0.55f
        const val FALLBACK_SCRIM = 0xFF1C1B1F.toInt()
    }
}
