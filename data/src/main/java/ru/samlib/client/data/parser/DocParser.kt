package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import ru.samlib.client.domain.model.ContentBlock
import java.util.zip.ZipInputStream

/**
 * Разбор документов Word.
 * - .docx — надёжно: zip → word/document.xml, абзацы `<w:p>` из текстовых прогонов `<w:t>`.
 * - .doc  — старый бинарный OLE2-формат: без Apache POI извлекаем текст эвристикой
 *   (декодируем и оставляем читаемые фрагменты). Результат приблизительный.
 */
class DocParser {
    data class DocDocument(
        val title: String?,
        val blocks: List<ContentBlock>,
    )

    /** Разбор .docx (Office Open XML). */
    fun parseDocx(bytes: ByteArray): DocDocument {
        val entries = readZip(bytes)
        val docXml =
            entries["word/document.xml"]?.toString(Charsets.UTF_8)
                ?: throw ParseException("DOCX: нет word/document.xml")
        val doc = Jsoup.parse(docXml, "", Parser.xmlParser())

        val blocks = mutableListOf<ContentBlock>()
        for (p in doc.getElementsByTag("w:p")) {
            val sb = StringBuilder()
            for (node in p.getAllElements()) {
                when (node.tagName()) {
                    // wholeText: в OOXML пробелы на краях w:t значимы (xml:space=preserve)
                    "w:t" -> sb.append(node.wholeText())
                    "w:tab" -> sb.append('\t')
                    "w:br", "w:cr" -> sb.append('\n')
                }
            }
            val text = sb.toString().trim()
            if (text.isNotEmpty()) blocks.add(ContentBlock.Paragraph(escape(text)))
        }
        // Заголовок из core.xml (свойства документа), иначе — первый абзац.
        val title =
            entries["docProps/core.xml"]?.toString(Charsets.UTF_8)?.let { core ->
                Jsoup
                    .parse(core, "", Parser.xmlParser())
                    .selectFirst("dc|title, title")
                    ?.text()
                    ?.trim()
                    ?.ifBlank { null }
            }
        return DocDocument(title, blocks)
    }

    /** Эвристический разбор .doc (OLE2): извлекаем читаемые текстовые фрагменты. */
    fun parseDoc(bytes: ByteArray): DocDocument {
        // Основной текст .doc хранится в кодировке cp1251/latin, вперемешку со служебными
        // байтами. Берём последовательности печатных символов достаточной длины.
        val cp1251 = charset("windows-1251")
        val decoded = String(bytes, cp1251)
        val runs =
            Regex("[\\p{L}\\p{N}\\p{P}\\p{Zs}]{20,}")
                .findAll(decoded)
                .map { it.value.replace(Regex("[\\u0000-\\u001F]+"), " ").trim() }
                .filter { it.length >= 20 && it.count { c -> c.isLetter() } * 2 >= it.length }
                .toList()
        val blocks = runs.map { ContentBlock.Paragraph(escape(it)) }
        return DocDocument(null, blocks)
    }

    private fun escape(s: String): String =
        s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun readZip(bytes: ByteArray): Map<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) out[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }
        return out
    }
}
