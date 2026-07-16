package ru.samlib.client.data.log

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * Дозапись строк лога в публичный Downloads/Samlib/samlib-log.txt.
 * API 29+ — через MediaStore (без разрешений), API 26–28 — прямой файл.
 * Весь IO обёрнут в runCatching: сбой записи никогда не пробрасывается наверх.
 * При переполнении MAX_BYTES файл обнуляется (без бэкапа).
 */
class LogFileWriter(
    private val appContext: Context,
) {
    private companion object {
        const val DIR = "Samlib"
        const val NAME = "samlib-log.txt"
        val REL_PATH = "${Environment.DIRECTORY_DOWNLOADS}/$DIR"
    }

    private var sizeBytes: Long = -1
    private var cachedUri: Uri? = null

    @Synchronized
    fun append(line: String) {
        runCatching {
            val bytes = line.toByteArray(Charsets.UTF_8)
            // Файл могли удалить (файл-менеджер, очистка Download) — кэш URI протухает,
            // запись молча теряется. Проверяем существование и при пропаже начинаем заново.
            if (cachedTargetMissing()) {
                cachedUri = null
                sizeBytes = -1
            }
            if (sizeBytes < 0) sizeBytes = currentSize()
            if (FileLogFormatter.shouldRotate(sizeBytes, bytes.size)) {
                truncate()
                sizeBytes = 0
            }
            writeAppend(bytes)
            sizeBytes += bytes.size
        }
    }

    /** true, если ранее уже знали целевой файл, но он исчез (удалён извне). */
    private fun cachedTargetMissing(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = cachedUri ?: return false
            runCatching {
                appContext.contentResolver.openFileDescriptor(uri, "r")?.use { false } ?: true
            }.getOrDefault(true)
        } else {
            sizeBytes >= 0 && !legacyFile().exists()
        }

    private fun currentSize(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) mediaStoreSize() else legacyFile().length()

    private fun writeAppend(bytes: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = ensureMediaStoreUri() ?: return
            val os = appContext.contentResolver.openOutputStream(uri, "wa")
            if (os == null) {
                cachedUri = null // поток не открылся — URI протух, пересоздадим на следующей записи
                return
            }
            os.use { it.write(bytes) }
        } else {
            val f = legacyFile()
            f.parentFile?.mkdirs()
            FileOutputStream(f, true).use { it.write(bytes) }
        }
    }

    private fun truncate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = ensureMediaStoreUri() ?: return
            appContext.contentResolver.openOutputStream(uri, "wt")?.use { /* обнуляем */ }
        } else {
            val f = legacyFile()
            f.parentFile?.mkdirs()
            FileOutputStream(f, false).use { /* обнуляем */ }
        }
    }

    private fun mediaStoreSize(): Long {
        val uri = ensureMediaStoreUri() ?: return 0
        return runCatching {
            appContext.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0
        }.getOrDefault(0)
    }

    private fun ensureMediaStoreUri(): Uri? {
        cachedUri?.let { return it }
        val resolver = appContext.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val selection =
            "${MediaStore.Downloads.RELATIVE_PATH}=? AND ${MediaStore.Downloads.DISPLAY_NAME}=?"
        val args = arrayOf("$REL_PATH/", NAME)
        resolver
            .query(collection, arrayOf(MediaStore.Downloads._ID), selection, args, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    cachedUri = ContentUris.withAppendedId(collection, c.getLong(0))
                    return cachedUri
                }
            }
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, NAME)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, REL_PATH)
            }
        cachedUri = resolver.insert(collection, values)
        return cachedUri
    }

    private fun legacyFile(): File =
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "$DIR/$NAME",
        )
}
