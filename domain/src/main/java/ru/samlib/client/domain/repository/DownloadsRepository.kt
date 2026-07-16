package ru.samlib.client.domain.repository

/** Сохранённый на устройство файл произведения. */
data class SavedFile(
    val uri: String,
    val name: String,
    val dateAdded: Long,
)

/**
 * Список файлов, сохранённых приложением (меню читалки «Сохранить на устройство»).
 * На Android Q+ — через MediaStore.Downloads (фильтр по владельцу-пакету),
 * на старых версиях — из приватного каталога Downloads приложения.
 */
interface DownloadsRepository {
    suspend fun list(): List<SavedFile>

    suspend fun delete(uriString: String)
}
