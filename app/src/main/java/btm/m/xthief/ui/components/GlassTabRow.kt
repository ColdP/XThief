// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val TAB_ROW_HEIGHT = 42.dp
private val TAB_ROW_SHAPE = RoundedCornerShape(50.dp)
private val TAB_ROW_BLUR = 6.dp

/**
 * Scrolling capsule tab row for the media library's folders.
 *
 * Layered the way HyperModifier does it: a translucent container colour underneath, an independent
 * Backdrop glass surface above it, then the selection indicator and labels on top. The shape is a
 * capsule and the strip scrolls horizontally, so any number of folders stays reachable.
 *
 * @param onTabLongPress fired when a tab is held, used to open the folder editor.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onTabLongPress: (Int) -> Unit = {},
) {
    if (tabs.isEmpty()) return
    val listState = rememberLazyListState()
    val backdrop = LocalPageBackdrop.current

    val container = MiuixTheme.colorScheme.surfaceContainer
    val isDark = container.luminance() < .5f
    val contentColor = MiuixTheme.colorScheme.onSurfaceContainer
    // Tinted container under the glass, so the capsule stays legible even with no backdrop.
    val containerColor = container.copy(alpha = if (isDark) .74f else .82f)
    // Subtle pill behind the active label.
    val indicatorColor = if (isDark) {
        Color.White.copy(alpha = .13f)
    } else {
        Color.Black.copy(alpha = .075f)
    }

    // Keep the active tab in view when the selection is changed from outside.
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in tabs.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    val shape = TAB_ROW_SHAPE
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(TAB_ROW_BLUR.toPx())
            },
            highlight = { Highlight.Default.copy(alpha = .55f) },
            shadow = { Shadow.Default.copy(radius = 10.dp, color = Color.Black, alpha = .28f) },
            innerShadow = null,
            onDrawSurface = {
                // Capsule: radius is half the row height.
                val radius = CornerRadius(
                    (TAB_ROW_HEIGHT / 2).toPx(),
                    (TAB_ROW_HEIGHT / 2).toPx(),
                )
                drawRoundRect(color = containerColor, cornerRadius = radius)
            },
        )
    } else {
        Modifier.clip(shape).background(containerColor)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(TAB_ROW_HEIGHT)
            .clip(shape)
            .then(glassModifier),
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(tabs) { index, title ->
                val selected = index == selectedIndex
                Box(
                    Modifier
                        .clip(TAB_ROW_SHAPE)
                        .background(if (selected) indicatorColor else Color.Transparent)
                        .combinedClickable(
                            onClick = { onTabSelected(index) },
                            onLongClick = { onTabLongPress(index) },
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        color = if (selected) contentColor else contentColor.copy(alpha = .50f),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
