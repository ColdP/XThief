// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import btm.m.xthief.data.MediaEntry
import btm.m.xthief.ui.AppPage
import btm.m.xthief.ui.components.EmptyState
import btm.m.xthief.ui.components.IconAction
import btm.m.xthief.ui.components.XThiefDialog
import btm.m.xthief.ui.fileSizeLabel
import btm.m.xthief.ui.timeLabel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LibraryPage(
    entries: List<MediaEntry>,
    directory: String,
    onRefresh: () -> Unit,
    onOpen: (MediaEntry) -> Unit,
    onShare: (MediaEntry) -> Unit,
    onDelete: (MediaEntry) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<MediaEntry?>(null) }
    var actionTarget by remember { mutableStateOf<MediaEntry?>(null) }
    AppPage(
        title = "媒体库",
        actions = {
            IconAction(
                icon = MiuixIcons.Regular.Refresh,
                description = "刷新",
                onClick = onRefresh,
            )
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
                    onLongPress = { actionTarget = entry },
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
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** One library entry, using MIUIX's card padding (no extra nested indentation). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCard(
    entry: MediaEntry,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onLongPress, onLongClick = onLongPress),
        cornerRadius = 22.5.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = entry.displayName,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
}

private fun durationLabel(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
