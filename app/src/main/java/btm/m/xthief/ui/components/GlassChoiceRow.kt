// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem

/**
 * A single-choice row built on HyperChanger's glass dropdown menu. Used where a preference
 * store is not involved (for example the per-download quality and destination pickers).
 */
@Composable
fun GlassChoiceRow(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    enabled: Boolean = true,
    /** Subtitle shown under the title, independent of the selected value. */
    summary: String? = null,
    onSelectedIndexChange: (Int) -> Unit,
) {
    if (items.isEmpty()) return
    val selected = selectedIndex.coerceIn(0, items.lastIndex)
    val entry = remember(items, selected) {
        DropdownEntry(
            items = items.mapIndexed { index, text ->
                DropdownItem(
                    text = text,
                    selected = index == selected,
                    enabled = true,
                    onClick = { onSelectedIndexChange(index) },
                )
            },
        )
    }
    GlassDropdownMenu(
        entry = entry,
        title = title,
        modifier = modifier,
        enabled = enabled,
        backdrop = backdrop,
        showValueOnEnd = true,
        summaryOverride = summary,
    )
}
