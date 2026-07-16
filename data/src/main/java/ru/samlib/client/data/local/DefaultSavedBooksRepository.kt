package ru.samlib.client.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.samlib.client.data.db.SavedBookDao
import ru.samlib.client.data.db.SavedBookEntity
import ru.samlib.client.domain.model.SavedItem
import ru.samlib.client.domain.repository.SavedBookInfo
import ru.samlib.client.domain.repository.SavedBooksRepository
import java.io.File

class DefaultSavedBooksRepository(
    private val context: Context,
    private val dao: SavedBookDao,
) : SavedBooksRepository {
    private val libraryDir by lazy {
        File(context.filesDir, "library").apply { mkdirs() }
    }

    override suspend fun all(): List<SavedItem> =
        withContext(Dispatchers.IO) {
            dao.getAll().map { SavedItem(it.uri, it.title, it.author, it.dateAdded) }
        }

    /** Записать открытую книгу: если исходник не в библиотеке — скопировать туда. */
    override suspend fun record(info: SavedBookInfo) =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(info.sourceUri)
            val storedUri =
                if (isInLibrary(uri)) {
                    info.sourceUri
                } else {
                    copyIntoLibrary(uri, info.title, info.format) ?: return@withContext
                }
            dao.upsert(SavedBookEntity(storedUri, info.title, info.author, info.format, info.date))
        }

    override suspend fun delete(uriString: String) =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") runCatching { uri.path?.let { File(it).delete() } }
            dao.delete(uriString)
        }

    private fun isInLibrary(uri: Uri): Boolean =
        uri.scheme == "file" && uri.path?.startsWith(libraryDir.absolutePath) == true

    private fun copyIntoLibrary(
        uri: Uri,
        title: String,
        format: String?,
    ): String? =
        runCatching {
            val ext = if (format.isNullOrBlank()) "" else ".$format"
            val safe =
                title
                    .replace(Regex("""[^\p{L}\p{N}._-]+"""), "_")
                    .trim('_')
                    .take(60)
                    .ifBlank { "book" }
            var file = File(libraryDir, "$safe$ext")
            var i = 1
            while (file.exists()) {
                file = File(libraryDir, "${safe}_$i$ext")
                i++
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            Uri.fromFile(file).toString()
        }.getOrNull()
}
