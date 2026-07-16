package ru.samlib.client.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.samlib.client.domain.repository.BlockTextExtractor
import ru.samlib.client.domain.repository.LocalFileRepository
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.WorkContentParser
import ru.samlib.client.domain.repository.WorkRepository

/**
 * Источник текста книги для полнотекстового поиска: сетевой (кеш [WorkRepository] + разбор
 * [WorkContentParser]) либо локальный файл ([LocalFileRepository]) — и подготовка поискового
 * индекса (plain-текст каждого блока + его lowercase-версия).
 */
class LoadBookTextUseCase(
    private val workRepo: WorkRepository,
    private val contentParser: WorkContentParser,
    private val localRepo: LocalFileRepository,
    private val textExtractor: BlockTextExtractor,
) {
    data class Params(
        val link: String?,
        val uri: String?,
    )

    data class Result(
        val texts: List<String>,
        val textsLower: List<String>,
    )

    suspend operator fun invoke(params: Params): Result =
        withContext(Dispatchers.Default) {
            val blocks =
                runCatching {
                    when {
                        params.link != null ->
                            when (val r = workRepo.getWork(params.link)) {
                                is SamlibResult.Success -> contentParser.parse(r.data.rawContent)
                                is SamlibResult.Error -> emptyList()
                            }

                        params.uri != null -> localRepo.load(params.uri).blocks
                        else -> emptyList()
                    }
                }.getOrDefault(emptyList())
            val texts = blocks.map { textExtractor.plainText(it).orEmpty() }
            Result(texts = texts, textsLower = texts.map { it.lowercase() })
        }
}
