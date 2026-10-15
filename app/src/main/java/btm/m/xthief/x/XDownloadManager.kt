// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.x

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
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
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
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
    /** The source post was flagged sensitive; carried into the history record. */
    val sensitive: Boolean = false,
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
        // The same rendition must never be queued twice: a double tap (or a screen rebuild while a
        // task is still running) would otherwise start a second download of an identical file.
        val duplicate = _tasks.value.firstOrNull { existing ->
            existing.tweetId == media.tweetId &&
                existing.variant.url == variant.url &&
                existing.status != DownloadStatus.FAILED &&
                existing.status != DownloadStatus.CANCELLED
        }
        if (duplicate != null) return duplicate.id

        val fileName = buildFileName(media, variant, settings)
        val task = DownloadTask(
            id = UUID.randomUUID().toString(),
            tweetId = media.tweetId,
            title = TweetLink.displayAuthor(media.authorHandle, media.author),
            subtitle = media.text.take(120),
            variant = variant,
            fileName = fileName,
            sensitive = media.sensitive,
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
            target = createTarget(task.fileName, settings)
            val tracker = SpeedTracker()
            var downloaded = 0L
            var lastIslandUpdate = 0L

            fun reportProgress(bytes: Long, total: Long) {
                downloaded = bytes
                update(id) {
                    it.copy(
                        bytesDownloaded = bytes,
                        totalBytes = if (total > 0) total else bytes,
                        speedBytesPerSecond = tracker.record(bytes),
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

            if (task.variant.isHls) {
                // HLS renditions (long/sensitive posts have no progressive MP4) are assembled from
                // the playlist's fMP4 init segment and parts, then the audio track is muxed in.
                downloaded = streamHls(task.variant, target) { bytes, total ->
                    reportProgress(bytes, total)
                }
            } else {
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
                connection.inputStream.use { input ->
                    target.openOutput().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            reportProgress(downloaded + read, total)
                        }
                        output.flush()
                    }
                }
            }
            val uri = target.finish(downloaded)
            update(id) {
                it.copy(
                    status = DownloadStatus.COMPLETED,
                    bytesDownloaded = downloaded,
                    // HLS tasks have no Content-Length, so the final size is what we wrote.
                    totalBytes = downloaded,
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
        sensitive = task.sensitive || media?.sensitive == true,
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

    // ------------------------------------------------------------------ HLS

    /**
     * Downloads an HLS rendition and writes a single MP4 to [target].
     *
     * X serves fMP4: every media playlist starts with an `#EXT-X-MAP` init segment (holding the
     * `moov` box) followed by `.m4s` parts, so appending them in order already yields a valid MP4.
     * The audio lives on a separate playlist; when present both tracks are muxed with the platform
     * [MediaMuxer] so the result keeps its sound.
     *
     * @return the number of bytes written to the target.
     */
    private suspend fun streamHls(
        variant: VideoVariant,
        target: DownloadTarget,
        onProgress: (bytes: Long, total: Long) -> Unit,
    ): Long {
        val workspace = File(appContext.cacheDir, "hls").apply { mkdirs() }
        val stamp = "${System.currentTimeMillis()}_${variant.url.hashCode()}"
        val videoFile = File(workspace, "video_$stamp.mp4")
        val audioFile = File(workspace, "audio_$stamp.mp4")
        var muxedFile: File? = null
        try {
            var videoBytes = 0L
            videoBytes += fetchPlaylistInto(variant.url, videoFile) { written ->
                onProgress(written, -1L)
            }
            val audioUrl = variant.hlsAudioUrl
            var audioBytes = 0L
            if (audioUrl != null) {
                audioBytes = fetchPlaylistInto(audioUrl, audioFile) { }
            }
            val total = videoBytes + audioBytes

            if (audioBytes > 0) {
                val (file, bytes) = muxTracks(videoFile, audioFile)
                muxedFile = file
                file.inputStream().use { input ->
                    target.openOutput().use { output ->
                        input.copyToWithProgress(output, bytes) { copied ->
                            onProgress(copied, bytes)
                        }
                    }
                }
                return bytes
            }

            // Video-only fallback: the concatenated fMP4 is already playable.
            videoFile.inputStream().use { input ->
                target.openOutput().use { output ->
                    input.copyToWithProgress(output, total) { copied ->
                        onProgress(copied, total)
                    }
                }
            }
            return total
        } finally {
            videoFile.delete()
            audioFile.delete()
            muxedFile?.delete()
        }
    }

    /**
     * Downloads a media playlist into [dest]: first the `#EXT-X-MAP` init segment, then every part,
     * appended in playlist order. Returns the number of bytes written.
     */
    private fun fetchPlaylistInto(
        playlistUrl: String,
        dest: File,
        onProgress: (Long) -> Unit,
    ): Long {
        val text = httpGetText(playlistUrl)
        val origin = originOf(playlistUrl)
        val entries = mutableListOf<String>()
        text.lineSequence().map(String::trim).forEach { line ->
            when {
                line.startsWith("#EXT-X-MAP:") ->
                    attr(line, "URI").takeIf(String::isNotBlank)?.let { entries += it }

                line.isBlank() || line.startsWith("#") -> Unit

                else -> entries += line
            }
        }
        if (entries.isEmpty()) throw IOException("HLS 播放列表为空")

        var written = 0L
        dest.outputStream().use { output ->
            for (entry in entries) {
                written += downloadInto(absoluteUrl(origin, entry), output)
                onProgress(written)
            }
            output.flush()
        }
        return written
    }

    /** Streams one HLS part into [output]. */
    private fun downloadInto(url: String, output: OutputStream): Long {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT)
            setRequestProperty("Accept", "*/*")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("分片下载失败（HTTP $code）")
            var total = 0L
            connection.inputStream.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    total += read
                }
            }
            total
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Muxes the downloaded video and audio tracks into one MP4 on disk.
     *
     * @return the muxed file and its size; the caller owns (and must delete) the file.
     */
    private fun muxTracks(videoFile: File, audioFile: File): Pair<File, Long> {
        val workspace = videoFile.parentFile ?: appContext.cacheDir
        val output = File(workspace, "muxed_${System.currentTimeMillis()}.mp4")
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val extractors = mutableListOf<MediaExtractor>()
        try {
            val videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            val audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
            extractors += videoExtractor
            extractors += audioExtractor

            val videoSource = firstTrackOf(videoExtractor, "video/")
            if (videoSource < 0) throw IOException("HLS 视频轨解析失败")
            val audioSource = firstTrackOf(audioExtractor, "audio/")

            videoExtractor.selectTrack(videoSource)
            val videoTarget = muxer.addTrack(videoExtractor.getTrackFormat(videoSource))
            var audioTarget = -1
            if (audioSource >= 0) {
                audioExtractor.selectTrack(audioSource)
                audioTarget = muxer.addTrack(audioExtractor.getTrackFormat(audioSource))
            }

            muxer.start()
            val buffer = ByteBuffer.allocate(1 shl 20)
            val info = MediaCodec.BufferInfo()
            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            copySamples(videoExtractor, muxer, videoTarget, buffer, info)
            if (audioTarget >= 0) {
                audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                copySamples(audioExtractor, muxer, audioTarget, buffer, info)
            }
            muxer.stop()
        } catch (error: Throwable) {
            runCatching { output.delete() }
            throw error
        } finally {
            runCatching { muxer.release() }
            extractors.forEach { runCatching { it.release() } }
        }
        return output to output.length()
    }

    private fun firstTrackOf(extractor: MediaExtractor, mimePrefix: String): Int {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index)
                .getString(MediaFormat.KEY_MIME)
                .orEmpty()
            if (mime.startsWith(mimePrefix)) return index
        }
        return -1
    }

    private fun copySamples(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        trackIndex: Int,
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
    ) {
        while (true) {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            info.offset = 0
            info.size = size
            info.presentationTimeUs = extractor.sampleTime
            info.flags = extractor.sampleFlags
            runCatching { muxer.writeSampleData(trackIndex, buffer, info) }
            if (!extractor.advance()) break
        }
    }

    private fun java.io.InputStream.copyToWithProgress(
        output: OutputStream,
        total: Long,
        onProgress: (Long) -> Unit,
    ) {
        val buffer = ByteArray(64 * 1024)
        var copied = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            copied += read
            onProgress(copied)
        }
        output.flush()
    }

    private fun httpGetText(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", DOWNLOAD_USER_AGENT)
            setRequestProperty("Accept", "*/*")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("播放列表下载失败（HTTP $code）")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun originOf(url: String): String = runCatching {
        val uri = URI(url)
        "${uri.scheme}://${uri.host}"
    }.getOrDefault("https://video.twimg.com")

    /** Playlist entries are root-relative (`/amplify_video/...`). */
    private fun absoluteUrl(origin: String, value: String): String = when {
        value.startsWith("http", ignoreCase = true) -> value
        value.startsWith("/") -> origin + value
        else -> "$origin/$value"
    }

    private fun attr(line: String, name: String): String {
        val marker = "$name="
        val start = line.indexOf(marker)
        if (start < 0) return ""
        val rest = line.substring(start + marker.length)
        return if (rest.startsWith("\"")) {
            rest.substringAfter('"').substringBefore('"')
        } else {
            rest.substringBefore(',')
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
