// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Liquid-glass floating toolbar transcribed from HyperChanger's `GlassLyricFloatingToolbar`: a
 * capsule whose material is sampled from the page backdrop, with a touch highlight and a soft
 * shadow. Used for multi-select actions on the media library.
 */
@Composable
fun GlassFloatingToolbar(
    modifier: Modifier = Modifier,
    /**
     * Overrides the surface tint. The default leans on the theme surface at 80% alpha, which reads
     * as a flat block over video; the player passes a much lighter scrim so the backdrop shows.
     */
    surfaceTint: Color? = null,
    /** Capsule by default; the player switches to a rounded rectangle while scrubbing. */
    cornerRadius: Dp = 50.dp,
    /** Shadow colour. The theme default is grey on light themes, which shows as a pale block. */
    shadowTint: Color? = null,
    /**
     * Strength of the glass highlight. The highlight is an edge-lit white sheen, so blurring it
     * during a hide/show animation smears it into a white patch; callers that animate can scale
     * this down with the same progress.
     */
    highlightAlpha: Float = .70f,
    content: @Composable () -> Unit,
) {
    // Scaffold's floatingToolbar slot sits outside the topBar's LocalToolbarBackdrop provider,
    // so fall back to the page backdrop that AppPage publishes for in-page overlays.
    val backdrop = LocalToolbarBackdrop.current ?: LocalPageBackdrop.current
    val animationScope = rememberCoroutineScope()
    val touchHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope,
            radiusMultiplier = .4f,
            surfaceAlpha = 0f,
            falloffMultiplier = .25f,
        ) { size, offset ->
            Offset(
                offset.x.coerceIn(0f, size.width),
                offset.y.coerceIn(0f, size.height),
            )
        }
    }
    val surface = MiuixTheme.colorScheme.surfaceContainer
    val isLightTheme = surface.luminance() > .5f
    val tint = surfaceTint
        ?: (if (isLightTheme) Color(0xFFF8F8F8) else surface).copy(alpha = .80f)
    val shadowColor = shadowTint ?: if (isLightTheme) Color.Gray else Color.Black
    val shape = RoundedCornerShape(cornerRadius)
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(3.dp.toPx())
                lens(12.dp.toPx(), 32.dp.toPx(), chromaticAberration = true)
            },
            highlight = { Highlight.Default.copy(alpha = highlightAlpha) },
            shadow = {
                Shadow.Default.copy(
                    radius = 12.dp,
                    color = shadowColor,
                    alpha = .35f,
                )
            },
            innerShadow = null,
            // Must follow the shape: a plain drawRect spills past the capsule's rounded corners
            // and shows up as an opaque block while the bar animates away.
            onDrawSurface = {
                val radius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                drawRoundRect(color = tint, cornerRadius = radius)
            },
        )
    } else {
        Modifier.clip(shape).background(tint)
    }
    FloatingToolbar(
        modifier = modifier
            .then(glassModifier)
            .then(touchHighlight.gestureModifier)
            .then(touchHighlight.modifier),
        color = Color.Transparent,
        cornerRadius = cornerRadius,
        shadowElevation = 0.dp,
        showDivider = false,
        content = content,
    )
}

/** Row wrapper matching HyperChanger's toolbar button spacing. */
@Composable
fun GlassFloatingToolbarRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        content = { content() },
    )
}

/**
 * Blur radius the floating toolbar resolves out of as it appears. Shared by the pages that host
 * one so the reveal looks identical everywhere.
 */
val TOOLBAR_MAX_BLUR = 12.dp

/**
 * Floating-toolbar enter/exit durations, scaled to 85% of the original 220/160 ms. Exposed so the
 * host can wait for the toolbar to leave before bringing the bottom navigation back.
 */
object ToolbarAnimationTiming {
    /** Blur + fade + scale in. */
    const val ENTER_MS = 187

    /** Fade + scale out. */
    const val EXIT_MS = 136
}
