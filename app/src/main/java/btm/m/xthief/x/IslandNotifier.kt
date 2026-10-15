// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.x

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import btm.m.xthief.MainActivity
import btm.m.xthief.R
import btm.m.xthief.data.VideoVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Publishes download progress to Xiaomi's Super Island (超级岛).
 *
 * The island payload is a **JSON string** under the `miui.focus.param` extra; pictures it
 * references by key travel as `Icon` values inside the `miui.focus.pics` bundle. The ROM must
 * report `notification_focus_protocol >= 3`, and `business` must be a scenario HyperOS accepts.
 *
 * Layout follows the published template library:
 *
 * - expand (展开态) template 7 「IM图文组件 + 识别图形组件1 + 进度组件2」 for file downloads:
 *   `chatInfo` (avatar + title + content) / `picInfo` / `progressInfo`;
 * - collapse (摘要态) 小岛 「图标组合组件」: `combinePicInfo` = `picInfo` + an outer progress ring
 *   using `colorReach` / `colorUnReach`;
 * - 大岛 `bigIslandArea`: A 区 `imageTextInfoLeft` + B 区 `progressTextInfo`.
 *
 * Field names matter: progress rings take `colorReach` / `colorUnReach`, while the linear bar
 * takes `colorProgress` / `colorProgressEnd`. An unknown key silently renders nothing.
 *
 * @see <a href="https://dev.mi.com/xiaomihyperos/documentation/detail?pId=2131">开发指南</a>
 */
class IslandNotifier(private val context: Context) {

    private val manager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var simulation: Job? = null

    /** True when the ROM advertises island support (`persist.sys.feature.island`). */
    val isSupported: Boolean
        get() = runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getDeclaredMethod(
                "getBoolean",
                String::class.java,
                Boolean::class.javaPrimitiveType,
            )
            method.invoke(null, "persist.sys.feature.island", false) as? Boolean ?: false
        }.getOrDefault(false)

    /**
     * Focus-notification protocol reported by the ROM: 1 = OS1, 2 = OS2, 3 = OS3. Super Island
     * requires at least 3.
     */
    val protocolVersion: Int
        get() = runCatching {
            android.provider.Settings.System.getInt(
                context.contentResolver,
                "notification_focus_protocol",
                0,
            )
        }.getOrDefault(0)

    fun updateProgress(task: DownloadTask) {
        if (manager == null) return
        ensureChannel()
        postProgress(
            id = task.id.hashCode(),
            title = task.title.ifBlank { "XThief" },
            description = task.subtitle.ifBlank { task.fileName },
            percent = (task.progress * 100).toInt().coerceIn(0, 100),
        )
    }

    fun notifyResult(task: DownloadTask, success: Boolean) {
        if (manager == null) return
        ensureChannel()
        val title = task.title.ifBlank { "XThief" }
        val result = if (success) "下载完成" else (task.error ?: "下载失败")
        post(
            id = task.id.hashCode(),
            notification = buildNotification(title, result, percent = null, ongoing = true)
                .apply {
                    buildIslandParam(title, result, title, percent = null)
                        ?.let { extras.putString(EXTRAS_PARAM, it) }
                }
                .build(),
        )
    }

    fun cancel(id: String) {
        runCatching { manager?.cancel(id.hashCode()) }
    }

    /**
     * Fires a simulated island; [durationMillis] controls how long the progress takes.
     *
     * The simulation builds a real [DownloadTask] and goes through exactly the same
     * `updateProgress` / `notifyResult` calls as a genuine download, so the island payload is
     * byte-for-byte what a real transfer produces.
     */
    fun simulate(durationMillis: Long = DEFAULT_SIMULATION_MS) {
        if (manager == null) return
        ensureChannel()
        simulation?.cancel()
        val id = SIMULATION_ID
        val total = 100L * 1024 * 1024
        val variant = VideoVariant(
            url = "https://video.twimg.com/xthief-simulation.mp4",
            bitrate = 2_176_000,
            contentType = "video/mp4",
            width = 1920,
            height = 1080,
        )
        fun task(bytes: Long, status: DownloadStatus, speed: Long = 0L) = DownloadTask(
            id = id,
            tweetId = "0000000000000000000",
            title = "小米超级岛模拟",
            subtitle = "XThief · 1080p",
            variant = variant,
            fileName = "XThief-simulation-1080p.mp4",
            status = status,
            bytesDownloaded = bytes,
            totalBytes = total,
            speedBytesPerSecond = speed,
        )
        val steps = 50
        val interval = (durationMillis / steps).coerceAtLeast(30L)
        simulation = scope.launch {
            for (step in 0..steps) {
                val bytes = total * step / steps
                updateProgress(task(bytes, DownloadStatus.RUNNING, speed = 4_200_000L))
                delay(interval)
            }
            notifyResult(task(total, DownloadStatus.COMPLETED), success = true)
        }
    }

    private fun postProgress(id: Int, title: String, description: String, percent: Int) {
        post(
            id = id,
            notification = buildNotification(title, "$description · $percent%", percent, true)
                .apply {
                    buildIslandParam(title, description, "$title · $percent%", percent)
                        ?.let { extras.putString(EXTRAS_PARAM, it) }
                }
                .build(),
        )
    }

    /**
     * Builds the `miui.focus.param` JSON for 展开态模版7 + 摘要态模版2.
     *
     * Returns null when the payload would exceed the 3 KB limit SystemUI enforces.
     */
    private fun buildIslandParam(
        title: String,
        description: String,
        ticker: String,
        percent: Int?,
    ): String? {
        val iconKey = PIC_IMAGE_TEXT
        val picInfo = JSONObject().put("type", 1).put("pic", iconKey)
        // Progress is pinned to 100% once finished so the capsule keeps its ring instead of
        // collapsing to nothing.
        val shownPercent = percent ?: 100

        // 摘要态 · 小岛：图标组合组件（中心图标 + 外圈环形进度）。
        val smallIsland = JSONObject().put(
            "combinePicInfo",
            JSONObject()
                .put("picInfo", picInfo)
                .put("progressInfo", ringProgress(shownPercent)),
        )

        // 摘要态 · 大岛：A 区图文组件1 + B 区进度文本组件。
        val bigIsland = JSONObject()
            .put(
                "imageTextInfoLeft",
                JSONObject()
                    .put("type", 1)
                    .put("picInfo", picInfo)
                    .put(
                        "textInfo",
                        JSONObject()
                            .put("title", title)
                            .put("showHighlightColor", false),
                    ),
            )
            .put(
                "progressTextInfo",
                JSONObject()
                    .put("progressInfo", ringProgress(shownPercent))
                    .put(
                        "textInfo",
                        JSONObject()
                            .put("title", "$shownPercent%")
                            .put("content", description)
                            .put("showHighlightColor", false),
                    ),
            )

        val paramIsland = JSONObject()
            .put("islandProperty", 1)
            .put("islandTimeout", 43_200)
            .put("bigIslandArea", bigIsland)
            .put("smallIslandArea", smallIsland)

        // 展开态 · 模版7：IM图文组件 + 识别图形组件1 + 进度组件2
        val paramV2 = JSONObject()
            .put("protocol", 1)
            .put("business", BUSINESS)
            // Neither float switch is enabled: otherwise every progress refresh re-expands the
            // capsule into the big island.
            .put("islandFirstFloat", false)
            .put("enableFloat", false)
            .put("updatable", true)
            .put("timeout", 720)
            .put("ticker", ticker)
            .put("tickerPic", iconKey)
            .put("aodTitle", title)
            .put("aodPic", iconKey)
            .put("filterWhenNoPermission", false)
            .put(
                "chatInfo",
                JSONObject()
                    .put("picProfile", iconKey)
                    .put("title", title)
                    .put("content", description),
            )
            .put("picInfo", picInfo)
        if (percent != null) {
            paramV2.put("progressInfo", barProgress(percent))
        }
        paramV2.put("param_island", paramIsland)

        val json = JSONObject().put(PARAM_V2, paramV2).toString()
        if (json.toByteArray(Charsets.UTF_8).size > MAX_FOCUS_PARAM_BYTES) return null
        return json
    }

    /** Outer ring for the capsule / progress text block: uses colorReach / colorUnReach. */
    private fun ringProgress(percent: Int) = JSONObject()
        .put("progress", percent)
        .put("colorReach", PROGRESS_COLOR)
        .put("colorUnReach", PROGRESS_TRACK_COLOR)
        .put("isCCW", false)

    /** 进度组件2 (纯净版水平进度条): progress + colorProgress / colorProgressEnd. */
    private fun barProgress(percent: Int) = JSONObject()
        .put("progress", percent)
        .put("colorProgress", PROGRESS_COLOR)
        .put("colorProgressEnd", PROGRESS_COLOR)

    private fun buildNotification(
        title: String,
        text: String,
        percent: Int?,
        ongoing: Boolean,
    ): Notification.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        builder
            .setSmallIcon(R.drawable.ic_xthief)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STATUS)
        if (percent != null) {
            builder.setProgress(100, percent, false)
        }
        val bitmap = decodeIslandIcon()
        if (bitmap != null) {
            val icon = Icon.createWithBitmap(bitmap)
            builder.extras.putBundle(
                EXTRAS_PICS,
                Bundle().apply {
                    putParcelable(PIC_IMAGE_TEXT, icon)
                    putParcelable(PIC_APP, icon)
                    putParcelable(PIC_TICKER, icon)
                    putParcelable(PIC_AOD, icon)
                },
            )
        }
        return builder
    }

    private fun post(id: Int, notification: Notification) {
        runCatching { manager?.notify(id, notification) }
    }

    private fun decodeIslandIcon() = runCatching {
        BitmapFactory.decodeResource(context.resources, R.drawable.ic_xthief)
    }.getOrNull()

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager?.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "下载任务",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "在小米超级岛与通知栏显示下载进度"
            setShowBadge(false)
        }
        runCatching { manager?.createNotificationChannel(channel) }
    }

    companion object {
        private const val CHANNEL_ID = "xthief_island"
        private const val SIMULATION_ID = "xthief-island-simulation"

        /** Default simulated download duration. */
        const val DEFAULT_SIMULATION_MS = 15_000L

        /** Holds the island JSON; SystemUI parses this extra as a string. */
        private const val EXTRAS_PARAM = "miui.focus.param"
        /** Holds the pictures referenced by `pic` keys inside the JSON. */
        private const val EXTRAS_PICS = "miui.focus.pics"

        private const val PARAM_V2 = "param_v2"

        /** SystemUI rejects focus payloads larger than 3 KB. */
        private const val MAX_FOCUS_PARAM_BYTES = 3_072

        /** Must be a scenario HyperOS accepts for island display. */
        private const val BUSINESS = "file_download"

        /** Keys into the `miui.focus.pics` bundle; must match the `pic` values in the JSON. */
        private const val PIC_IMAGE_TEXT = "miui.focus.pic_imageText"
        private const val PIC_APP = "miui.focus.pic_xthief"
        private const val PIC_TICKER = "miui.focus.pic_ticker"
        private const val PIC_AOD = "miui.focus.pic_aod"

        private const val PROGRESS_COLOR = "#3482FF"
        private const val PROGRESS_TRACK_COLOR = "#333333"
    }
}
