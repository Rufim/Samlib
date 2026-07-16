package ru.samlib.client.ui.work

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import ru.samlib.client.ui.R

/** Действия меню-«троеточия» читалки — единый канал вместо пачки nullable-колбэков. */
sealed interface ReaderMenuAction {
    data object TogglePin : ReaderMenuAction

    data object GoAuthor : ReaderMenuAction

    data object ToggleObserve : ReaderMenuAction

    data object OpenIllustrations : ReaderMenuAction

    data object DownloadFb2 : ReaderMenuAction

    data object Save : ReaderMenuAction

    data object OpenExternal : ReaderMenuAction

    data object ToggleTranslate : ReaderMenuAction

    data object StartAutoScroll : ReaderMenuAction

    data object StartSpeak : ReaderMenuAction
}

/** Прогресс/состояние перевода в шапке читалки. */
@Immutable
data class TranslateStatus(
    val translating: Boolean, // идёт перевод — показать индикатор
    val done: Int, // сколько блоков переведено
    val total: Int, // всего блоков
    val translated: Boolean, // текст уже переведён (можно вернуть оригинал)
    val targetName: String, // название языка перевода для подписи
)

/** Видимость пунктов меню-«троеточия» читалки. */
@Immutable
data class ReaderMenuVisibility(
    val search: Boolean, // поиск по книге
    val goAuthor: Boolean, // перейти к автору
    val observe: Boolean, // следить за произведением
    val illustrations: Boolean, // иллюстрации
    val downloadFb2: Boolean, // скачать FB2
    val save: Boolean, // сохранить
    val translate: Boolean, // перевод
)

/** Состояние верхней панели читалки (без колбэков). */
@Immutable
data class ReaderTopBarState(
    val title: String?, // заголовок (null → подпись по умолчанию)
    val hasChapters: Boolean, // есть оглавление (≥2 заголовка)
    val pinned: Boolean, // закреплено в истории
    val observed: Boolean, // включено слежение
    val translate: TranslateStatus,
    val menu: ReaderMenuVisibility,
)

/** Верхняя панель читалки: заголовок (тап — полный текст), поиск/оглавление/настройки, меню действий. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    state: ReaderTopBarState,
    onBack: () -> Unit,
    onTitleClick: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenChapters: () -> Unit,
    onOpenSettings: () -> Unit,
    onMenuAction: (ReaderMenuAction) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            // название в одну строку с «…»; полностью — по тапу в диалоге
            Text(
                text = state.title ?: stringResource(R.string.reader_title_fallback),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onTitleClick),
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.reader_back),
                )
            }
        },
        actions = {
            if (state.menu.search) {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.reader_search_in_book),
                    )
                }
            }
            // оглавление показываем только при реальном наличии глав (≥2 заголовка);
            // одинокий заголовок-название — это не оглавление
            if (state.hasChapters) {
                IconButton(onClick = onOpenChapters) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.reader_toc),
                    )
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.reader_settings),
                )
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.reader_more),
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (state.pinned) R.string.reader_unpin_history else R.string.reader_pin_history,
                            ),
                        )
                    },
                    onClick = {
                        showMenu = false
                        onMenuAction(ReaderMenuAction.TogglePin)
                    },
                )
                if (state.menu.goAuthor) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reader_go_author)) },
                        onClick = {
                            showMenu = false
                            onMenuAction(ReaderMenuAction.GoAuthor)
                        },
                    )
                }
                if (state.menu.observe) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reader_observe_work)) },
                        trailingIcon = {
                            Checkbox(checked = state.observed, onCheckedChange = null)
                        },
                        onClick = {
                            showMenu = false
                            onMenuAction(ReaderMenuAction.ToggleObserve)
                        },
                    )
                }
                if (state.menu.illustrations) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reader_illustrations)) },
                        onClick = {
                            showMenu = false
                            onMenuAction(ReaderMenuAction.OpenIllustrations)
                        },
                    )
                }
                if (state.menu.downloadFb2) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reader_download_fb2)) },
                        onClick = {
                            showMenu = false
                            onMenuAction(ReaderMenuAction.DownloadFb2)
                        },
                    )
                }
                if (state.menu.save) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reader_save_to_device)) },
                        onClick = {
                            showMenu = false
                            onMenuAction(ReaderMenuAction.Save)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_open_external)) },
                    onClick = {
                        showMenu = false
                        onMenuAction(ReaderMenuAction.OpenExternal)
                    },
                )
                if (state.menu.translate) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                when {
                                    state.translate.translating ->
                                        stringResource(
                                            R.string.reader_translating,
                                            state.translate.done,
                                            state.translate.total,
                                        )

                                    state.translate.translated -> stringResource(R.string.reader_show_original)
                                    // язык перевода выбирается в общих настройках
                                    else ->
                                        stringResource(
                                            R.string.reader_translate_to,
                                            state.translate.targetName,
                                        )
                                },
                            )
                        },
                        enabled = !state.translate.translating,
                        onClick = {
                            showMenu = false
                            onMenuAction(ReaderMenuAction.ToggleTranslate)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_autoscroll)) },
                    onClick = {
                        showMenu = false
                        onMenuAction(ReaderMenuAction.StartAutoScroll)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_speak)) },
                    onClick = {
                        showMenu = false
                        onMenuAction(ReaderMenuAction.StartSpeak)
                    },
                )
            }
        },
    )
}
