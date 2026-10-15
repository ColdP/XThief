// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Scans the MediaStore video collection for files XThief wrote into its own
 * relative directories, so only app-downloaded media is listed.
 */
class MediaLibrary(
    context: Context,
    /** Used to label entries that came from sensitive posts. */
    private val history: HistoryStore? = null,
) {
    private val appContext = context.applicationContext
    private val _entries = MutableStateFlow<List<MediaEntry>>(emptyList())
    val entries: StateFlow<List<MediaEntry>> = _entries.asStateFlow()

    val current: List<MediaEntry> get() = _entries.value

    private var lastScanAt = 0L

    /**
     * Rescans the media store. Calls closer together than [SCAN_MIN_INTERVAL_MS] are skipped so
     * that automatic refreshes on page entry cannot stack up MediaStore queries.
     */
    suspend fun refresh(directories: List<String> = listOf(DEFAULT_SAVE_DIR)) {
        val now = System.currentTimeMillis()
        if (now - lastScanAt < SCAN_MIN_INTERVAL_MS) return
        lastScanAt = now
        val scanned = withContext(Dispatchers.IO) { query(scannedDirectories(directories)) }
        // Stamp the sensitive flag from the download history so the library can label or filter
        // these entries later; MediaStore itself carries no such metadata.
        val sensitiveNames = history?.current.orEmpty()
            .filter { it.sensitive }
            .mapTo(hashSetOf()) { it.fileName }
        _entries.value = scanned
            .map { entry ->
                if (!entry.sensitive && entry.fileName in sensitiveNames) {
                    entry.copy(sensitive = true)
                } else {
                    entry
                }
            }
            .sortedByDescending { it.addedAt }
    }

    private fun scannedDirectories(settingsDirectories: List<String>): List<String> {
        val defaults = settingsDirectories
            .map { it.trim('/').trimEnd('/') }
            .map { if (it.startsWith("Movies")) it else "Movies/$it" }
            .map { "$it/" }
            .toMutableList()
        val appDir = appContext.getExternalFilesDir(null)?.absolutePath.orEmpty()
        if (appDir.isNotBlank()) {
            defaults += appDir.removePrefix("/storage/emulated/0/").trimEnd('/') + "/"
        }
        return defaults.distinct()
    }

    private fun query(directories: List<String>): List<MediaEntry> {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.DATA,
        )
        val selection = directories.joinToString(" OR ") { "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?" }
        val args = directories.map { "%$it%" }.toTypedArray()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val results = mutableListOf<MediaEntry>()
        runCatching {
            appContext.contentResolver.query(
                collection,
                projection,
                selection,
                args,
                "${MediaStore.Video.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val pathColumn = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(collection, id)
                    results += MediaEntry(
                        fileName = cursor.getString(nameColumn).orEmpty(),
                        uri = uri.toString(),
                        displayName = cursor.getString(nameColumn).orEmpty(),
                        sizeBytes = cursor.getLong(sizeColumn),
                        addedAt = cursor.getLong(dateColumn) * 1000L,
                        directory = pathColumn.takeIf { it >= 0 }?.let(cursor::getString).orEmpty(),
                        durationMs = durationColumn.takeIf { it >= 0 }?.let(cursor::getLong) ?: 0L,
                        width = widthColumn.takeIf { it >= 0 }?.let(cursor::getInt) ?: 0,
                        height = heightColumn.takeIf { it >= 0 }?.let(cursor::getInt) ?: 0,
                    )
                }
            }
        }
        return results
    }

    fun delete(entry: MediaEntry): Boolean = runCatching {
        appContext.contentResolver.delete(Uri.parse(entry.uri), null, null) > 0
    }.getOrDefault(false)

    fun deleteFile(path: String): Boolean = runCatching { File(path).delete() }.getOrDefault(false)

    /** Deletes several entries at once and drops them from the cached list. */
    fun deleteAll(entries: List<MediaEntry>): Int {
        val removed = entries.count { delete(it) }
        if (removed > 0) {
            val ids = entries.mapTo(hashSetOf()) { it.uri }
            _entries.value = _entries.value.filterNot { it.uri in ids }
        }
        return removed
    }

    private companion object {
        /** Minimum gap between two MediaStore scans. */
        const val SCAN_MIN_INTERVAL_MS = 1_200L
    }
}
