// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Checkbox sheet for filing a clip into folders.
 *
 * Multi-select on purpose: a video can live in several folders, and 收藏 is offered as just another
 * box rather than a special case. Row layout and checkbox colours follow HyperChanger's
 * scope-restart dialog.
 *
 * @param initialFavorites whether the clip is currently starred.
 * @param initialFolders folders the clip is already in.
 * @param onConfirm receives the ticked folders plus the star state.
 */
@Composable
fun FolderPickerSheet(
    folders: List<String>,
    initialFavorites: Boolean,
    initialFolders: Set<String>,
    onConfirm: (favorite: Boolean, folders: Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var favorite by remember { mutableStateOf(initialFavorites) }
    var selected by remember { mutableStateOf(initialFolders) }

    WindowDialog(show = true, onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "添加到文件夹",
                modifier = Modifier.fillMaxWidth(),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
            )

            Column(Modifier.fillMaxWidth()) {
                CheckRow(
                    label = "收藏",
                    checked = favorite,
                    onToggle = { favorite = !favorite },
                )
                folders.forEach { folder ->
                    CheckRow(
                        label = folder,
                        checked = folder in selected,
                        onToggle = {
                            selected = if (folder in selected) {
                                selected - folder
                            } else {
                                selected + folder
                            }
                        },
                    )
                }
                if (folders.isEmpty()) {
                    Text(
                        text = "还没有文件夹，先在媒体库的「添加」里建一个。",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Start,
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassDialogButton(
                    onClick = { onConfirm(favorite, selected) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        color = Color(0xFF0088FF),
                        contentColor = Color.White,
                    ),
                ) { Text("添加") }
                GlassDialogButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
            }
        }
    }
}

/** One label / checkbox row; the whole row is tappable, like HyperChanger's. */
@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            state = if (checked) ToggleableState.On else ToggleableState.Off,
            onClick = onToggle,
            colors = CheckboxDefaults.checkboxColors(
                uncheckedForegroundColor = Color(0x808F8F8F),
            ),
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
