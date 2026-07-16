package ru.samlib.client.data.local

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import ru.samlib.client.domain.repository.ReaderFileGateway
import java.io.File

/** [ReaderFileGateway] поверх файловой системы Android: Downloads/кеш + системный выбор приложения. */
class DefaultReaderFileGateway(
    private val context: Context,
) : ReaderFileGateway {
    // Маркеры контента samlib — чтобы при повторном открытии файл распознавался как книга
    // и извлекалось именно тело произведения (см. WorkParser.extractSamlibContent).
    private fun wrapHtml(
        title: String,
        body: String,
    ): String =
        "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>$title</title></head>" +
            "<body><!-- Собственно произведение -->$body" +
            "<!-- Блок описания произведения (слева внизу) --></body></html>"

    private fun fileName(title: String?): String {
        val base =
            (title ?: "samlib")
                .replace(Regex("[^\\p{L}\\p{N}\\-_ ]"), "")
                .trim()
                .ifBlank { "samlib" }
        return "$base.html"
    }

    /** Сохраняет в Downloads. Возвращает имя файла. Блокирующая — вызывать на IO. */
    override fun saveToDownloads(
        title: String?,
        body: String,
    ): String {
        val name = fileName(title)
        val html = wrapHtml(title.orEmpty(), body)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values =
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "text/html")
                }
            val resolver = context.contentResolver
            val uri =
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Не удалось создать файл")
            resolver.openOutputStream(uri)?.use { it.write(html.toByteArray(Charsets.UTF_8)) }
                ?: error("Не удалось записать файл")
        } else {
            val dir =
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: error("Нет доступа к хранилищу")
            File(dir, name).writeText(html, Charsets.UTF_8)
        }
        return name
    }

    /** MIME по расширению имени файла (для чужого приложения-читалки). */
    internal fun mimeOf(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(LocalDocumentFormat.EXT_FB2_ZIP) -> "application/zip"
            lower.endsWith(LocalDocumentFormat.EXT_FB2) -> "application/xml"
            lower.endsWith(LocalDocumentFormat.EXT_TXT) -> "text/plain"
            lower.endsWith(LocalDocumentFormat.EXT_HTML) || lower.endsWith(LocalDocumentFormat.EXT_HTM) -> "text/html"
            lower.endsWith(LocalDocumentFormat.EXT_EPUB) -> "application/epub+zip"
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(LocalDocumentFormat.EXT_DOCX) ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            lower.endsWith(LocalDocumentFormat.EXT_DOC) -> "application/msword"
            lower.endsWith(".rtf") -> "application/rtf"
            else -> "*/*"
        }
    }

    /**
     * Имя файла для внешнего приложения. Расширение берётся из уже распознанного формата
     * книги ([format] — "fb2"/"epub"/… — то, что реально сумел разобрать наш парсер), а не
     * из метаданных исходного URI: у файлов, пришедших через «Поделиться» из чужого
     * приложения, DISPLAY_NAME провайдера часто не содержит расширения, а заголовок книги
     * (из тегов fb2/epub) — никогда. Без верного расширения строгие читалки (AlReader и т.п.)
     * не распознают формат и отказываются открывать файл, хотя байты корректны.
     * [format] неизвестен — fallback на расширение исходного файла на диске.
     * Заголовок книги (из тегов fb2/epub) может содержать «:» и другие символы, недопустимые
     * в имени файла на эмулированном хранилище — некоторые читалки (AlReaderX) материализуют
     * полученный content:// файл на диск под этим именем и падают с EPERM, если не вырезать их.
     */
    internal fun resolveSharedFileName(
        sourceUri: String,
        displayName: String?,
        format: String? = null,
    ): String {
        val sourceName = Uri.parse(sourceUri).lastPathSegment?.substringAfterLast('/') ?: "book"
        val ext = format?.trim()?.ifBlank { null } ?: sourceName.substringAfterLast('.', "")
        val base =
            displayName
                ?.trim()
                ?.replace(Regex("[^\\p{L}\\p{N}\\-_ ]"), "")
                ?.trim()
                ?.ifBlank { null }
                ?: sourceName.substringBeforeLast('.', sourceName)
        return if (ext.isBlank()) base else "$base.$ext"
    }

    /**
     * Открывает локальный файл (по его URI) во внешнем приложении, сохраняя оригинальный формат.
     * Копируем байты в кеш `shared` (уже замаплен в FileProvider), т.к. напрямую отдать
     * file:// из filesDir нельзя (FileUriExposedException), а content:// без прав — не у всех.
     * Блокирующая (I/O) — вызывать на IO.
     */
    override fun openFileExternally(
        sourceUri: String,
        displayName: String?,
        format: String?,
    ) {
        val src = Uri.parse(sourceUri)
        val bytes =
            context.contentResolver.openInputStream(src)?.use { it.readBytes() }
                ?: error("Не удалось прочитать файл")
        val name = resolveSharedFileName(sourceUri, displayName, format)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name)
        file.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val view =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeOf(name))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        val chooser =
            Intent.createChooser(view, "Открыть в…").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(chooser)
    }

    /** Пишет во временный файл и открывает системным выбором приложения. */
    override fun openExternally(
        title: String?,
        body: String,
    ) {
        val name = fileName(title)
        val html = wrapHtml(title.orEmpty(), body)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, name)
        file.writeText(html, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val view =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        val chooser =
            Intent.createChooser(view, "Открыть в…").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(chooser)
    }
}
