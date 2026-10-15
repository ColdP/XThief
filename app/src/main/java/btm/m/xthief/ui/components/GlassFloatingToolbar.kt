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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    content: @Composable () -> Unit,
) {
    val backdrop = LocalToolbarBackdrop.current
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
    val tint = (if (isLightTheme) Color(0xFFF8F8F8) else surface).copy(alpha = .80f)
    val shadowColor = if (isLightTheme) Color.Gray else Color.Black
    val shape = RoundedCornerShape(50.dp)
    val glassModifier = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(3.dp.toPx())
                lens(12.dp.toPx(), 32.dp.toPx(), chromaticAberration = true)
            },
            highlight = { Highlight.Default.copy(alpha = .70f) },
            shadow = {
                Shadow.Default.copy(
                    radius = 12.dp,
                    color = shadowColor,
                    alpha = .35f,
                )
            },
            innerShadow = null,
            onDrawSurface = { drawRect(tint) },
        )
    } else {
        Modifier.clip(shape).background(tint)
    }
    FloatingToolbar(
        modifier = glassModifier
            .then(touchHighlight.gestureModifier)
            .then(touchHighlight.modifier),
        color = Color.Transparent,
        cornerRadius = 50.dp,
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
