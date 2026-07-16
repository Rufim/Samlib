package ru.samlib.client.domain.usecase

import ru.samlib.client.domain.repository.DownloadsRepository
import ru.samlib.client.domain.repository.SavedBooksRepository

/**
 * Удаление элемента «Сохранённых»: удаляет из обоих источников ([SavedBooksRepository] и
 * [DownloadsRepository]) по одному и тому же uri, чтобы не осталась «осиротевшая» запись
 * в одном из них, если элемент на самом деле относится только к одному источнику.
 */
class DeleteSavedItemUseCase(
    private val downloadsRepo: DownloadsRepository,
    private val savedBooks: SavedBooksRepository,
) {
    suspend operator fun invoke(uri: String) {
        savedBooks.delete(uri)
        downloadsRepo.delete(uri)
    }
}
