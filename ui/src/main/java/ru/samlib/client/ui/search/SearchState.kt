package ru.samlib.client.ui.search

import androidx.compose.runtime.Immutable
import ru.samlib.client.domain.model.SearchEngine
import ru.samlib.client.domain.model.Work

@Immutable
data class SearchState(
    val query: String = "",
    val engine: SearchEngine = SearchEngine.SAMLIB,
    val loading: Boolean = false,
    val items: List<Work> = emptyList(),
    val page: Int = 1,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    /** true после завершения поиска — чтобы отличать «ещё не искали» от «ничего не найдено». */
    val searched: Boolean = false,
    /** true, если DuckDuckGo показал капчу → раскрываем WebView для её прохождения. */
    val captcha: Boolean = false,
    /** WebView-фетч активен (грузим/парсим выдачу через браузер; виден только при капче). */
    val webActive: Boolean = false,
    /** URL веб-поиска, который грузит WebView. */
    val webUrl: String = "",
)

sealed interface SearchSideEffect {
    /** Открыть поисковый запрос в дефолтном поисковике устройства. */
    data class OpenInBrowser(
        val query: String,
    ) : SearchSideEffect
}
