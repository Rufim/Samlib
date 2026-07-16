package ru.samlib.client.ui.work

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax
import ru.samlib.client.domain.usecase.LoadBookTextUseCase

/** Одно найденное вхождение: индекс блока и контекст (до/сам фрагмент/после). */
@Immutable
data class SearchHit(
    val blockIndex: Int,
    val before: String,
    val match: String,
    val after: String,
)

/** Одноразовая команда прокрутки списка результатов — экран обязан погасить её после применения. */
@Immutable
data class ScrollCommand(
    val index: Int,
    val offset: Int = 0,
)

@Immutable
data class WorkSearchState(
    val loading: Boolean = false,
    val ready: Boolean = false,
    val query: String = "",
    val hits: List<SearchHit> = emptyList(),
    val truncated: Boolean = false,
    val error: String? = null,
    /** Всего блоков в книге — для процента позиции находки (см. [SearchHit.blockIndex]). */
    val blockCount: Int = 0,
    val scrollCommand: ScrollCommand? = null,
)

/**
 * Поиск по тексту книги. Загружает блоки (сетевые — через кеш репозитория, локальные — из файла),
 * держит их plain-текст в памяти и по каждому запросу выдаёт список вхождений с контекстом.
 * Состояние (запрос + результаты) живёт в ViewModel и переживает переход в читалку и обратно.
 *
 * Решение "куда прокрутить список результатов" принимается целиком здесь, детерминированно и
 * последовательно (внутри runSearch/при построении начального состояния) — а не реактивно в
 * Compose, где два независимых LaunchedEffect (решение о скролле и сохранение позиции списка в
 * память) могли гоняться друг за другом при быстрой смене запроса и давать нестабильный результат.
 */
class WorkSearchViewModel(
    private val link: String?,
    private val uri: String?,
    // текущая позиция чтения на момент открытия поиска (-1 — неизвестна/не нужна)
    private val fromIndex: Int,
    private val loadBookText: LoadBookTextUseCase,
    private val memory: InBookSearchMemory,
) : ViewModel(),
    ContainerHost<WorkSearchState, Nothing> {
    private val bookKey = link ?: uri ?: ""

    init {
        // Синхронно и ДО чтения полей memory ниже: переключение/сброс при смене книги.
        memory.forBook(bookKey)
    }

    // Запрос, с которым была открыта ЭТА сессия экрана (из app-памяти при восстановлении).
    // Сохранённая прокрутка результатов релевантна только пока запрос не менялся с открытия —
    // как только пользователь очистил поле и ввёл другое слово, это уже новый поиск.
    private val initialQuery = memory.query.trim()
    private var initialScrollDecided = false

    // Запрос+результаты уже могут быть в app-scoped памяти (см. InBookSearchMemory) — кладём их
    // сразу в начальное состояние, без задержки на debounce/повторный поиск.
    override val container =
        viewModelScope.container<WorkSearchState, Nothing>(
            WorkSearchState(
                ready = memory.textLoaded,
                query = memory.query,
                hits = memory.hits,
                truncated = memory.truncated,
                blockCount = memory.texts.size,
                scrollCommand = decideScrollCommand(initialQuery, memory.hits),
            ),
        )

    private var texts: List<String> = memory.texts
    private var textsLower: List<String> = memory.textsLower
    private var loaded = memory.textLoaded

    // Стартовое значение — восстановленный запрос: коллектор ниже при первом срабатывании
    // пересчитает ТЕ ЖЕ результаты (быстро, текст уже в кеше), а не молча очистит их пустой
    // строкой, с которой раньше всегда стартовал queryFlow независимо от memory.
    private val queryFlow = MutableStateFlow(memory.query)

    init {
        // Поиск — по последнему запросу после паузы ввода; collectLatest отменяет
        // сканирование по устаревшему запросу, как только пришёл новый.
        intent {
            @OptIn(FlowPreview::class)
            queryFlow
                .debounce(DEBOUNCE_MS)
                .collectLatest { q -> runSearch(q) }
        }
    }

    fun load() =
        intent {
            if (loaded) return@intent
            reduce { state.copy(loading = true, error = null) }
            val result = loadBookText(LoadBookTextUseCase.Params(link, uri))
            texts = result.texts
            textsLower = result.textsLower
            loaded = true
            memory.texts = texts
            memory.textsLower = textsLower
            memory.textLoaded = true
            reduce {
                state.copy(
                    loading = false,
                    ready = true,
                    error = if (result.texts.isEmpty()) "Не удалось загрузить текст" else null,
                    blockCount = result.texts.size,
                )
            }
            if (state.query.isNotBlank()) queryFlow.value = state.query
        }

    fun setQuery(query: String) =
        intent {
            memory.query = query
            reduce { state.copy(query = query) }
            queryFlow.value = query
        }

    /** Запомнить позицию списка результатов (для восстановления при повторном открытии). */
    fun rememberScroll(
        index: Int,
        offset: Int,
    ) {
        memory.firstVisibleIndex = index
        memory.firstVisibleOffset = offset
    }

    /** Экран вызывает сразу после применения scrollCommand — гасит одноразовую команду. */
    fun scrollCommandConsumed() = intent { reduce { state.copy(scrollCommand = null) } }

    /**
     * Куда прокрутить список для только что вычисленных [hits]:
     * - для запроса, с которым сессия открылась, один раз учитываем сохранённую прокрутку
     *   (пользователь уже листал именно эти результаты раньше);
     * - иначе (или если сохранённой прокрутки нет) — к первой находке от текущей позиции чтения.
     */
    private fun decideScrollCommand(
        query: String,
        hits: List<SearchHit>,
    ): ScrollCommand? {
        if (hits.isEmpty()) return null
        val useSavedScroll = !initialScrollDecided && query == initialQuery
        initialScrollDecided = true
        if (useSavedScroll) {
            val i = memory.firstVisibleIndex
            val off = memory.firstVisibleOffset
            if (i > 0 || off > 0) return ScrollCommand(i, off)
        }
        if (fromIndex < 0) return null
        val target = hits.indexOfFirst { it.blockIndex >= fromIndex }
        return if (target > 0) ScrollCommand(target) else null
    }

    private suspend fun Syntax<WorkSearchState, Nothing>.runSearch(raw: String) {
        val query = raw.trim()
        if (query.length < MIN_QUERY) {
            reduce { state.copy(hits = emptyList(), truncated = false, scrollCommand = null) }
            memory.hits = emptyList()
            memory.truncated = false
            return
        }
        val needle = query.lowercase()
        val hits = ArrayList<SearchHit>()
        var truncated = false
        loop@ for (i in textsLower.indices) {
            kotlin.coroutines.coroutineContext.ensureActive() // отмена между блоками при новом запросе
            val lower = textsLower[i]
            val text = texts[i]
            var from = 0
            while (true) {
                val idx = lower.indexOf(needle, from)
                if (idx < 0) break
                val end = idx + query.length
                val bStart = (idx - CONTEXT).coerceAtLeast(0)
                val aEnd = (end + CONTEXT).coerceAtMost(text.length)
                hits.add(
                    SearchHit(
                        blockIndex = i,
                        before = (if (bStart > 0) "…" else "") + text.substring(bStart, idx),
                        match = text.substring(idx, end),
                        after = text.substring(end, aEnd) + (if (aEnd < text.length) "…" else ""),
                    ),
                )
                from = end
                if (hits.size >= MAX_HITS) {
                    truncated = true
                    break@loop
                }
            }
        }
        reduce { state.copy(hits = hits, truncated = truncated, scrollCommand = decideScrollCommand(query, hits)) }
        memory.hits = hits
        memory.truncated = truncated
    }

    private companion object {
        const val MIN_QUERY = SearchDefaults.MIN_QUERY
        const val CONTEXT = SearchDefaults.CONTEXT
        const val MAX_HITS = SearchDefaults.MAX_HITS

        /** Пауза ввода перед запуском сканирования текста книги. */
        const val DEBOUNCE_MS = 250L
    }
}
