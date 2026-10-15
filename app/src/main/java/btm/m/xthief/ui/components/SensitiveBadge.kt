// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Compact pill marking a download whose source post X flagged as sensitive.
 *
 * The flag is recorded per download (see `DownloadRecord.sensitive`) and stamped onto media
 * library entries by file name, so the library can later offer labelling or filtering.
 */
@Composable
fun SensitiveBadge(modifier: Modifier = Modifier) {
    Text(
        text = "敏感",
        style = MiuixTheme.textStyles.footnote2,
        color = MiuixTheme.colorScheme.error,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MiuixTheme.colorScheme.error.copy(alpha = .14f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}
