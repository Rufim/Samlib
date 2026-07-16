package ru.samlib.client.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.samlib.client.ui.R

/**
 * Оборачивает выделенный фрагмент [value] парой тегов [open]/[close]. Если выделения нет —
 * вставляет пустую пару и ставит курсор между тегами. Возвращает новое значение поля.
 */
fun wrapSelection(
    value: TextFieldValue,
    open: String,
    close: String,
): TextFieldValue {
    val sel = value.selection
    val text = value.text
    val start = sel.min
    val end = sel.max
    val selected = text.substring(start, end)
    val newText = text.substring(0, start) + open + selected + close + text.substring(end)
    // курсор после закрывающего тега, либо между тегами при пустом выделении
    val cursor =
        if (selected.isEmpty()) start + open.length else start + open.length + selected.length + close.length
    return value.copy(text = newText, selection = TextRange(cursor))
}

/** Вставляет [snippet] в позицию курсора (или заменяет выделение). */
fun insertSnippet(
    value: TextFieldValue,
    snippet: String,
): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val newText = value.text.substring(0, start) + snippet + value.text.substring(end)
    return value.copy(text = newText, selection = TextRange(start + snippet.length))
}

/** Маркеры-обёртки для форматирования выделения (напр. <li>/</li> + <ul>/</ul>). */
data class WrapMarkers(
    val prefix: String, // ставится перед каждой строкой
    val suffix: String, // ставится после каждой строки
    val wrapAll: String, // ставится перед всем блоком
    val wrapAllEnd: String, // ставится после всего блока
)

/** Оборачивает каждую строку выделения в [markers] (для списков/пунктов). */
private fun wrapLines(
    value: TextFieldValue,
    markers: WrapMarkers,
): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val chunk = value.text.substring(start, end).ifEmpty { "пункт" }
    val body = chunk.split("\n").joinToString("\n") { "${markers.prefix}$it${markers.suffix}" }
    val replacement = "${markers.wrapAll}\n$body\n${markers.wrapAllEnd}"
    val newText = value.text.substring(0, start) + replacement + value.text.substring(end)
    return value.copy(text = newText, selection = TextRange(start + replacement.length))
}

/**
 * Абстракция редактируемого текста для [FormatToolbar]. Позволяет одной панели
 * работать и с Compose-полем (`TextFieldValue`), и с нативным `EditText`.
 * Значение читается по требованию ([current]) в момент нажатия кнопки, чтобы
 * панель видела актуальное выделение даже у View, не участвующего в рекомпозиции.
 */
interface FormatTarget {
    /** Текущий текст и выделение редактора. */
    fun current(): TextFieldValue

    /** Применяет новое значение (текст + позиция курсора) к редактору. */
    fun apply(value: TextFieldValue)
}

/**
 * Панель форматирования для textarea-редакторов раздела. Вставляет HTML-теги,
 * поддерживаемые samlib: b, i, u, s, a, заголовок, маркированный список.
 *
 * Совместимая обёртка для Compose-полей: строит [FormatTarget] из текущего
 * [value] и колбэка [onChange].
 */
@Composable
fun FormatToolbar(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val target =
        object : FormatTarget {
            override fun current() = value

            override fun apply(value: TextFieldValue) = onChange(value)
        }
    FormatToolbar(target = target, modifier = modifier)
}

/** Панель форматирования поверх произвольного [target] (Compose-поле или EditText). */
@Composable
fun FormatToolbar(
    target: FormatTarget,
    modifier: Modifier = Modifier,
) {
    var showLink by remember { mutableStateOf(false) }
    var showTable by remember { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = { target.apply(wrapSelection(target.current(), "<b>", "</b>")) }) {
            Text(stringResource(R.string.format_bold_letter), fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = { target.apply(wrapSelection(target.current(), "<i>", "</i>")) }) {
            Text(stringResource(R.string.format_italic_letter), fontStyle = FontStyle.Italic)
        }
        TextButton(onClick = { target.apply(wrapSelection(target.current(), "<u>", "</u>")) }) {
            Text(
                stringResource(R.string.format_underline_letter),
                textDecoration = TextDecoration.Underline,
            )
        }
        TextButton(onClick = { target.apply(wrapSelection(target.current(), "<s>", "</s>")) }) {
            Text("Ƨ", textDecoration = TextDecoration.LineThrough)
        }
        TextButton(onClick = { target.apply(wrapSelection(target.current(), "<h3>", "</h3>")) }) {
            Text("H")
        }
        TextButton(onClick = {
            target.apply(
                wrapLines(
                    target.current(),
                    WrapMarkers("<li>", "</li>", "<ul>", "</ul>"),
                ),
            )
        }) {
            Text(stringResource(R.string.format_list))
        }
        TextButton(onClick = { showLink = true }) {
            Text(stringResource(R.string.format_link))
        }
        TextButton(onClick = { showTable = true }) {
            Text(stringResource(R.string.format_table))
        }
    }

    if (showTable) {
        var rows by remember { mutableStateOf("2") }
        var cols by remember { mutableStateOf("2") }
        AlertDialog(
            onDismissRequest = { showTable = false },
            confirmButton = {
                TextButton(onClick = {
                    val r = rows.toIntOrNull()?.coerceIn(1, 20) ?: 2
                    val c = cols.toIntOrNull()?.coerceIn(1, 10) ?: 2
                    val body =
                        (1..r).joinToString("\n") { row ->
                            "<tr>" + (1..c).joinToString("") { "<td>  </td>" } + "</tr>"
                        }
                    target.apply(
                        insertSnippet(
                            target.current(),
                            "\n<table border=1 cellpadding=4>\n$body\n</table>\n",
                        ),
                    )
                    showTable = false
                }) { Text(stringResource(R.string.common_insert)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTable = false
                }) { Text(stringResource(R.string.common_cancel)) }
            },
            title = { Text(stringResource(R.string.format_table)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = rows,
                        onValueChange = { rows = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.format_rows_label)) },
                        singleLine = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = cols,
                        onValueChange = { cols = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.format_cols_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }

    if (showLink) {
        var url by remember { mutableStateOf("http://") }
        var caption by remember {
            val v = target.current()
            mutableStateOf(v.text.substring(v.selection.min, v.selection.max))
        }
        AlertDialog(
            onDismissRequest = { showLink = false },
            confirmButton = {
                TextButton(onClick = {
                    val text = caption.ifBlank { url }
                    target.apply(insertSnippet(target.current(), "<a href=\"$url\">$text</a>"))
                    showLink = false
                }) { Text(stringResource(R.string.common_insert)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLink = false
                }) { Text(stringResource(R.string.common_cancel)) }
            },
            title = { Text(stringResource(R.string.format_link)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.format_url_label)) },
                        singleLine = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text(stringResource(R.string.format_link_text_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }
}
