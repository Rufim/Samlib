package ru.samlib.client.domain.usecase

import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.repository.AuthorRepository

/**
 * Импорт подписок из текстового списка (обратное к «экспорту» — строки вида
 * `link[\tИмя]`). Строки без ссылки (не начинающиеся с `/`) молча пропускаются —
 * формат best-effort, как и у [ru.samlib.client.data.local.LegacyImporter].
 */
class ImportFollowedAuthorsUseCase(
    private val authorRepo: AuthorRepository,
) {
    suspend operator fun invoke(text: String) {
        text
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val parts = line.split('\t')
                val link = parts[0].trim()
                if (link.startsWith("/")) {
                    authorRepo.follow(
                        Author(
                            link = link,
                            fullName = parts.getOrNull(1)?.trim()?.ifBlank { null },
                        ),
                    )
                }
            }
    }
}
