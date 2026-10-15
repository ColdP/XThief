// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import btm.m.xthief.XThiefApplication
import btm.m.xthief.data.DownloadRecord
import btm.m.xthief.ui.AppPage
import btm.m.xthief.ui.components.BlurFadeVisibility
import btm.m.xthief.ui.components.EmptyState
import btm.m.xthief.ui.components.GlassDialogButton
import btm.m.xthief.ui.components.IconAction
import btm.m.xthief.ui.components.SectionHeader
import btm.m.xthief.ui.components.ThinProgressBar
import btm.m.xthief.ui.components.WindowDialog
import btm.m.xthief.ui.components.XThiefDialog
import btm.m.xthief.ui.fileSizeLabel
import btm.m.xthief.ui.speedLabel
import btm.m.xthief.ui.timeLabel
import btm.m.xthief.x.DownloadStatus
import btm.m.xthief.x.DownloadTask
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Maximum number of rows rendered inside one collapsible group. */
private const val GROUP_LIMIT = 20

/** Blur radius used while a grouped row fades in or out. */
private val GROUP_BLUR = 14.dp

/**
 * The 下载 tab: active downloads, today's finished downloads and the older history, each in a
 * MIUIX card that can be collapsed down to its first row.
 */
@Composable
fun HistoryPage(
    container: XThiefApplication,
    records: List<DownloadRecord>,
    tasks: List<DownloadTask>,
    onDelete: (DownloadRecord) -> Unit,
    onClear: () -> Unit,
    onOpen: (DownloadRecord) -> Unit,
    onShare: (DownloadRecord) -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<DownloadRecord?>(null) }

    val today = LocalDate.now()
    fun dayOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    val activeTasks = tasks.filter { it.isActive || it.status == DownloadStatus.PAUSED }
    val todayRecords = records.filter { dayOf(it.createdAt) == today }
    val olderRecords = records.filter { dayOf(it.createdAt) != today }

    AppPage(
        title = "下载",
        actions = {
            if (records.isNotEmpty() || tasks.isNotEmpty()) {
                IconAction(
                    icon = MiuixIcons.Regular.Delete,
                    description = "清空记录",
                    tint = MiuixTheme.colorScheme.error,
                    onClick = { confirmClear = true },
                )
            }
        },
    ) {
        if (activeTasks.isEmpty() && records.isEmpty()) {
            item {
                EmptyState(
                    title = "还没有下载任务",
                    description = "在「解析」页填入 X（Twitter）视频链接并选择画质，任务就会出现在这里；下载完成的视频会按日期归档。",
                    icon = {
                        androidx.compose.foundation.Image(
                            painter = painterResource(btm.m.xthief.R.drawable.ic_xthief_sp),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                        )
                    },
                )
            }
        }

        if (activeTasks.isNotEmpty()) {
            item {
                DownloadGroup(
                    title = "进行中",
                    count = activeTasks.size,
                    tasks = activeTasks.take(GROUP_LIMIT),
                    container = container,
                )
            }
        }

        if (todayRecords.isNotEmpty()) {
            item {
                HistoryGroup(
                    title = "今日",
                    count = todayRecords.size,
                    records = todayRecords.take(GROUP_LIMIT),
                    onLongPress = { actionTarget = it },
                )
            }
        }

        if (olderRecords.isNotEmpty()) {
            item {
                HistoryGroup(
                    title = "历史下载",
                    count = olderRecords.size,
                    records = olderRecords.take(GROUP_LIMIT),
                    onLongPress = { actionTarget = it },
                )
            }
        }
    }

    actionTarget?.let { record ->
        MediaActionDialog(
            title = record.authorHandle.ifBlank { "未知作者" },
            subtitle = buildString {
                if (record.byteCount > 0) append(fileSizeLabel(record.byteCount))
                if (record.variantLabel.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(record.variantLabel)
                }
            },
            onPlay = { actionTarget = null; onOpen(record) },
            onShare = { actionTarget = null; onShare(record) },
            onDelete = { actionTarget = null; onDelete(record) },
            onDismiss = { actionTarget = null },
        )
    }

    if (confirmClear) {
        XThiefDialog(
            title = "清空下载记录",
            message = "所有历史记录都会被删除，已下载的视频文件不受影响。",
            confirmText = "清空",
            destructive = true,
            onConfirm = { confirmClear = false; onClear() },
            onDismiss = { confirmClear = false },
        )
    }
}

/** Collapse / expand a grouped row with the shared blur + fade transition. */
@Composable
private fun GroupedRow(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    BlurFadeVisibility(visible = visible) { content() }
}

/** MIUIX card holding a group of rows, collapsible to its first row. */
@Composable
private fun GroupCard(
    title: String,
    count: Int,
    rowCount: Int,
    content: @Composable (expanded: Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    val collapsible = rowCount > 1
    SectionHeader(
        title = title,
        subtitle = "$count 个",
        action = if (collapsible) {
            {
                Text(
                    text = if (expanded) "收起" else "展开",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        } else {
            null
        },
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.5.dp,
        insideMargin = PaddingValues(vertical = 4.dp),
    ) {
        content(expanded)
    }
}

/** Active downloads group: the first row stays visible while collapsed. */
@Composable
private fun DownloadGroup(
    title: String,
    count: Int,
    tasks: List<DownloadTask>,
    container: XThiefApplication,
) {
    GroupCard(title = title, count = count, rowCount = tasks.size) { expanded ->
        Column(Modifier.fillMaxWidth()) {
            tasks.forEachIndexed { index, task ->
                val row: @Composable () -> Unit = {
                    DownloadTaskRow(
                        task = task,
                        onPause = { container.downloadManager.pause(task.id) },
                        onResume = { container.downloadManager.start(task.id) },
                        onCancel = { container.downloadManager.cancel(task.id) },
                        onRetry = { container.downloadManager.retry(task.id) },
                        onRemove = { container.downloadManager.remove(task.id) },
                    )
                }
                if (index == 0) row() else GroupedRow(visible = expanded) { row() }
            }
        }
    }
}

/** Finished-download group: long-press a row for play / share / delete. */
@Composable
private fun HistoryGroup(
    title: String,
    count: Int,
    records: List<DownloadRecord>,
    onLongPress: (DownloadRecord) -> Unit,
) {
    GroupCard(title = title, count = count, rowCount = records.size) { expanded ->
        Column(Modifier.fillMaxWidth()) {
            records.forEachIndexed { index, record ->
                val row: @Composable () -> Unit = {
                    HistoryRow(record = record, onLongPress = { onLongPress(record) })
                }
                if (index == 0) row() else GroupedRow(visible = expanded) { row() }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    record: DownloadRecord,
    onLongPress: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onLongPress, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = record.fileName.ifBlank { "未命名视频" },
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = buildString {
                if (record.authorHandle.isNotBlank()) append("${record.authorHandle} · ")
                if (record.variantLabel.isNotBlank()) append(record.variantLabel)
                if (record.byteCount > 0) append(" · ${fileSizeLabel(record.byteCount)}")
            },
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
        Text(
            text = timeLabel(record.createdAt),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun DownloadTaskRow(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(task.variant.resolutionLabel)
                        when (task.status) {
                            DownloadStatus.QUEUED -> append(" · 排队中")
                            DownloadStatus.RUNNING -> append(" · ${speedLabel(task.speedBytesPerSecond)}")
                            DownloadStatus.PAUSED -> append(" · 已暂停")
                            DownloadStatus.COMPLETED -> append(" · 已完成 ${fileSizeLabel(task.bytesDownloaded)}")
                            DownloadStatus.FAILED -> append(" · 失败")
                            DownloadStatus.CANCELLED -> append(" · 已取消")
                        }
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = if (task.status == DownloadStatus.FAILED) {
                        MiuixTheme.colorScheme.error
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                when (task.status) {
                    DownloadStatus.RUNNING -> IconAction(
                        icon = MiuixIcons.Regular.Pause,
                        description = "暂停",
                        size = 20,
                        onClick = onPause,
                    )

                    DownloadStatus.PAUSED,
                    DownloadStatus.QUEUED -> IconAction(
                        icon = MiuixIcons.Regular.Play,
                        description = "继续",
                        size = 20,
                        onClick = onResume,
                    )

                    DownloadStatus.FAILED,
                    DownloadStatus.CANCELLED -> IconAction(
                        icon = MiuixIcons.Regular.Refresh,
                        description = "重试",
                        size = 20,
                        onClick = onRetry,
                    )

                    DownloadStatus.COMPLETED -> IconAction(
                        icon = MiuixIcons.Regular.Download,
                        description = "移除任务",
                        size = 20,
                        onClick = onRemove,
                    )
                }
                if (task.status == DownloadStatus.COMPLETED ||
                    task.status == DownloadStatus.FAILED ||
                    task.status == DownloadStatus.CANCELLED
                ) {
                    IconAction(
                        icon = MiuixIcons.Regular.Delete,
                        description = "移除",
                        size = 20,
                        tint = MiuixTheme.colorScheme.error,
                        onClick = onRemove,
                    )
                } else {
                    IconAction(
                        icon = MiuixIcons.Regular.Close,
                        description = "取消",
                        size = 20,
                        tint = MiuixTheme.colorScheme.error,
                        onClick = onCancel,
                    )
                }
            }
        }
        if (task.status == DownloadStatus.RUNNING || task.status == DownloadStatus.PAUSED) {
            Spacer(Modifier.height(10.dp))
            ThinProgressBar(progress = task.progress)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${(task.progress * 100).toInt()}% · ${fileSizeLabel(task.bytesDownloaded)}" +
                    if (task.totalBytes > 0) " / ${fileSizeLabel(task.totalBytes)}" else "",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        if (task.error != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = task.error.orEmpty(),
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.error,
            )
        }
    }
}

/**
 * Long-press action sheet shared by history and media-library rows: the poster as the title,
 * the file facts as the summary, then play / share / delete.
 */
@Composable
fun MediaActionDialog(
    title: String,
    subtitle: String,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    WindowDialog(
        show = true,
        title = title,
        summary = subtitle.ifBlank { null },
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassDialogButton(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
                Text("播放")
            }
            GlassDialogButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Text("分享")
            }
            GlassDialogButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.error,
                    contentColor = MiuixTheme.colorScheme.onError,
                ),
            ) {
                Text("删除")
            }
            GlassDialogButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("取消")
            }
        }
    }
}
