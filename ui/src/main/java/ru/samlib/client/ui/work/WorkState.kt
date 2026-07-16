package ru.samlib.client.ui.work

import androidx.compose.runtime.Immutable

@Immutable
data class WorkState(
    val loading: Boolean = false,
    val title: String? = null,
    /** Формат локальной книги (fb2/epub/txt/…) — для «Открыть в другой читалке»; для книг samlib не используется. */
    val format: String? = null,
    /**
     * Актуальный идентификатор книги для сохранения позиции чтения/озвучки (передаётся в
     * [ReaderScaffold] как workLink). Для книг samlib совпадает с исходной ссылкой; для локальных
     * файлов — это effectiveUri ПОСЛЕ ensureLocalCopy (content:// может замениться на file://-копию),
     * поэтому сам composable-параметр uri для этого не годится и должен браться из состояния.
     */
    val resolvedLink: String? = null,
    val blocks: List<UiContentBlock> = emptyList(),
    val pinned: Boolean = false,
    /** Книга отслеживается (чекбокс в меню читалки). */
    val observed: Boolean = false,
    val startIndex: Int = 0,
    val settings: UiReaderSettings = UiReaderSettings(),
    /** Показан перевод (блоки заменены переведёнными). */
    val translated: Boolean = false,
    /** Перевод в процессе: сколько блоков готово из скольких. */
    val translating: Boolean = false,
    val translateDone: Int = 0,
    val translateTotal: Int = 0,
    /** Определённый язык оригинала (BCP-47), когда перевод включён. */
    val sourceLang: String? = null,
    val error: String? = null,
)

sealed interface WorkSideEffect {
    /** Готовое сообщение пользователю (тост) — например, текст ошибки перевода. */
    data class Message(
        val text: String,
    ) : WorkSideEffect

    /** Книга сохранена в «Загрузки» под именем [fileName]. */
    data class Saved(
        val fileName: String,
    ) : WorkSideEffect

    /** Не удалось сохранить книгу. */
    data object SaveFailed : WorkSideEffect

    /** Не удалось открыть книгу во внешней читалке. */
    data object OpenFailed : WorkSideEffect

    /** Скачивание FB2 поставлено в очередь. */
    data object Fb2Downloading : WorkSideEffect

    /** Не удалось поставить скачивание FB2. */
    data object Fb2Failed : WorkSideEffect
}
