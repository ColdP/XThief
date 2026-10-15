// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.pages

import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import btm.m.xthief.XThiefApplication
import btm.m.xthief.data.DownloadRecord
import btm.m.xthief.ui.AppPage
import btm.m.xthief.ui.components.BlurFadeVisibility
import btm.m.xthief.ui.components.EmptyState
import btm.m.xthief.ui.components.GlassDialogButton
import btm.m.xthief.ui.components.GlassFloatingToolbar
import btm.m.xthief.ui.components.GlassFloatingToolbarRow
import btm.m.xthief.ui.components.IconAction
import btm.m.xthief.ui.components.SectionHeader
import btm.m.xthief.ui.components.SensitiveBadge
import btm.m.xthief.ui.components.TOOLBAR_MAX_BLUR
import btm.m.xthief.ui.components.ThinProgressBar
import btm.m.xthief.ui.components.ToolbarAnimationTiming
import btm.m.xthief.ui.components.WindowDialog
import btm.m.xthief.ui.components.XThiefDialog
import btm.m.xthief.ui.fileSizeLabel
import btm.m.xthief.ui.speedLabel
import btm.m.xthief.ui.timeLabel
import btm.m.xthief.x.DownloadStatus
import btm.m.xthief.x.DownloadTask
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.SelectAll
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
    /**
     * Host-driven visibility of the floating toolbar; delayed until the bottom navigation has
     * finished leaving the screen so the two never overlap.
     */
    toolbarVisible: Boolean = false,
    onSelectionModeChange: (Boolean) -> Unit = {},
) {
    var confirmClear by remember { mutableStateOf(false) }
    var confirmBatchDelete by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<DownloadRecord?>(null) }
    // Multi-select over tasks and history rows alike; both are keyed by the same task id.
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }

    fun closeSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun toggleSelection(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    val today = LocalDate.now()
    fun dayOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    val activeTasks = tasks.filter { it.isActive || it.status == DownloadStatus.PAUSED }
    val todayRecords = records.filter { dayOf(it.createdAt) == today }
    val olderRecords = records.filter { dayOf(it.createdAt) != today }

    val selectableIds = (activeTasks.map { it.id } + records.map { it.id }).toSet()

    // Report the selection state so the host can animate the bottom navigation away.
    LaunchedEffect(selectionMode) { onSelectionModeChange(selectionMode) }
    DisposableEffect(Unit) {
        onDispose { onSelectionModeChange(false) }
    }
    BackHandler(enabled = selectionMode) { closeSelection() }

    AppPage(
        title = "下载",
        floatingToolbarPosition = ToolbarPosition.BottomCenter,
        floatingToolbar = {
            AnimatedVisibility(
                visible = toolbarVisible,
                enter = fadeIn(tween(ToolbarAnimationTiming.ENTER_MS)) +
                    scaleIn(tween(ToolbarAnimationTiming.ENTER_MS), initialScale = .86f),
                exit = fadeOut(tween(ToolbarAnimationTiming.EXIT_MS)) +
                    scaleOut(tween(ToolbarAnimationTiming.EXIT_MS), targetScale = .9f),
            ) {
                val blurProgress by animateFloatAsState(
                    targetValue = if (toolbarVisible) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = ToolbarAnimationTiming.ENTER_MS,
                        easing = FastOutSlowInEasing,
                    ),
                    label = "downloadToolbarBlur",
                )
                val blurPx = with(LocalDensity.current) {
                    (TOOLBAR_MAX_BLUR * (1f - blurProgress.coerceIn(0f, 1f))).toPx()
                }
                Box(
                    Modifier.graphicsLayer {
                        alpha = blurProgress.coerceIn(0f, 1f)
                        renderEffect =
                            if (blurPx > .1f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                BlurEffect(blurPx, blurPx, TileMode.Clamp)
                            } else {
                                null
                            }
                    },
                ) {
                    GlassFloatingToolbar {
                        GlassFloatingToolbarRow {
                            IconButton(onClick = {
                                selectedIds = if (selectedIds.size == selectableIds.size) {
                                    emptySet()
                                } else {
                                    selectableIds
                                }
                            }) {
                                Icon(
                                    MiuixIcons.Regular.SelectAll,
                                    "全选",
                                    Modifier.size(26.4.dp),
                                )
                            }
                            IconButton(
                                enabled = selectedIds.isNotEmpty(),
                                onClick = { confirmBatchDelete = true },
                            ) {
                                Icon(
                                    MiuixIcons.Regular.Delete,
                                    "删除",
                                    Modifier.size(26.4.dp),
                                    tint = Color(0xFFD32F2F),
                                )
                            }
                            IconButton(onClick = { closeSelection() }) {
                                Icon(
                                    MiuixIcons.Regular.Close,
                                    "关闭",
                                    Modifier.size(26.4.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        actions = {
            AnimatedVisibility(
                visible = !selectionMode,
                enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = .9f),
                exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = .9f),
            ) {
                if (records.isNotEmpty() || tasks.isNotEmpty()) {
                    IconAction(
                        icon = MiuixIcons.Regular.Delete,
                        description = "清空记录",
                        tint = MiuixTheme.colorScheme.error,
                        onClick = { confirmClear = true },
                    )
                }
            }
        },
    ) {
        if (activeTasks.isEmpty() && records.isEmpty()) {
            item {
                EmptyState(
                    title = "还没有下载任务",
                    description = "在「解析」页填入 X（Twitter）视频链接并选择画质，任务就会出现在这里；下载完成的视频会按日期归档。",
                    icon = {
                        // Vector + theme tint, matching the other empty states and dark mode.
                        Icon(
                            MiuixIcons.Regular.Recent,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
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
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    onToggle = ::toggleSelection,
                    onLongPress = { id ->
                        selectionMode = true
                        selectedIds = selectedIds + id
                    },
                )
            }
        }

        if (todayRecords.isNotEmpty()) {
            item {
                HistoryGroup(
                    title = "今日",
                    count = todayRecords.size,
                    records = todayRecords.take(GROUP_LIMIT),
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    onToggle = ::toggleSelection,
                    onClick = { actionTarget = it },
                    onLongPress = { id ->
                        selectionMode = true
                        selectedIds = selectedIds + id
                    },
                )
            }
        }

        if (olderRecords.isNotEmpty()) {
            item {
                HistoryGroup(
                    title = "历史下载",
                    count = olderRecords.size,
                    records = olderRecords.take(GROUP_LIMIT),
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    onToggle = ::toggleSelection,
                    onClick = { actionTarget = it },
                    onLongPress = { id ->
                        selectionMode = true
                        selectedIds = selectedIds + id
                    },
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

    if (confirmBatchDelete) {
        val taskTargets = tasks.filter { it.id in selectedIds }
        val recordTargets = records.filter { it.id in selectedIds }
        XThiefDialog(
            title = "删除 ${selectedIds.size} 项",
            message = buildString {
                if (taskTargets.isNotEmpty()) {
                    append("${taskTargets.size} 个下载任务将被移除")
                }
                if (recordTargets.isNotEmpty()) {
                    if (isNotEmpty()) append("，")
                    append("${recordTargets.size} 条历史记录将被删除")
                }
                append("。已下载的视频文件不受影响。")
            },
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                confirmBatchDelete = false
                taskTargets.forEach { container.downloadManager.remove(it.id) }
                recordTargets.forEach { onDelete(it) }
                closeSelection()
            },
            onDismiss = { confirmBatchDelete = false },
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
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    GroupCard(title = title, count = count, rowCount = tasks.size) { expanded ->
        Column(Modifier.fillMaxWidth()) {
            tasks.forEachIndexed { index, task ->
                val row: @Composable () -> Unit = {
                    DownloadTaskRow(
                        task = task,
                        selectionMode = selectionMode,
                        selected = task.id in selectedIds,
                        onToggle = { onToggle(task.id) },
                        onLongPress = { onLongPress(task.id) },
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

/** Finished-download group: long-press a row to start selecting. */
@Composable
private fun HistoryGroup(
    title: String,
    count: Int,
    records: List<DownloadRecord>,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onClick: (DownloadRecord) -> Unit,
    onLongPress: (String) -> Unit,
) {
    GroupCard(title = title, count = count, rowCount = records.size) { expanded ->
        Column(Modifier.fillMaxWidth()) {
            records.forEachIndexed { index, record ->
                val row: @Composable () -> Unit = {
                    HistoryRow(
                        record = record,
                        selectionMode = selectionMode,
                        selected = record.id in selectedIds,
                        onClick = { if (selectionMode) onToggle(record.id) else onClick(record) },
                        onLongPress = { onLongPress(record.id) },
                        onToggle = { onToggle(record.id) },
                    )
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
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = record.fileName.ifBlank { "未命名视频" },
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (record.sensitive) {
                    SensitiveBadge(Modifier.padding(start = 8.dp))
                }
            }
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
        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 2 },
            exit = fadeOut(tween(120)) + slideOutHorizontally(tween(160)) { it / 2 },
        ) {
            Checkbox(
                state = if (selected) ToggleableState.On else ToggleableState.Off,
                onClick = onToggle,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadTaskRow(
    task: DownloadTask,
    selectionMode: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggle() },
                onLongClick = onLongPress,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
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
            // Row actions give way to a checkbox while selecting.
            AnimatedVisibility(
                visible = !selectionMode,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120)),
            ) {
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
            AnimatedVisibility(
                visible = selectionMode,
                enter = fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 2 },
                exit = fadeOut(tween(120)) + slideOutHorizontally(tween(160)) { it / 2 },
            ) {
                Checkbox(
                    state = if (selected) ToggleableState.On else ToggleableState.Off,
                    onClick = onToggle,
                    modifier = Modifier.padding(start = 12.dp),
                )
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
    inFolder: Boolean = false,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRemoveFromFolder: () -> Unit = {},
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Drawn here rather than through WindowDialog's title/summary slots, which are centred.
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Start,
                )
            }
            GlassDialogButton(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
                Text("播放")
            }
            GlassDialogButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Text("分享")
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (inFolder) {
                    GlassDialogButton(
                        onClick = onRemoveFromFolder,
                        modifier = Modifier.weight(1f),
                    ) { Text("移出文件夹") }
                }
                GlassDialogButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    // Default surface with red text, not a filled red button.
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MiuixTheme.colorScheme.error,
                    ),
                ) {
                    Text("删除")
                }
            }
            GlassDialogButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("取消")
            }
        }
    }
}
