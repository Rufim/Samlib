package ru.samlib.client.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.model.SearchEngine
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.SearchRepository
import ru.samlib.client.domain.repository.errorText

class SearchViewModel(
    private val repo: SearchRepository,
) : ViewModel(),
    ContainerHost<SearchState, SearchSideEffect> {
    override val container = viewModelScope.container<SearchState, SearchSideEffect>(SearchState())

    fun onQueryChange(q: String) =
        intent {
            reduce { state.copy(query = q, searched = false, captcha = false) }
        }

    /**
     * HTML выдачи DuckDuckGo из WebView (реальный браузер). Парсим в список; если это
     * страница-капча — оставляем WebView видимым для прохождения, иначе показываем список.
     */
    fun onWebHtml(html: String) =
        intent {
            if (state.engine != SearchEngine.WEB || !state.webActive) return@intent
            val r = repo.parseWeb(html)
            // применяем только финальную страницу (результаты/капча/«нет результатов»);
            // промежуточные загрузочные страницы игнорируем → нет моргания
            if (!r.finished) return@intent
            // WebView остаётся смонтированным (скрыт 1dp), виден только при капче — без ремоунта
            reduce {
                state.copy(
                    loading = false,
                    searched = true,
                    items = r.works,
                    captcha = r.captcha,
                )
            }
        }

    fun setEngine(engine: SearchEngine) =
        intent {
            if (engine == state.engine) return@intent
            // Только меняем движок; поиск — по кнопке/Enter. Уходя с веб — размонтируем WebView.
            reduce {
                state.copy(
                    engine = engine,
                    webActive = engine == SearchEngine.WEB && state.webActive,
                )
            }
        }

    fun search() =
        intent {
            val query = state.query.trim()
            if (query.isEmpty()) return@intent
            // «В браузере» — сразу открыть внешний браузер, без внутренней выдачи.
            if (state.engine == SearchEngine.BROWSER) {
                postSideEffect(SearchSideEffect.OpenInBrowser("site:samlib.ru $query"))
                return@intent
            }
            when (state.engine) {
                SearchEngine.SAMLIB -> {
                    reduce {
                        state.copy(
                            loading = true,
                            error = null,
                            page = 1,
                            captcha = false,
                            webActive = false,
                        )
                    }
                    when (val result = repo.search(query, 1)) {
                        is SamlibResult.Success ->
                            reduce {
                                state.copy(
                                    loading = false,
                                    items = result.data,
                                    canLoadMore = result.data.isNotEmpty(),
                                    searched = true,
                                )
                            }

                        is SamlibResult.Error ->
                            reduce {
                                state.copy(
                                    loading = false,
                                    error = result.type.errorText(),
                                    searched = true,
                                )
                            }
                    }
                }
                // Веб: грузим выдачу в WebView (реальный браузер), результат придёт в onWebHtml.
                SearchEngine.WEB ->
                    reduce {
                        state.copy(
                            loading = true,
                            error = null,
                            items = emptyList(),
                            canLoadMore = false,
                            searched = false,
                            captcha = false,
                            webActive = true,
                            webUrl = repo.webSearchUrl(query),
                        )
                    }

                SearchEngine.BROWSER -> Unit // обработано выше
            }
        }

    fun loadMore() =
        intent {
            val query = state.query.trim()
            if (query.isEmpty() || state.loading || !state.canLoadMore) return@intent
            val next = state.page + 1
            reduce { state.copy(loading = true, error = null) }
            when (val result = repo.search(query, next)) {
                is SamlibResult.Success ->
                    reduce {
                        state.copy(
                            loading = false,
                            items = state.items + result.data,
                            page = next,
                            canLoadMore = result.data.isNotEmpty(),
                        )
                    }

                is SamlibResult.Error ->
                    reduce {
                        state.copy(loading = false, error = result.type.errorText())
                    }
            }
        }
}
