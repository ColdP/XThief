// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.components

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * The page-level Backdrop recorded by `AppPage`. Screens reach it from here instead of receiving
 * it as a parameter, so overlays such as glass dropdowns sample the page content that is actually
 * recorded behind them.
 */
val LocalPageBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * Reveals or hides [content] with a combined height, alpha, scale and blur transition, so
 * toggling a card feels smooth rather than snapping. Used for the collapsible download groups and
 * for settings rows that appear conditionally.
 */
@Composable
fun BlurFadeVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(320, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top,
        ) + fadeIn(tween(260, easing = FastOutSlowInEasing)) + scaleIn(
            animationSpec = tween(320, easing = FastOutSlowInEasing),
            initialScale = .97f,
        ),
        exit = shrinkVertically(
            animationSpec = tween(260, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top,
        ) + fadeOut(tween(200, easing = FastOutSlowInEasing)) + scaleOut(
            animationSpec = tween(260, easing = FastOutSlowInEasing),
            targetScale = .97f,
        ),
    ) {
        BlurFadeContent(modifier = modifier, visible = visible) { content() }
    }
}

/**
 * Applies the blur (and a light alpha lift) that accompanies a reveal. The blur radius scales with
 * the measured height so short rows are not smeared while tall cards still get a visible effect.
 */
@Composable
fun BlurFadeContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    /** Fixed blur radius; when null the radius scales with the measured height. */
    maxBlur: Dp? = null,
    content: @Composable () -> Unit,
) {
    val settled by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "blurFade",
    )
    var heightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val effectiveBlur = maxBlur ?: with(density) {
        // ~6% of the row height, clamped so small rows stay legible.
        (heightPx * .06f).toDp().coerceIn(.5.dp, 16.dp)
    }
    val radius: Dp = effectiveBlur * (1f - settled.coerceIn(0f, 1f))
    Column(
        modifier
            .fillMaxWidth()
            .onSizeChanged { heightPx = it.height }
            .graphicsLayer { alpha = .55f + .45f * settled.coerceIn(0f, 1f) }
            .then(if (radius > .1.dp) Modifier.blur(radius) else Modifier),
    ) {
        content()
    }
}
