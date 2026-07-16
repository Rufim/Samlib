package ru.samlib.client.domain.repository

/** Постановка файла в очередь системного менеджера загрузок. */
interface FileDownloadGateway {
    /** Ставит [url] в очередь загрузки с именем [displayName] (видно в уведомлении/Загрузках). */
    fun enqueue(
        url: String,
        displayName: String,
    )
}
