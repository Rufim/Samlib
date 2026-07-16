package ru.samlib.client.domain.usecase

import ru.samlib.client.domain.repository.LocalFileRepository

/**
 * Политика выбора локального файла книги через SAF: список MIME-типов и закрепление
 * долговременного доступа на чтение. Сам ActivityResult-launcher остаётся в UI.
 */
class PickLocalFileUseCase(
    private val localFileRepo: LocalFileRepository,
) {
    /** MIME-типы, показываемые в системном пикере (книги + запасной «любой файл»). */
    val mimeTypes: Array<String> =
        arrayOf(
            "text/plain",
            "text/html",
            "application/epub+zip",
            "application/x-fictionbook+xml",
            "application/zip",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/octet-stream",
            "*/*",
        )

    /** Закрепить доступ на чтение, чтобы файл переживал перезапуск приложения. */
    fun persistReadAccess(uri: String) = localFileRepo.persistReadAccess(uri)
}
