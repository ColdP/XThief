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
class MediaLibrary(context: Context) {
    private val appContext = context.applicationContext
    private val _entries = MutableStateFlow<List<MediaEntry>>(emptyList())
    val entries: StateFlow<List<MediaEntry>> = _entries.asStateFlow()

    val current: List<MediaEntry> get() = _entries.value

    suspend fun refresh(directories: List<String> = listOf("Movies/XThief")) {
        val scanned = withContext(Dispatchers.IO) { query(scannedDirectories(directories)) }
        _entries.value = scanned.sortedByDescending { it.addedAt }
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
}
