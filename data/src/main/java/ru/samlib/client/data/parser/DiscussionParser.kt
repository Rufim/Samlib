package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.model.Discussion
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.samlib.SamlibLinkFormat

/**
 * Парсер «Обсуждения» (`/rating/comment/`). Строки — `tr` таблицы `table[width=100%]`:
 * `<td><a href="/comment/.../x">Заголовок</a> <small><b>28k</b> Жанр</small></td>`
 * `<td><a href="/...">Автор</a></td><td><center>857/92</center></td><td><center>23:59, 03/07</center></td>`
 */
class DiscussionParser {
    fun parse(html: String): List<Discussion> {
        val doc =
            try {
                Jsoup.parse(html)
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать обсуждения", e)
            }
        return doc.select("table[width=100%] tr").mapNotNull(::parseRow)
    }

    private fun parseRow(row: Element): Discussion? {
        val tds = row.children().filter { it.tagName() == "td" }
        if (tds.size < 4) return null
        val titleAnchor = tds[0].selectFirst("a[href^=/comment/]") ?: return null

        val commentsLink = SamlibLinkFormat.normalizeLink(titleAnchor.attr("href"))
        val workLink =
            SamlibLinkFormat.normalizeLink(
                titleAnchor.attr("href").removePrefix(SamlibLinkFormat.COMMENT_PREFIX).let {
                    if (it.endsWith(SamlibLinkFormat.WORK_SUFFIX)) it else "$it${SamlibLinkFormat.WORK_SUFFIX}"
                },
            ) ?: return null
        val title = titleAnchor.text().trim().ifBlank { return null }
        val small = tds[0].selectFirst("small")
        val size = ParserUtils.parseSizeKb(small?.selectFirst("b")?.ownText())
        val genres = small?.ownText()?.trim()?.ifBlank { null }

        val authorAnchor = tds[1].selectFirst("a[href]")
        val authorLink = SamlibLinkFormat.normalizeLink(authorAnchor?.attr("href"))
        val authorName = authorAnchor?.text()?.trim()?.ifBlank { null }

        val counts = tds[2].text().trim().split("/")
        val count = counts.getOrNull(0)?.trim()?.toIntOrNull()
        val countOfDay = counts.getOrNull(1)?.trim()?.toIntOrNull()
        val lastOne = tds[3].text().trim().ifBlank { null }

        val work =
            Work(
                link = workLink,
                title = title,
                size = size,
                customGenres = genres,
                authorLink = authorLink,
                authorShortName = authorName,
            )
        val author = Author(link = authorLink ?: workLink, fullName = authorName)
        return Discussion(
            work = work,
            author = author,
            count = count,
            countOfDay = countOfDay,
            lastOne = lastOne,
            commentsLink = commentsLink,
        )
    }
}
