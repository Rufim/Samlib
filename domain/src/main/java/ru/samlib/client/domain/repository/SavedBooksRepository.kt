package ru.samlib.client.domain.repository

import ru.samlib.client.domain.model.SavedItem

// Реестр книг, открытых извне. Метаданные (название/автор) берутся из fb2/epub-тегов.
// Файл копируется в приватную «библиотеку» приложения, чтобы книга оставалась
// доступной для повторного открытия из «Сохранённых» после закрытия внешнего доступа.

/** Описание открытой книги для записи в «Сохранённые». */
data class SavedBookInfo(
    val sourceUri: String, // исходный URI (content:// или file://)
    val title: String, // заголовок книги
    val author: String?, // автор (может быть неизвестен)
    val format: String?, // формат: fb2/fb2.zip/epub/…
    val date: Long, // время открытия, epoch ms
)

interface SavedBooksRepository {
    suspend fun all(): List<SavedItem>

    suspend fun record(info: SavedBookInfo)

    suspend fun delete(uriString: String)
}
