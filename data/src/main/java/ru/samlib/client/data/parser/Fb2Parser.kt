package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import ru.samlib.client.domain.model.ContentBlock
import java.util.zip.ZipInputStream

/** Разбор FB2 (или .fb2.zip) в блоки читалки. Текст + картинки (base64 → data-uri). */
class Fb2Parser {
    data class Fb2Document(
        val title: String?,
        val author: String?,
        val blocks: List<ContentBlock>,
    )

    fun parse(bytes: ByteArray): Fb2Document {
        val xml = extractXml(bytes)
        val doc =
            try {
                Jsoup.parse(xml, "", Parser.xmlParser())
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать FB2", e)
            }
        val binaries =
            doc.select("binary").associate { b ->
                b.attr("id") to (
                    b.attr("content-type").ifBlank { "image/jpeg" } to
                        b
                            .text()
                            .filterNot { it.isWhitespace() }
                )
            }
        val title =
            doc
                .selectFirst("book-title")
                ?.text()
                ?.trim()
                ?.ifBlank { null }
        val author = parseAuthor(doc)
        val blocks = mutableListOf<ContentBlock>()
        doc.select("body").forEach { body -> walk(body, binaries, blocks) }
        return Fb2Document(title, author, blocks)
    }

    /** Автор из <title-info><author> (ФИО), иначе никнейм. */
    private fun parseAuthor(doc: Element): String? {
        val a = doc.selectFirst("title-info > author") ?: doc.selectFirst("author") ?: return null
        val full =
            listOf("first-name", "middle-name", "last-name")
                .mapNotNull {
                    a
                        .selectFirst(it)
                        ?.text()
                        ?.trim()
                        ?.ifBlank { null }
                }.joinToString(" ")
        return full.ifBlank { a.selectFirst("nickname")?.text()?.trim() }?.ifBlank { null }
    }

    private fun walk(
        el: Element,
        binaries: Map<String, Pair<String, String>>,
        out: MutableList<ContentBlock>,
    ) {
        for (child in el.children()) {
            when (child.tagName().lowercase()) {
                "title" ->
                    child.select("p").forEach {
                        val t = it.text().trim()
                        if (t.isNotEmpty()) out.add(ContentBlock.Heading(2, t))
                    }

                "subtitle" -> {
                    val t = child.text().trim()
                    if (t.isNotEmpty()) out.add(ContentBlock.Heading(3, t))
                }

                "p" -> {
                    val html = inlineHtml(child)
                    if (html.isNotBlank()) out.add(ContentBlock.Paragraph(html))
                }

                "empty-line" -> {}
                "image" -> {
                    val href =
                        (
                            child
                                .attr("l:href")
                                .ifBlank { child.attr("xlink:href") }
                        ).removePrefix("#")
                    binaries[href]?.let { (type, b64) ->
                        out.add(ContentBlock.ImageBlock("data:$type;base64,$b64"))
                    }
                }

                "table" -> {
                    val rows =
                        child.select("tr").mapNotNull { tr ->
                            val cells =
                                tr
                                    .children()
                                    .filter { it.tagName().lowercase() in setOf("td", "th") }
                                    .map { cell ->
                                        val html = inlineHtml(cell)
                                        if (cell
                                                .tagName()
                                                .equals("th", ignoreCase = true) &&
                                            html.isNotBlank()
                                        ) {
                                            "<b>$html</b>"
                                        } else {
                                            html
                                        }
                                    }
                            cells.ifEmpty { null }
                        }
                    if (rows.isNotEmpty()) out.add(ContentBlock.Table(rows))
                }

                "section", "epigraph", "cite", "annotation" -> walk(child, binaries, out)
                else -> if (child.children().isNotEmpty()) walk(child, binaries, out)
            }
        }
    }

    /** FB2-разметку абзаца приводим к простому HTML (emphasis→i, strong→b). */
    private fun inlineHtml(p: Element): String {
        val clone = p.clone()
        clone.select("emphasis").tagName("i")
        clone.select("strong").tagName("b")
        return clone.html().trim()
    }

    private fun extractXml(bytes: ByteArray): String {
        // ZIP-сигнатура "PK" → распаковываем первый .fb2
        if (bytes.size > 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
            ZipInputStream(bytes.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".fb2", true)) {
                        return zis.readBytes().decodeFb2()
                    }
                    entry = zis.nextEntry
                }
            }
        }
        return bytes.decodeFb2()
    }

    private fun ByteArray.decodeFb2(): String {
        val head = String(this, 0, minOf(size, 200), Charsets.ISO_8859_1).lowercase()
        val charset =
            when {
                head.contains("windows-1251") -> "windows-1251"
                head.contains("koi8-r") -> "KOI8-R"
                else -> "UTF-8"
            }
        return String(this, charset(charset))
    }
}
