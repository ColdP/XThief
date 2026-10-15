// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale
import kotlin.math.roundToInt

/**
 * MIUIX slider plus HyperChanger's tap-to-type dialog: tapping the row opens a glass
 * dialog where the exact value can be entered, with a "restore default" action.
 * Transcribed from HyperChanger's `EditableSliderPreference`.
 */
@Composable
fun EditableSliderPreference(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    title: String,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    defaultValue: Float = valueRange.start,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var showValueDialog by remember { mutableStateOf(false) }
    SliderPreference(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        title = title,
        valueText = valueText,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        modifier = modifier,
        onClick = if (enabled) ({ showValueDialog = true }) else null,
    )
    if (showValueDialog) {
        SliderValueDialog(
            sliderTitle = title,
            value = value,
            defaultValue = defaultValue,
            valueRange = valueRange,
            steps = steps,
            onDismiss = { showValueDialog = false },
            onSave = { adjustedValue ->
                onValueChange(adjustedValue)
                onValueChangeFinished()
                showValueDialog = false
            },
        )
    }
}

@Composable
private fun SliderValueDialog(
    sliderTitle: String,
    value: Float,
    defaultValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit,
) {
    var draft by remember(value) { mutableStateOf(formatSliderInput(value)) }
    val enteredValue = draft.trim().toFloatOrNull()
    val canSave = enteredValue != null &&
        enteredValue.isFinite() &&
        enteredValue >= valueRange.start &&
        enteredValue <= valueRange.endInclusive

    WindowDialog(
        show = true,
        onDismissRequest = onDismiss,
        imeBottomSafetyFraction = .175f,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "数值调整",
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
            )
            Text(sliderTitle, style = MiuixTheme.textStyles.body2)
            TextField(
                value = draft,
                onValueChange = { draft = it },
                label = sliderTitle,
                useLabelAsPlaceholder = true,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        enteredValue?.let {
                            onSave(normalizeSliderValue(it, valueRange, steps))
                            onDismiss()
                        }
                    },
                ),
                cornerRadius = 999.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            GlassDialogButton(
                onClick = {
                    draft = formatSliderInput(
                        defaultValue.coerceIn(valueRange.start, valueRange.endInclusive),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("恢复默认数值") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassDialogButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                GlassDialogButton(
                    onClick = {
                        enteredValue?.let { onSave(normalizeSliderValue(it, valueRange, steps)) }
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) { Text("保存") }
            }
        }
    }
}

private fun normalizeSliderValue(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
): Float {
    if (steps <= 0) return value.coerceIn(valueRange.start, valueRange.endInclusive)
    val interval = (valueRange.endInclusive - valueRange.start) / (steps + 1)
    if (interval <= 0f) return valueRange.start
    return (valueRange.start + ((value - valueRange.start) / interval).roundToInt() * interval)
        .coerceIn(valueRange.start, valueRange.endInclusive)
}

private fun formatSliderInput(value: Float): String {
    val rounded = (value * 100_000f).roundToInt() / 100_000f
    return if (rounded == rounded.roundToInt().toFloat()) {
        rounded.roundToInt().toString()
    } else {
        "%.5f".format(Locale.US, rounded).trimEnd('0').trimEnd('.')
    }
}
