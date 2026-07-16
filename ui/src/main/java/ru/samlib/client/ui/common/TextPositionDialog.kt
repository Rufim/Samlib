package ru.samlib.client.ui.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import ru.samlib.client.ui.R

/**
 * Диалог «Позиция в тексте»: переход к доле текста по проценту (0–100, до сотых).
 * [currentFraction] — текущая позиция для предзаполнения; [onGo] получает долю 0..1.
 */
@Composable
fun TextPositionDialog(
    currentFraction: Float,
    onDismiss: () -> Unit,
    onGo: (Float) -> Unit,
) {
    var input by remember { mutableStateOf("%.2f".format(currentFraction * 100)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val pct = input.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                onGo((pct / 100.0).toFloat())
            }) { Text(stringResource(R.string.common_go)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
        title = { Text(stringResource(R.string.position_dialog_title)) },
        text = {
            OutlinedTextField(
                value = input,
                // допускаем сотые доли процента: цифры и одна разделительная точка/запятая
                onValueChange = { raw ->
                    val cleaned = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
                    val dot = cleaned.indexOf('.')
                    input =
                        if (dot < 0) {
                            cleaned.take(3)
                        } else {
                            cleaned.substring(0, dot).take(3) + "." +
                                cleaned.substring(dot + 1).filter { it.isDigit() }.take(2)
                        }
                },
                label = { Text(stringResource(R.string.position_dialog_percent_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
    )
}
