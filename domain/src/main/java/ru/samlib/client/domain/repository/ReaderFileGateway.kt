package ru.samlib.client.domain.repository

/** Сохранение и открытие книги во внешних приложениях (Downloads/кеш + системный выбор). */
interface ReaderFileGateway {
    /** Сохраняет [title]/[body] как html-файл в Downloads. Возвращает имя созданного файла. */
    fun saveToDownloads(
        title: String?,
        body: String,
    ): String

    /** Пишет [title]/[body] во временный файл и открывает системным выбором приложения. */
    fun openExternally(
        title: String?,
        body: String,
    )

    /** Открывает локальный файл (по [sourceUri]) во внешнем приложении, сохраняя формат. */
    fun openFileExternally(
        sourceUri: String,
        displayName: String?,
        format: String? = null,
    )
}
