package ru.samlib.client.domain.repository

import ru.samlib.client.domain.model.Work

/**
 * Результат разбора веб-выдачи. [finished] = страница окончательная (есть работы,
 * капча или явное «нет результатов»); промежуточные/загрузочные страницы — не финальные.
 */
data class WebResults(
    val works: List<Work>,
    val captcha: Boolean,
    val finished: Boolean,
)

interface SearchRepository {
    /** Поиск средствами самого samlib (`/cgi-bin/seek`), с пагинацией. */
    suspend fun search(
        query: String,
        page: Int,
    ): SamlibResult<List<Work>>

    /**
     * Разбор HTML выдачи DuckDuckGo, полученного через WebView (реальный браузер —
     * надёжно обходит бот-фингерпринт DDG). Возвращает результаты или признак капчи.
     */
    fun parseWeb(html: String): WebResults

    /** URL страницы веб-поиска для WebView. */
    fun webSearchUrl(query: String): String
}
