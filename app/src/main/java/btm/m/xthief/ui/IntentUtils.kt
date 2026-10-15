// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import btm.m.xthief.data.DownloadRecord
import java.io.File

/** Opens a downloaded media file with the system player. */
fun openUri(context: Context, uri: String) {
    if (uri.isBlank()) {
        toast(context, "文件不可用")
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(uri), "video/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { toast(context, "没有可以播放该视频的应用") }
}

/** Shares a media URI through the system share sheet. */
fun shareUri(context: Context, uri: String, title: String) {
    if (uri.isBlank()) {
        toast(context, "文件不可用")
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
        putExtra(Intent.EXTRA_TITLE, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "分享视频")) }
        .onFailure { error ->
            if (error !is ActivityNotFoundException) toast(context, "分享失败")
        }
}

/** Shares the source post link of a history entry. */
fun shareRecord(context: Context, record: DownloadRecord) {
    if (record.uri.isNotBlank() && Uri.parse(record.uri).scheme == "content") {
        shareUri(context, record.uri, record.fileName)
        return
    }
    val link = "https://x.com/i/status/${record.tweetId}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, link)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "分享链接")) }
        .onFailure { toast(context, "分享失败") }
}

/** True when the app may manage all files (Android 11+) or holds legacy storage access. */
fun hasAllFilesAccess(context: Context): Boolean = if (android.os.Build.VERSION.SDK_INT >= 30) {
    android.os.Environment.isExternalStorageManager()
} else {
    context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

/** Opens the system screen where the user can grant "all files" access. */
fun requestAllFilesAccess(context: Context) {
    if (android.os.Build.VERSION.SDK_INT >= 30) {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData(android.net.Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fallback = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { runCatching { context.startActivity(fallback) } }
    } else {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}

/**
 * Turns a folder URI picked with ACTION_OPEN_DOCUMENT_TREE into a file path the downloader can
 * write to once "all files" access is granted (for example
 * `content://…/tree/primary%3ADownload` becomes `/storage/emulated/0/Download`).
 */
fun folderPathFromUri(context: Context, uri: android.net.Uri): String {
    val path = uri.path.orEmpty()
    val treeIndex = path.indexOf("/tree")
    val marker = path.indexOf(':', treeIndex.coerceAtLeast(0))
    if (marker >= 0) {
        val relative = android.net.Uri.decode(path.substring(marker + 1)).trim('/')
        val volume = relative.substringBefore(':', "")
        val tail = relative.substringAfter(':', "")
        val storage = if (volume.equals("primary", ignoreCase = true)) {
            "/storage/emulated/0"
        } else {
            "/storage/$volume"
        }
        return if (tail.isBlank()) storage else "$storage/$tail"
    }
    return path
}

/** Opens an http(s) link in the system browser. */
fun openExternalUrl(context: Context, url: String) {
    if (url.isBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { toast(context, "没有可以打开该链接的应用") }
}

/** Copies text to the clipboard. */
fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        ?: return
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
    toast(context, "已复制")
}

fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

/** Resolves a history record back to a readable file path when it lives in app storage. */
fun recordFilePath(record: DownloadRecord): String? {
    val uri = Uri.parse(record.uri)
    if (uri.scheme == "file") return uri.path
    return null
}

fun fileSizeLabel(bytes: Long): String {
    if (bytes <= 0) return "未知"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) {
        "${value.toLong()} ${units[index]}"
    } else {
        String.format(java.util.Locale.getDefault(), "%.2f %s", value, units[index])
    }
}

fun speedLabel(bytesPerSecond: Long): String =
    if (bytesPerSecond <= 0) "--" else "${fileSizeLabel(bytesPerSecond)}/s"

fun timeLabel(millis: Long): String {
    if (millis <= 0) return ""
    val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return format.format(java.util.Date(millis))
}

fun File.readableName(): String = name
