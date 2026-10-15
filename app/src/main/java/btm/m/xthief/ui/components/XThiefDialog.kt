// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Confirmation dialog rendered with HyperChanger's Backdrop glass dialog surface.
 * Callers keep the simple confirm/dismiss API, the look comes from [WindowDialog].
 */
@Composable
fun XThiefDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Drawn here instead of through WindowDialog's title/summary slots: those are
            // hard-coded to TextAlign.Center, and HyperChanger's dialogs read left-aligned.
            // Button labels stay centred.
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
            )
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Start,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassDialogButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) { Text(dismissText) }
                GlassDialogButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = if (destructive) {
                        ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.error,
                            contentColor = MiuixTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColorsPrimary()
                    },
                ) { Text(confirmText) }
            }
        }
    }
}
