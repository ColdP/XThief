// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import btm.m.xthief.data.AppColorMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** Resolves the effective dark mode from the user preference. */
@Composable
fun rememberDarkTheme(colorMode: AppColorMode): Boolean = when (colorMode) {
    AppColorMode.SYSTEM -> isSystemInDarkTheme()
    AppColorMode.LIGHT -> false
    AppColorMode.DARK -> true
}

/**
 * Applies the MIUIX theme. No Material component is ever composed: the theme only feeds
 * MIUIX's own colour scheme and text styles. The seed colour is fixed, matching the
 * single-accent look HyperChanger uses.
 */
@Composable
fun XThiefTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val controller = remember(darkTheme) {
        ThemeController(
            colorSchemeMode = if (darkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light,
            keyColor = Color(0xFF3482FF),
        )
    }
    MiuixTheme(controller = controller) {
        content()
    }
}
