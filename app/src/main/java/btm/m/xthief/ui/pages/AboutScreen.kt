// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.pages

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import btm.m.xthief.ui.AboutMaterialBackground
import btm.m.xthief.ui.AppPage
import btm.m.xthief.ui.DetailPage
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Standalone About page, matching HyperChanger: its own page with the animated
 * advanced-material background behind the scrolling grouped cards. It reuses [AppPage] so the
 * title bar keeps the same status-bar insets and glass behaviour as every other page.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenPage: (DetailPage) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    AppPage(
        title = "关于",
        onBack = onBack,
        contentBottomPadding = 40,
        // HyperChanger's About page hides the large title: the brand header carries the
        // identity while expanded, and "关于" appears once the bar collapses.
        showLargeTitle = false,
        background = { AboutMaterialBackground(Modifier.fillMaxWidth().fillMaxHeight()) },
        content = { aboutContent(onOpenUrl, onOpenPage) },
    )
}

/** Simple text page (LICENSE / open-source notices) sharing the same page shell. */
@Composable
fun LicenseScreen(
    title: String,
    body: String,
    onBack: () -> Unit,
) {
    AppPage(
        title = title,
        onBack = onBack,
        contentBottomPadding = 40,
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.5.dp,
                insideMargin = PaddingValues(16.dp),
            ) {
                Text(body, style = MiuixTheme.textStyles.body2)
            }
        }
    }
}
