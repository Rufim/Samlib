package ru.samlib.client.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.repository.AuthorRepository
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.WorkContentParser
import ru.samlib.client.domain.repository.WorkRepository

/**
 * Открытие книги: загрузка (сеть/кеш), парсинг в блоки (на Default — DOM всей книги),
 * запись в историю, снятие пометки «обновлено», выбор стартовой позиции.
 */
class LoadWorkUseCase(
    private val workRepo: WorkRepository,
    private val authorRepo: AuthorRepository,
    private val contentParser: WorkContentParser,
) {
    data class Result(
        val title: String?,
        val blocks: List<ContentBlock>,
        val startIndex: Int,
        val pinned: Boolean,
    )

    /** [scrollTo] >= 0 — переход из поиска по книге, приоритетнее сохранённой позиции. */
    suspend operator fun invoke(
        link: String,
        scrollTo: Int,
    ): SamlibResult<Result> =
        when (val r = workRepo.getWork(link)) {
            is SamlibResult.Success -> {
                val work = r.data
                val blocks =
                    withContext(Dispatchers.Default) { contentParser.parse(work.rawContent) }
                workRepo.addToHistory(work)
                // открытие снимает пометку «обновлено» у книги (и бейдж автора, если книг не осталось)
                authorRepo.markWorkOpened(link)
                val start = if (scrollTo >= 0) scrollTo else workRepo.getReadingPosition(link)
                SamlibResult.Success(
                    Result(
                        title = work.title,
                        blocks = blocks,
                        startIndex = start.coerceIn(0, (blocks.size - 1).coerceAtLeast(0)),
                        pinned = workRepo.isHistoryPinned(link),
                    ),
                )
            }

            is SamlibResult.Error -> r
        }
}
