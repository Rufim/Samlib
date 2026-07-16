package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.samlib.client.domain.model.Genre
import ru.samlib.client.domain.model.Type
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.samlib.SamlibLinkFormat

class SearchParser {
    fun parse(html: String): List<Work> {
        val doc =
            try {
                Jsoup.parse(html)
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать результаты поиска", e)
            }
        return doc.select("table[width=640][border=0]").mapNotNull { parseRow(it) }
    }

    private fun parseRow(row: Element): Work? {
        val bodies = row.select("tbody")
        if (bodies.isEmpty()) return null
        val links = bodies[0].select("a[href]")
        if (links.size < 2) return null

        val authorLink = SamlibLinkFormat.normalizeLink(links[0].attr("href"))
        val authorName = links[0].text().trim()
        val workLink = SamlibLinkFormat.normalizeLink(links[1].attr("href")) ?: return null
        val title = links[1].text().trim()
        if (title.isBlank()) return null

        val size =
            ParserUtils.parseSizeKb(bodies[0].selectFirst("ul")?.text()?.substringBefore("k"))

        var type = Type.OTHER
        var genres: List<Genre> = emptyList()
        if (bodies.size > 1) {
            val fontText = bodies[1].selectFirst("font[size=2]")?.text().orEmpty()
            val parts = fontText.split("\"")
            if (parts.size > 1 && parts[1].contains(":")) {
                type = Type.parseType(parts[1].substringAfter(":").trim())
            }
            if (parts.size > 2 && parts[2].contains(":")) {
                genres =
                    parts[2]
                        .substringAfter(":")
                        .split(",")
                        .mapNotNull { Genre.parseGenre(it.trim()) }
            }
        }

        return Work(
            link = workLink,
            title = title,
            authorLink = authorLink,
            authorShortName = authorName,
            size = size,
            type = type,
            genres = genres,
        )
    }
}
