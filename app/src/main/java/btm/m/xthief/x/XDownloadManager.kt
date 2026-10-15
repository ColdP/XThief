// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.x

import android.content.ContentValues
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import btm.m.xthief.data.DEFAULT_SAVE_DIR
import btm.m.xthief.data.DownloadRecord
import btm.m.xthief.data.FileNameRule
import btm.m.xthief.data.TweetMedia
import btm.m.xthief.data.VideoVariant
import btm.m.xthief.data.XThiefSettings
import btm.m.xthief.data.XThiefSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

enum class DownloadStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }

data class DownloadTask(
    val id: String,
    val tweetId: String,
    val title: String,
    val subtitle: String,
    val variant: VideoVariant,
    val fileName: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val speedBytesPerSecond: Long = 0L,
    val error: String? = null,
    val savedUri: String? = null,
) {
    val progress: Float
        get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val isActive: Boolean get() = status == DownloadStatus.RUNNING || status == DownloadStatus.QUEUED
}

/**
 * Sequential download engine with pause / resume / cancel support.
 * Files land in the MediaStore video collection so they appear in the media library.
 */
class XDownloadManager(
    context: Context,
    private val settingsStore: XThiefSettingsStore,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<String, Job>()
    private val notifier by lazy { DownloadNotifier(appContext) }
    private val island by lazy { IslandNotifier(appContext) }

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    fun task(id: String): DownloadTask? = _tasks.value.firstOrNull { it.id == id }

    fun enqueue(media: TweetMedia, variant: VideoVariant, settings: XThiefSettings = settingsStore.current): String {
        val fileName = buildFileName(media, variant, settings)
        val task = DownloadTask(
            id = UUID.randomUUID().toString(),
            tweetId = media.tweetId,
            title = TweetLink.displayAuthor(media.authorHandle, media.author),
            subtitle = media.text.take(120),
            variant = variant,
            fileName = fileName,
        )
        _tasks.value = listOf(task) + _tasks.value
        start(task.id)
        return task.id
    }

    fun start(id: String) {
        val current = task(id) ?: return
        if (current.status == DownloadStatus.RUNNING) return
        update(id) { it.copy(status = DownloadStatus.QUEUED, error = null) }
        dispatch()
    }

    /** Runs queued tasks while respecting the configured concurrency limit. */
    private fun dispatch() {
        val limit = settingsStore.current.concurrentDownloads.coerceIn(1, 4)
        while (jobs.size < limit) {
            val next = _tasks.value.lastOrNull { it.status == DownloadStatus.QUEUED } ?: return
            jobs[next.id] = scope.launch { runTask(next.id) }
        }
    }

    fun pause(id: String) {
        val current = task(id) ?: return
        if (current.status != DownloadStatus.RUNNING) return
        jobs.remove(id)?.cancel()
        update(id) { it.copy(status = DownloadStatus.PAUSED, speedBytesPerSecond = 0L) }
    }

    fun cancel(id: String) {
        val current = task(id) ?: return
        jobs.remove(id)?.cancel()
        if (current.status == DownloadStatus.RUNNING || current.status == DownloadStatus.QUEUED) {
            update(id) { it.copy(status = DownloadStatus.CANCELLED, speedBytesPerSecond = 0L) }
        }
    }

    fun retry(id: String) {
        update(id) { it.copy(bytesDownloaded = 0L, status = DownloadStatus.QUEUED, error = null) }
        start(id)
    }

    fun remove(id: String) {
        jobs.remove(id)?.cancel()
        _tasks.value = _tasks.value.filterNot { it.id == id }
    }

    fun clearFinished() {
        _tasks.value = _tasks.value.filter { it.isActive }
    }

    private suspend fun runTask(id: String) {
        val task = task(id) ?: return
        val settings = settingsStore.current
        var connection: HttpURLConnection? = null
        var target: DownloadTarget? = null
        try {
            if (settings.wifiOnly && !isOnWifi()) {
                throw IOException("已开启「仅 Wi-Fi 下载」，当前不在 Wi-Fi 网络")
            }
            update(id) { it.copy(status = DownloadStatus.RUNNING, bytesDownloaded = 0L, totalBytes = -1L) }
            connection = (URL(task.variant.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT)
                setRequestProperty("Accept", "*/*")
            }
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("下载失败（HTTP $code）")
            val total = connection.contentLengthLong
            target = createTarget(task.fileName, settings)
            val tracker = SpeedTracker()
            var downloaded = 0L
            var lastIslandUpdate = 0L
            connection.inputStream.use { input ->
                target.openOutput().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val speed = tracker.record(downloaded)
                        update(id) {
                            it.copy(
                                bytesDownloaded = downloaded,
                                totalBytes = if (total > 0) total else downloaded,
                                speedBytesPerSecond = speed,
                            )
                        }
                        // Refresh the island at most every 500ms so it stays smooth.
                        if (settings.islandEnabled) {
                            val now = System.currentTimeMillis()
                            if (now - lastIslandUpdate >= ISLAND_UPDATE_INTERVAL_MS) {
                                lastIslandUpdate = now
                                task(id)?.let { island.updateProgress(it) }
                            }
                        }
                    }
                    output.flush()
                }
            }
            val uri = target.finish(downloaded)
            update(id) {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    bytesDownloaded = downloaded,
                    totalBytes = if (total > 0) total else downloaded,
                    speedBytesPerSecond = 0L,
                    savedUri = uri,
                )
            }
            // Only one completion notification may be posted per task. The island and the plain
            // notifier both key off task.id.hashCode(), so posting both makes the plain one
            // replace the island notification (same id, different channel) and the island
            // disappears. The island already carries the result text, so it wins when enabled.
            if (settings.islandEnabled) {
                task(id)?.let { finished -> island.notifyResult(finished, success = true) }
            } else if (settings.showDownloadNotifications) {
                task(id)?.let { finished -> notifier.notifyCompleted(finished, downloaded) }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            target?.abort()
            throw cancelled
        } catch (error: Throwable) {
            target?.abort()
            update(id) {
                it.copy(
                    status = DownloadStatus.FAILED,
                    error = error.message ?: "下载失败",
                    speedBytesPerSecond = 0L,
                )
            }
            if (settings.islandEnabled) {
                task(id)?.let { failed -> island.notifyResult(failed, success = false) }
            } else if (settings.showDownloadNotifications) {
                task(id)?.let { failed ->
                    notifier.notifyCompleted(failed, failed.bytesDownloaded)
                }
            }
        } finally {
            connection?.disconnect()
            jobs.remove(id)
            dispatch()
        }
    }

    fun toRecord(task: DownloadTask, media: TweetMedia?): DownloadRecord = DownloadRecord(
        id = task.id,
        tweetId = task.tweetId,
        author = media?.author.orEmpty(),
        authorHandle = media?.authorHandle.orEmpty(),
        text = task.subtitle,
        variantLabel = task.variant.resolutionLabel,
        bitrate = task.variant.bitrate,
        fileName = task.fileName,
        uri = task.savedUri.orEmpty(),
        byteCount = task.bytesDownloaded,
        createdAt = System.currentTimeMillis(),
        completed = task.status == DownloadStatus.COMPLETED,
    )

    private fun update(id: String, transform: (DownloadTask) -> DownloadTask) {
        _tasks.value = _tasks.value.map { if (it.id == id) transform(it) else it }
    }

    /** True when the active network is Wi-Fi or Ethernet. */
    private fun isOnWifi(): Boolean {
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // ------------------------------------------------------------ file plumbing

    private fun buildFileName(media: TweetMedia, variant: VideoVariant, settings: XThiefSettings): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val handle = media.authorHandle.removePrefix("@").ifBlank { "x" }
        val base = when (settings.nameRule) {
            FileNameRule.AUTHOR_DATE -> "${handle}_${stamp}"
            FileNameRule.TWEET_ID -> media.tweetId
            FileNameRule.ORIGINAL -> "x_${media.tweetId}_${variant.resolutionLabel}"
            FileNameRule.CUSTOM -> {
                val prefix = settings.customPrefix.ifBlank { "XThief" }
                "${prefix}_${media.tweetId}_${variant.resolutionLabel}"
            }
        }
        return sanitize(base) + ".mp4"
    }

    private fun sanitize(value: String): String = value
        .replace(Regex("""[\\/:*?"<>|\s]+"""), "_")
        .trim('_')
        .ifBlank { "xthief_${System.currentTimeMillis()}" }
        .take(96)

    private interface DownloadTarget {
        fun openOutput(): java.io.OutputStream
        fun finish(bytes: Long): String
        fun abort()
    }

    private inner class MediaStoreTarget(
        private val displayName: String,
        private val relativePath: String,
        private val mimeType: String,
    ) : DownloadTarget {
        private var pendingUri: Uri? = null

        override fun openOutput(): java.io.OutputStream {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val uri = appContext.contentResolver.insert(collection, values)
                ?: throw IOException("无法在媒体库中创建文件")
            pendingUri = uri
            return appContext.contentResolver.openOutputStream(uri)
                ?: throw IOException("无法写入媒体库文件")
        }

        override fun finish(bytes: Long): String {
            val uri = pendingUri ?: return ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                    put(MediaStore.Video.Media.SIZE, bytes)
                }
                appContext.contentResolver.update(uri, values, null, null)
            }
            return uri.toString()
        }

        override fun abort() {
            pendingUri?.let { runCatching { appContext.contentResolver.delete(it, null, null) } }
            pendingUri = null
        }
    }

    private inner class AppStorageTarget(
        private val directory: File,
        private val displayName: String,
        private val mimeType: String,
    ) : DownloadTarget {
        private var partFile: File? = null
        private var finalFile: File? = null

        override fun openOutput(): java.io.OutputStream {
            directory.mkdirs()
            val part = File(directory, "$displayName.part")
            partFile = part
            finalFile = File(directory, displayName)
            return part.outputStream()
        }

        override fun finish(bytes: Long): String {
            val part = partFile
            val target = finalFile
            if (part != null && target != null) {
                if (target.exists()) target.delete()
                if (!part.renameTo(target)) {
                    part.copyTo(target, overwrite = true)
                    part.delete()
                }
            }
            return target?.let { Uri.fromFile(it).toString() }.orEmpty()
        }

        override fun abort() {
            partFile?.delete()
            partFile = null
        }
    }

    private fun createTarget(fileName: String, settings: XThiefSettings): DownloadTarget {
        val mime = "video/mp4"
        return if (settings.usesCustomDirectory && settings.customDirectory.isNotBlank()) {
            // User-picked folder; requires the "all files" access granted in settings.
            AppStorageTarget(File(settings.customDirectory), fileName, mime)
        } else {
            val relative = DEFAULT_SAVE_DIR.trim('/').trimEnd('/') + "/"
            MediaStoreTarget(fileName, relative, mime)
        }
    }

    private class SpeedTracker {
        private val startedAt = System.currentTimeMillis()
        private val lastSampleAt = AtomicBoolean(false)
        private var lastBytes = 0L
        private var lastTime = System.currentTimeMillis()
        private var speed = 0L

        fun record(bytes: Long): Long {
            val now = System.currentTimeMillis()
            if (now - lastTime >= 400) {
                val delta = bytes - lastBytes
                if (delta > 0) speed = delta * 1000L / (now - lastTime).coerceAtLeast(1)
                lastBytes = bytes
                lastTime = now
            }
            if (startedAt == 0L) lastSampleAt.set(true)
            return speed
        }
    }

    companion object {
        private const val DOWNLOAD_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/133.0.0.0 Mobile Safari/537.36"

        /** Minimum gap between two Super Island progress refreshes. */
        private const val ISLAND_UPDATE_INTERVAL_MS = 500L
    }
}
