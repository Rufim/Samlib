package ru.samlib.client.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.samlib.client.domain.repository.ReaderFileGateway

/**
 * Открытие книги во внешней читалке. Для книги samlib пишем её тело во временный html,
 * для локального файла — отдаём оригинал, сохраняя формат.
 */
class OpenExternallyUseCase(
    private val gateway: ReaderFileGateway,
) {
    /** Онлайн-книга samlib: тело [body] во временный файл и системный выбор приложения. */
    suspend operator fun invoke(
        title: String?,
        body: String,
    ) = withContext(Dispatchers.IO) { gateway.openExternally(title, body) }

    /** Локальный файл: открыть оригинал (по [sourceUri]) во внешней читалке. */
    suspend fun localFile(
        sourceUri: String,
        title: String?,
        format: String?,
    ) = withContext(Dispatchers.IO) {
        gateway.openFileExternally(sourceUri, title, format)
    }
}
