// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.pages

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import btm.m.xthief.data.MediaEntry
import btm.m.xthief.ui.AppPage
import btm.m.xthief.ui.components.EmptyState
import btm.m.xthief.ui.components.GlassFloatingToolbar
import btm.m.xthief.ui.components.GlassFloatingToolbarRow
import btm.m.xthief.ui.components.IconAction
import btm.m.xthief.ui.components.SensitiveBadge
import btm.m.xthief.ui.components.TOOLBAR_MAX_BLUR
import btm.m.xthief.ui.components.ToolbarAnimationTiming
import btm.m.xthief.ui.components.XThiefDialog
import btm.m.xthief.ui.fileSizeLabel
import btm.m.xthief.ui.timeLabel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.state.ToggleableState

@Composable
fun LibraryPage(
    entries: List<MediaEntry>,
    directory: String,
    onRefresh: () -> Unit,
    onOpen: (MediaEntry) -> Unit,
    onShare: (MediaEntry) -> Unit,
    onDelete: (MediaEntry) -> Unit,
    onDeleteAll: (List<MediaEntry>) -> Unit = {},
    /**
     * Host-driven visibility of the floating toolbar. It is delayed until the bottom navigation
     * has finished leaving the screen, so the two never overlap.
     */
    toolbarVisible: Boolean = false,
    /** Lets the host hide the bottom navigation while the selection toolbar is up. */
    onSelectionModeChange: (Boolean) -> Unit = {},
) {
    // Multi-select state, mirroring HyperChanger's list pages.
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var confirmBatchDelete by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MediaEntry?>(null) }
    var actionTarget by remember { mutableStateOf<MediaEntry?>(null) }

    fun closeSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun toggleSelection(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    // Refresh automatically on entry so changes made elsewhere show up without a manual tap.
    LaunchedEffect(Unit) { onRefresh() }

    // Report the selection state so the host can animate the bottom navigation away.
    LaunchedEffect(selectionMode) { onSelectionModeChange(selectionMode) }
    DisposableEffect(Unit) {
        onDispose { onSelectionModeChange(false) }
    }

    // Drop selections whose entries disappeared (e.g. after delete + refresh).
    LaunchedEffect(entries) {
        if (selectedIds.isNotEmpty()) {
            val alive = entries.mapTo(hashSetOf()) { it.uri }
            selectedIds = selectedIds intersect alive
        }
    }

    BackHandler(enabled = selectionMode) { closeSelection() }

    AppPage(
        title = "媒体库",
        floatingToolbarPosition = ToolbarPosition.BottomCenter,
        floatingToolbar = {
            AnimatedVisibility(
                visible = toolbarVisible,
                enter = fadeIn(tween(ToolbarAnimationTiming.ENTER_MS)) +
                    scaleIn(tween(ToolbarAnimationTiming.ENTER_MS), initialScale = .86f),
                exit = fadeOut(tween(ToolbarAnimationTiming.EXIT_MS)) +
                    scaleOut(tween(ToolbarAnimationTiming.EXIT_MS), targetScale = .9f),
            ) {
                // Blur-in alongside the fade: the capsule resolves out of a blur as it appears.
                val blurProgress by animateFloatAsState(
                    targetValue = if (toolbarVisible) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = ToolbarAnimationTiming.ENTER_MS,
                        easing = FastOutSlowInEasing,
                    ),
                    label = "toolbarBlur",
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
                                selectedIds = if (selectedIds.size == entries.size) {
                                    emptySet()
                                } else {
                                    entries.mapTo(linkedSetOf()) { it.uri }
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
                IconAction(
                    icon = MiuixIcons.Regular.Refresh,
                    description = "刷新",
                    onClick = onRefresh,
                )
            }
        },
    ) {
        if (entries.isEmpty()) {
            item {
                EmptyState(
                    title = "媒体库是空的",
                    description = "下载完成的视频会自动出现在这里。也可以点右上角刷新，重新扫描 $directory。",
                    icon = {
                        androidx.compose.foundation.Image(
                            imageVector = MiuixIcons.Regular.Photos,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                    },
                )
            }
        } else {
            items(entries, key = { it.uri }) { entry ->
                MediaCard(
                    entry = entry,
                    selectionMode = selectionMode,
                    selected = entry.uri in selectedIds,
                    onClick = {
                        if (selectionMode) toggleSelection(entry.uri) else actionTarget = entry
                    },
                    onLongPress = {
                        if (selectionMode) {
                            toggleSelection(entry.uri)
                        } else {
                            selectionMode = true
                            selectedIds = selectedIds + entry.uri
                        }
                    },
                    onToggle = { toggleSelection(entry.uri) },
                )
            }
        }
    }

    actionTarget?.let { entry ->
        MediaActionDialog(
            title = entry.displayName,
            subtitle = buildString {
                append(fileSizeLabel(entry.sizeBytes))
                if (entry.width > 0 && entry.height > 0) {
                    append(" · ${minOf(entry.width, entry.height)}p")
                }
                if (entry.durationMs > 0) append(" · ${durationLabel(entry.durationMs)}")
            },
            onPlay = { actionTarget = null; onOpen(entry) },
            onShare = { actionTarget = null; onShare(entry) },
            onDelete = { actionTarget = null; pendingDelete = entry },
            onDismiss = { actionTarget = null },
        )
    }

    pendingDelete?.let { entry ->
        XThiefDialog(
            title = "删除视频",
            message = "「${entry.displayName}」将从设备存储中永久删除。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                onDelete(entry)
                pendingDelete = null
                onRefresh()
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (confirmBatchDelete) {
        val targets = entries.filter { it.uri in selectedIds }
        XThiefDialog(
            title = "删除 ${targets.size} 个视频",
            message = "所选视频将从设备存储中永久删除，此操作不可撤销。",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                confirmBatchDelete = false
                onDeleteAll(targets)
                closeSelection()
                // Re-scan so the list reflects what is actually left on disk.
                onRefresh()
            },
            onDismiss = { confirmBatchDelete = false },
        )
    }
}

/** One library entry, using MIUIX's card padding and an animated selection checkbox. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCard(
    entry: MediaEntry,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.5.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongPress),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.displayName,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (entry.sensitive) {
                        SensitiveBadge(Modifier.padding(start = 8.dp))
                    }
                }
                Text(
                    text = buildString {
                        append(fileSizeLabel(entry.sizeBytes))
                        if (entry.width > 0 && entry.height > 0) {
                            append(" · ${minOf(entry.width, entry.height)}p")
                        }
                        if (entry.durationMs > 0) append(" · ${durationLabel(entry.durationMs)}")
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = buildString {
                        append(timeLabel(entry.addedAt))
                        if (entry.directory.isNotBlank()) append(" · ${entry.directory.trimEnd('/')}")
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
}

private fun durationLabel(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
