// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Folder chooser shared by the media library's selection toolbar and the player's favourite button.
 *
 * When [favoriteToggle] is supplied the sheet also offers to star / unstar the item, so the player
 * can fold both actions into one button.
 *
 * @param onPick receives the chosen folder, or null to take the item out of every folder.
 */
@Composable
fun FolderPickerSheet(
    folders: List<String>,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
    title: String = "移动到",
    emptyHint: String = "还没有文件夹，先在媒体库的「添加」里建一个。",
    favoriteToggle: FavoriteToggle? = null,
) {
    WindowDialog(show = true, onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
            )
            favoriteToggle?.let { toggle ->
                GlassDialogButton(
                    onClick = { toggle.onToggle() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (toggle.favorite) "取消收藏" else "收藏") }
            }
            if (folders.isEmpty() && favoriteToggle == null) {
                Text(
                    text = emptyHint,
                    modifier = Modifier.fillMaxWidth(),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Start,
                )
            }
            folders.forEach { folder ->
                GlassDialogButton(
                    onClick = { onPick(folder) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(folder) }
            }
            if (folders.isNotEmpty()) {
                GlassDialogButton(
                    onClick = { onPick(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("移出文件夹") }
            }
            GlassDialogButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("取消")
            }
        }
    }
}

/** Optional star / unstar action shown at the top of the picker. */
data class FavoriteToggle(val favorite: Boolean, val onToggle: () -> Unit)
