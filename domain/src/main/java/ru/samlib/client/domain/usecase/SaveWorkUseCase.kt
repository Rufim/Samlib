package ru.samlib.client.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.samlib.client.domain.repository.ReaderFileGateway

/** Сохранение книги samlib в «Загрузки». Возвращает имя созданного файла. */
class SaveWorkUseCase(
    private val gateway: ReaderFileGateway,
) : UseCase<SaveWorkUseCase.Params, String>() {
    data class Params(
        val title: String?,
        val body: String,
    )

    /** Блокирующая I/O явно на Dispatchers.IO — базовый класс по умолчанию грузит Default. */
    override suspend fun execute(params: Params): String =
        withContext(Dispatchers.IO) { gateway.saveToDownloads(params.title, params.body) }
}
