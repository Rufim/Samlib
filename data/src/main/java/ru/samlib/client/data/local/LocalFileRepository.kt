package ru.samlib.client.data.local

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import org.jsoup.Jsoup
import ru.samlib.client.data.parser.DocParser
import ru.samlib.client.data.parser.EpubParser
import ru.samlib.client.data.parser.Fb2Parser
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.repository.LocalDocument
import ru.samlib.client.domain.repository.LocalFileRepository
import ru.samlib.client.domain.repository.WorkContentParser
import java.io.File
import java.security.MessageDigest

/** Чистый разбор содержимого файла в блоки — без зависимости от Context (тестируемо). */
fun parseLocalDocument(
    bytes: ByteArray,
    title: String,
    isHtml: Boolean,
    contentParser: WorkContentParser,
): LocalDocument {
    val lower = title.lowercase()
    return when {
        lower.endsWith(LocalDocumentFormat.EXT_FB2_ZIP) -> {
            val d = Fb2Parser().parse(bytes)
            LocalDocument(d.title ?: title, d.blocks, d.author, LocalDocumentFormat.EXT_FB2_ZIP.removePrefix("."))
        }

        lower.endsWith(LocalDocumentFormat.EXT_FB2) -> {
            val d = Fb2Parser().parse(bytes)
            LocalDocument(d.title ?: title, d.blocks, d.author, LocalDocumentFormat.EXT_FB2.removePrefix("."))
        }

        lower.endsWith(LocalDocumentFormat.EXT_EPUB) -> {
            val d = EpubParser(contentParser).parse(bytes)
            LocalDocument(d.title ?: title, d.blocks, d.author, LocalDocumentFormat.EXT_EPUB.removePrefix("."))
        }

        lower.endsWith(LocalDocumentFormat.EXT_DOCX) -> {
            val d = DocParser().parseDocx(bytes)
            LocalDocument(d.title ?: title, d.blocks, format = LocalDocumentFormat.EXT_DOCX.removePrefix("."))
        }
        // generic .zip: внутри может лежать .fb2 (частый случай скачанных книг)
        lower.endsWith(".zip") -> {
            val d = Fb2Parser().parse(bytes)
            LocalDocument(
                d.title ?: title.removeSuffix(".zip"),
                d.blocks,
                d.author,
                LocalDocumentFormat.EXT_FB2_ZIP.removePrefix("."),
            )
        }

        lower.endsWith(LocalDocumentFormat.EXT_DOC) -> {
            val d = DocParser().parseDoc(bytes)
            LocalDocument(title, d.blocks, format = LocalDocumentFormat.EXT_DOC.removePrefix("."))
        }

        isHtml -> parseHtmlDocument(decodeBytes(bytes, true), title, contentParser)
        else -> {
            val text = decodeBytes(bytes, false)
            LocalDocument(
                title = title,
                blocks = splitTxt(text),
                format = LocalDocumentFormat.EXT_TXT.removePrefix("."),
            )
        }
    }
}

/**
 * HTML: если это сохранённая страница произведения samlib (есть маркеры контента) —
 * извлекаем тело и метаданные из <title> «Автор. Название». Иначе — режим чтения:
 * выкидываем скрипты/навигацию и оставляем читаемый текст (как reader mode в браузере).
 */
private fun parseHtmlDocument(
    text: String,
    fileName: String,
    contentParser: WorkContentParser,
): LocalDocument {
    val samlib = WorkParser.extractSamlibContent(text)
    if (samlib != null) {
        val rawTitle =
            Jsoup
                .parse(text)
                .selectFirst("title")
                ?.text()
                ?.trim()
                ?.ifBlank { null }
        val name =
            rawTitle?.let { if (it.contains(". ")) it.substringAfterLast(". ").trim() else it }
                ?: fileName
        val author =
            rawTitle
                ?.let { if (it.contains(". ")) it.substringBeforeLast(". ").trim() else null }
                ?.ifBlank { null }
        return LocalDocument(name, contentParser.parse(samlib), author, LocalDocumentFormat.EXT_HTML.removePrefix("."))
    }
    return LocalDocument(
        fileName,
        contentParser.parse(readingModeHtml(text)),
        format = LocalDocumentFormat.EXT_HTML.removePrefix("."),
    )
}

/** Режим чтения для произвольного HTML: убираем служебные блоки, берём основной контент. */
private fun readingModeHtml(html: String): String {
    val doc = Jsoup.parse(html)
    doc.select("script, style, noscript, nav, header, footer, aside, form, button, iframe").remove()
    val root = doc.selectFirst("article") ?: doc.selectFirst("main") ?: doc.body()
    return root?.html().orEmpty()
}

private fun splitTxt(text: String): List<ContentBlock> =
    text
        .split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { paragraph ->
            val escaped =
                paragraph
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\n", "<br>")
            ContentBlock.Paragraph(escaped)
        }

class DefaultLocalFileRepository(
    private val context: Context,
    private val contentParser: WorkContentParser,
) : LocalFileRepository {
    override fun hasPersistedAccess(uri: String): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri.toString() == uri && it.isReadPermission }

    override suspend fun ensureLocalCopy(uri: String): String {
        val parsed = Uri.parse(uri)
        if (parsed.scheme != ContentResolver.SCHEME_CONTENT || hasPersistedAccess(uri)) return uri
        val bytes =
            context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
                ?: return uri // не читается — пусть load() покажет обычную ошибку
        val name = displayName(parsed)
        // имя копии детерминировано содержимым: то же произведение → тот же файл и та же позиция;
        // кладём в общую «библиотеку», чтобы SavedBooksRepository не делал второй копии
        val hash =
            MessageDigest
                .getInstance("SHA-1")
                .digest(bytes)
                .joinToString("") { "%02x".format(it) }
                .take(12)
        val dir = File(context.filesDir, "library").apply { mkdirs() }
        val copy = File(dir, "${hash}_$name")
        if (!copy.exists() || copy.length() != bytes.size.toLong()) copy.writeBytes(bytes)
        return Uri.fromFile(copy).toString()
    }

    override suspend fun load(uri: String): LocalDocument {
        val parsed = Uri.parse(uri)
        val title = displayName(parsed)
        val isHtml =
            title.endsWith(LocalDocumentFormat.EXT_HTML, true) ||
                title.endsWith(LocalDocumentFormat.EXT_HTM, true) ||
                context.contentResolver.getType(parsed)?.contains("html") == true
        val bytes =
            context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
                ?: throw IllegalStateException("Не удалось открыть файл")
        return parseLocalDocument(bytes, title, isHtml, contentParser)
    }

    private fun displayName(uri: Uri): String {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) {
                    val name = c.getString(idx)
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
        return uri.lastPathSegment ?: "Файл"
    }

    override fun persistReadAccess(uri: String) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}
