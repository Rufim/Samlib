package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.samlib.SamlibLinkFormat

/**
 * Парсер «Топ авторов» (`/rating/hits/`). Строки — элементы `dl`:
 * `<DL><b>423394</b> <a href=...>Имя</a> "цитата" (59881k,150)<DD><i>описание</i></DD></DL>`
 */
class TopAuthorsParser {
    companion object {
        private val SIZE_COUNT = Regex("""\((\d+)k,\s*(\d+)\)""")
        private val QUOTE = Regex(""""([^"]*)"""")
    }

    fun parse(html: String): List<Author> {
        val doc =
            try {
                Jsoup.parse(html)
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать топ авторов", e)
            }
        return doc.select("dl").mapNotNull(::parseRow)
    }

    private fun parseRow(row: Element): Author? {
        val link =
            SamlibLinkFormat.normalizeLink(row.selectFirst("a[href]")?.attr("href")) ?: return null
        val fullName =
            row
                .selectFirst("a[href]")
                ?.text()
                ?.trim()
                .orEmpty()
        if (fullName.isBlank()) return null
        val views =
            row
                .selectFirst("b")
                ?.text()
                ?.filter { it.isDigit() }
                ?.toIntOrNull()
        val own = row.ownText()
        val quote =
            QUOTE
                .find(own)
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.ifBlank { null }
        val sizeCount = SIZE_COUNT.find(own)
        val size = sizeCount?.groupValues?.get(1)?.toIntOrNull()
        val workCount = sizeCount?.groupValues?.get(2)?.toIntOrNull()
        val annotation =
            row
                .selectFirst("dd")
                ?.text()
                ?.trim()
                ?.ifBlank { null }
        return Author(
            link = link,
            fullName = fullName,
            views = views,
            size = size,
            workCount = workCount,
            sectionAnnotation = quote,
            annotation = annotation,
        )
    }
}
