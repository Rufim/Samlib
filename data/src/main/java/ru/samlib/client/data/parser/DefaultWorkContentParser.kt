package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.repository.WorkContentParser
import ru.samlib.client.domain.samlib.SamlibUrls

class DefaultWorkContentParser : WorkContentParser {
    private val inlineTags =
        setOf(
            "b",
            "strong",
            "i",
            "em",
            "u",
            "ins",
            "s",
            "strike",
            "del",
            "font",
            "a",
            "span",
            "small",
            "big",
            "sub",
            "sup",
            "tt",
            "code",
        )

    override fun parse(rawHtml: String): List<ContentBlock> {
        val doc = Jsoup.parse(rawHtml)
        val blocks = mutableListOf<ContentBlock>()
        val buffer = StringBuilder()

        fun flush() {
            val html = buffer.toString().trim()
            if (html.isNotBlank()) blocks.add(ContentBlock.Paragraph(html))
            buffer.setLength(0)
        }

        fun walk(
            node: Node,
            pre: Boolean,
        ) {
            for (child in node.childNodes()) {
                when (child) {
                    is TextNode ->
                        if (pre) {
                            // в <pre> значимы и переносы строк, и пробелы между тегами →
                            // не фильтруем пустые узлы, переносы превращаем в <br>
                            val t = child.wholeText
                            if (t.isNotEmpty()) buffer.append(escape(t).replace("\n", "<br>"))
                        } else {
                            if (child.text().isNotBlank()) buffer.append(child.text())
                        }

                    is Element ->
                        when (child.tagName().lowercase()) {
                            // samlib оборачивает всё тело в <pre>; браузер всё равно применяет
                            // теги внутри. Если внутри есть разметка — разбираем как обычный
                            // контент (сохраняя переносы), иначе оставляем моноширинным (стихи).
                            "pre" -> {
                                flush()
                                if (child.children().any { it.tagName().lowercase() != "br" }) {
                                    walk(child, pre = true)
                                    flush()
                                } else {
                                    blocks.add(ContentBlock.Preformatted(child.wholeText().trim()))
                                }
                            }

                            "h1", "h2", "h3" -> {
                                flush()
                                val level = if (child.tagName().lowercase() == "h3") 3 else 2
                                val text = child.text().trim()
                                if (text.isNotEmpty()) blocks.add(ContentBlock.Heading(level, text))
                            }

                            "hr" -> {
                                flush()
                                blocks.add(ContentBlock.Divider)
                            }

                            "img" -> {
                                flush()
                                blocks.add(ContentBlock.ImageBlock(absUrl(child.attr("src"))))
                            }

                            "br" -> buffer.append("<br>")
                            "table" -> {
                                flush()
                                val rows = parseTable(child)
                                if (rows.isNotEmpty()) blocks.add(ContentBlock.Table(rows))
                            }

                            "ul", "ol" -> {
                                flush()
                                val ordered = child.tagName().lowercase() == "ol"
                                child
                                    .children()
                                    .filter { it.tagName().lowercase() == "li" }
                                    .forEachIndexed { i, li ->
                                        val marker = if (ordered) "${i + 1}. " else "• "
                                        val html = li.html().trim()
                                        if (html.isNotBlank()) blocks.add(ContentBlock.Paragraph(marker + html))
                                    }
                            }

                            "dd", "p", "div", "blockquote" -> {
                                flush()
                                walk(child, pre)
                                flush()
                            }

                            in inlineTags -> buffer.append(child.outerHtml())
                            else -> walk(child, pre)
                        }
                }
            }
        }

        walk(doc.body(), pre = false)
        flush()
        if (blocks.isEmpty()) {
            val whole = doc.body().html().trim()
            if (whole.isNotBlank()) blocks.add(ContentBlock.Paragraph(whole))
        }
        return blocks
    }

    /** Строки таблицы: td → html ячейки, th выделяется жирным. */
    private fun parseTable(table: Element): List<List<String>> =
        table.select("tr").mapNotNull { tr ->
            val cells =
                tr
                    .children()
                    .filter { it.tagName().lowercase() in setOf("td", "th") }
                    .map { cell ->
                        val html = cell.html().trim()
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

    private fun escape(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun absUrl(src: String): String =
        when {
            src.isBlank() -> src
            src.startsWith("http") || src.startsWith("data:") -> src
            src.startsWith("/") -> "${SamlibUrls.BASE}$src"
            else -> "${SamlibUrls.BASE}/$src"
        }
}
