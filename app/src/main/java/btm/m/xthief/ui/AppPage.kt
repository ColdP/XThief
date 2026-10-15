// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect
import btm.m.xthief.ui.components.GlassToolbarIconButton
import btm.m.xthief.ui.components.LocalPageBackdrop
import btm.m.xthief.ui.components.LocalToolbarBackdrop
import btm.m.xthief.ui.components.LocalToolbarCollapsed
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Page shell transcribed from HyperChanger's `AppPage`:
 * - MIUIX [TopAppBar]; when `showLargeTitle` is false the large-title text is empty, so the
 *   title only appears as the collapsed caption (HyperChanger's About page behaviour);
 * - the Backdrop progressive-blur + gradient top-bar material;
 * - the liquid-glass toolbar icon button, whose transparency follows the collapse fraction.
 */
@Composable
fun AppPage(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    contentBottomPadding: Int = 148,
    showLargeTitle: Boolean = true,
    background: (@Composable () -> Unit)? = null,
    floatingToolbarPosition: ToolbarPosition = ToolbarPosition.BottomCenter,
    floatingToolbar: @Composable () -> Unit = {},
    /**
     * When provided, the page body becomes a two-column staggered grid instead of a list. The top
     * bar, backdrop and floating toolbar behave identically either way.
     *
     * Declared *before* [content] so the trailing-lambda call form keeps binding to `content`.
     */
    staggeredContent: (LazyStaggeredGridScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val scroll = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val staggeredState = rememberLazyStaggeredGridState()
    val backdrop = rememberLayerBackdrop()
    val surface = MiuixTheme.colorScheme.surface
    val collapsedFraction = scroll.state.collapsedFraction.coerceIn(0f, 1f)
    val topBarMaterialProgress by animateFloatAsState(
        targetValue = collapsedFraction,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "topBarMaterialProgress",
    )
    Box(modifier.fillMaxSize().background(surface)) {
        background?.invoke()
        // Expose the page's own backdrop so in-page overlays (glass dropdowns) sample the exact
        // layer that is being recorded here, rather than one owned by an outer container.
        CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingToolbar = floatingToolbar,
            floatingToolbarPosition = floatingToolbarPosition,
            topBar = {
                CompositionLocalProvider(
                    LocalToolbarBackdrop provides backdrop,
                    LocalToolbarCollapsed provides collapsedFraction,
                ) {
                    Box {
                        // HyperChanger's top-bar material: pages that provide their own
                        // background use the progressive-blur mask, plain pages use a regular
                        // backdrop blur plus a tinted surface that fades as the bar collapses.
                        if (background == null) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .graphicsLayer { alpha = 1f - topBarMaterialProgress }
                                    .drawPlainBackdrop(
                                        backdrop = backdrop,
                                        shape = { RectangleShape },
                                        effects = { blur(6.dp.toPx()) },
                                        onDrawSurface = { drawRect(surface.copy(alpha = 0.55f)) },
                                    ),
                            )
                        }
                        Box(
                            Modifier
                                .matchParentSize()
                                .expandDrawHeight(TOP_BAR_MATERIAL_HEIGHT_FACTOR)
                                .graphicsLayer { alpha = topBarMaterialProgress }
                                .drawPlainBackdrop(
                                    backdrop = backdrop,
                                    shape = { RectangleShape },
                                    effects = {
                                        blur(10.dp.toPx())
                                        runtimeShaderEffect(
                                            key = "progressive-blur-alpha-mask",
                                            shaderString = PROGRESSIVE_BLUR_ALPHA_MASK_SHADER,
                                            uniformShaderName = "content",
                                        ) { setFloatUniform("size", size.width, size.height) }
                                    },
                                ),
                        )
                        CollapsedTopBarGradientLayer(surface, topBarMaterialProgress)
                        if (showLargeTitle) {
                            TopAppBar(
                                title = "",
                                largeTitle = title,
                                color = Color.Transparent,
                                scrollBehavior = scroll,
                                navigationIcon = {
                                    if (onBack != null) {
                                        GlassToolbarIconButton(
                                            icon = MiuixIcons.Regular.ChevronBackward,
                                            description = "返回",
                                            backdrop = backdrop,
                                            collapsedFraction = collapsedFraction,
                                            containerPadding = PaddingValues(start = 8.dp),
                                            onClick = onBack,
                                        )
                                    }
                                },
                                // Page actions sit inside a Row that shares the toolbar's
                                // backdrop, so their glass buttons sample the page correctly.
                                actions = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        content = actions,
                                    )
                                },
                            )
                            // Caption that fades in as the large title collapses.
                            Box(
                                Modifier
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .height(52.dp)
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = collapsedFraction }
                                    .padding(horizontal = 64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(title, style = MiuixTheme.textStyles.title3, maxLines = 1)
                            }
                        } else {
                            // HyperChanger's About page keeps an empty TopAppBar and drives the
                            // collapsed caption itself: nothing is shown while the page is at
                            // the top, and the title fades in once it collapses.
                            TopAppBar(
                                title = "",
                                largeTitle = "",
                                color = Color.Transparent,
                                scrollBehavior = scroll,
                                navigationIcon = {
                                    if (onBack != null) {
                                        GlassToolbarIconButton(
                                            icon = MiuixIcons.Regular.ChevronBackward,
                                            description = "返回",
                                            backdrop = backdrop,
                                            collapsedFraction = collapsedFraction,
                                            containerPadding = PaddingValues(start = 8.dp),
                                            onClick = onBack,
                                        )
                                    }
                                },
                                actions = actions,
                            )
                            Box(
                                Modifier
                                    .windowInsetsPadding(WindowInsets.statusBars)
                                    .height(52.dp)
                                    .fillMaxWidth()
                                    .graphicsLayer { alpha = collapsedFraction }
                                    .padding(horizontal = 64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(title, style = MiuixTheme.textStyles.title3, maxLines = 1)
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                val contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + contentBottomPadding.dp,
                )
                if (staggeredContent != null) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        state = staggeredState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scroll.nestedScrollConnection),
                        contentPadding = contentPadding,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                        content = staggeredContent,
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scroll.nestedScrollConnection),
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content,
                    )
                }
            }
        }
        }
    }
}

/** Toolbar action with HyperChanger's liquid-glass capsule background. */
@Composable
fun PageAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlassToolbarIconButton(
        icon = icon,
        description = description,
        onClick = onClick,
    )
}

@Composable
private fun BoxScope.CollapsedTopBarGradientLayer(surface: Color, progress: Float) {
    if (progress <= 0f) return
    Box(
        Modifier
            .matchParentSize()
            .expandDrawHeight(TOP_BAR_MATERIAL_HEIGHT_FACTOR)
            .graphicsLayer { alpha = progress }
            .background(
                Brush.verticalGradient(
                    0f to surface.copy(alpha = 0.92f),
                    0.72f to surface.copy(alpha = 0.18f),
                    1f to Color.Transparent,
                ),
            ),
    )
}

// Keep the progressive blur and gradient tint aligned to the same extended height.
private const val TOP_BAR_MATERIAL_HEIGHT_FACTOR = 1.4926275f

private fun Modifier.expandDrawHeight(factor: Float): Modifier = layout { measurable, constraints ->
    val expandedHeight = (constraints.maxHeight * factor).toInt()
    val placeable = measurable.measure(
        constraints.copy(minHeight = expandedHeight, maxHeight = expandedHeight),
    )
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.placeRelative(0, 0) }
}

private const val PROGRESSIVE_BLUR_ALPHA_MASK_SHADER = """
uniform shader content;
uniform float2 size;
half4 main(float2 coord) {
    float blurAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
    return content.eval(coord) * blurAlpha;
}
"""
