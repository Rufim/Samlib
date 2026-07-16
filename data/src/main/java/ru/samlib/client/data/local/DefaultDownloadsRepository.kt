package ru.samlib.client.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.samlib.client.domain.repository.DownloadsRepository
import ru.samlib.client.domain.repository.SavedFile
import java.io.File

class DefaultDownloadsRepository(
    private val context: Context,
) : DownloadsRepository {
    override suspend fun list(): List<SavedFile> =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listFromMediaStore() else listFromDir()
        }

    override suspend fun delete(uriString: String) =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            runCatching {
                if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    context.contentResolver.delete(uri, null, null)
                } else {
                    uri.path?.let { File(it).delete() }
                }
            }
            Unit
        }

    private fun listFromMediaStore(): List<SavedFile> {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection =
            arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.DATE_ADDED,
            )
        val selection = "${MediaStore.MediaColumns.OWNER_PACKAGE_NAME} = ?"
        val args = arrayOf(context.packageName)
        val sort = "${MediaStore.Downloads.DATE_ADDED} DESC"
        val result = mutableListOf<SavedFile>()
        context.contentResolver.query(collection, projection, selection, args, sort)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                result.add(
                    SavedFile(
                        uri.toString(),
                        c.getString(nameCol) ?: "файл",
                        c.getLong(dateCol),
                    ),
                )
            }
        }
        return result
    }

    private fun listFromDir(): List<SavedFile> {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return emptyList()
        return dir
            .listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { SavedFile(Uri.fromFile(it).toString(), it.name, it.lastModified() / 1000) }
            ?: emptyList()
    }
}
