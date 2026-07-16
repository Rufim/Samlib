package ru.samlib.client.domain.usecase

import ru.samlib.client.domain.model.SavedItem
import ru.samlib.client.domain.repository.DownloadsRepository
import ru.samlib.client.domain.repository.SavedBooksRepository

/**
 * Список «Сохранённых»: сливает книги из реестра ([SavedBooksRepository], с метаданными
 * автора/названия) и файлы устройства ([DownloadsRepository]), исключая из вторых те, что
 * уже учтены как книги (по uri) — иначе один и тот же файл показывался бы дважды.
 */
class LoadSavedItemsUseCase(
    private val downloadsRepo: DownloadsRepository,
    private val savedBooks: SavedBooksRepository,
) {
    suspend operator fun invoke(): List<SavedItem> {
        val books = savedBooks.all()
        val bookUris = books.mapTo(HashSet()) { it.uri }
        val files =
            downloadsRepo
                .list()
                .filterNot { it.uri in bookUris }
                .map { SavedItem(it.uri, it.name, date = it.dateAdded * 1000) }
        return (books + files).sortedByDescending { it.date }
    }
}
