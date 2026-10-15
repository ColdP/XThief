// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.x

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import btm.m.xthief.MainActivity
import btm.m.xthief.R

/** Posts a system notification when a download finishes. */
class DownloadNotifier(private val context: Context) {

    private val manager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val existing = manager?.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "下载完成",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "XThief 视频下载完成提醒"
            setShowBadge(true)
        }
        manager?.createNotificationChannel(channel)
    }

    fun notifyCompleted(task: DownloadTask, byteCount: Long) {
        if (manager == null) return
        if (!hasPermission()) return
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_xthief_sp)
            .setContentTitle("下载完成")
            .setContentText("${task.fileName} · ${formatSize(byteCount)}")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        runCatching { manager?.notify(plainNotificationId(task.id), notification) }
    }

    /**
     * Plain completion notifications use their own id space so they can never collide with the
     * Super Island's focus notification for the same task — a shared id would make one replace
     * the other.
     */
    private fun plainNotificationId(taskId: String): Int = taskId.hashCode() + PLAIN_ID_OFFSET

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "未知大小"
        val units = listOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }
        return "%.2f %s".format(value, units[index])
    }

    private companion object {
        const val CHANNEL_ID = "xthief_downloads"

        /** Keeps plain notification ids away from the island's ids. */
        const val PLAIN_ID_OFFSET = 0x40000000
    }
}
