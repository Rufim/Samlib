package ru.samlib.client.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.samlib.client.domain.model.Work

interface WorkRepository {
    /**
     * Загружает произведение из сети и кеширует содержимое в файл;
     * при ошибке сети отдаёт кешированную версию для офлайн-чтения (если есть).
     */
    suspend fun getWork(link: String): SamlibResult<Work>

    suspend fun clearWorkCache()

    /** Есть ли офлайн-кеш книги (файл существует). */
    suspend fun hasCached(link: String): Boolean

    /** Сырое содержимое книги из файлового кеша (для «Сохранить»/«Открыть в другой читалке»). */
    suspend fun cachedRawContent(link: String): String?

    suspend fun addToHistory(work: Work)

    /** Заносит локальную книгу (открытый файл) в ту же историю, что и книги samlib. */
    suspend fun addLocalToHistory(
        uri: String,
        title: String?,
        author: String?,
    )

    fun observeHistory(): Flow<List<Work>>

    suspend fun clearHistory()

    suspend fun deleteHistory(link: String)

    suspend fun restoreHistory(work: Work)

    suspend fun setHistoryPinned(
        link: String,
        pinned: Boolean,
    )

    suspend fun isHistoryPinned(link: String): Boolean

    /** Запоминает позицию в памяти (дёшево, вызывается на каждый скролл/блок озвучки). */
    suspend fun saveReadingPosition(
        workLink: String,
        blockIndex: Int,
    )

    /** Сначала — из памяти (см. [saveReadingPosition]), если для книги ничего нет — из БД. */
    suspend fun getReadingPosition(workLink: String): Int

    /** Переносит запомненную в памяти позицию книги в БД (см. [saveReadingPosition]). */
    suspend fun flushReadingPosition(workLink: String)
}
