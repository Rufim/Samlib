package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import java.math.BigDecimal

/**
 * Единый парсер рейтинговых списков произведений: «Обзоры» (жанр литобзора) и
 * «Избранное» (`/rating/expert/`). Экспертная страница — надмножество жанровой
 * (добавляет ведущую ссылку с экспертной оценкой). Строки — `dl`, внутри `li`:
 *
 * `<DL><DT><li>[<b><a href=...best_show>7.98*87</a></b>] <a href=/w/../><font>Автор</font></a>:
 *  <a href=/w/../x.shtml><b>Название</b></a> <small><b>28k</b> Оценка:<b>06.47*1154</b> Жанры
 *  <a href=/comment/..>Комментарии</a></small><DD><i>аннотация</i></DD></DL>`
 */
class WorkRatingParser {
    companion object {
        private val SIZE = Regex("""^\d+k$""")
        private val RATE_VOTES = Regex("""^\s*(\d+(?:\.\d+)?)\s*\*\s*(\d+)\s*$""")
    }

    fun parse(html: String): List<Work> {
        val doc =
            try {
                Jsoup.parse(html)
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать список произведений", e)
            }
        return doc.select("dl").mapNotNull(::parseRow)
    }

    private fun parseRow(row: Element): Work? {
        val li = row.selectFirst("li") ?: row
        val workAnchor = li.selectFirst("a[href~=(?i)\\.shtml]") ?: return null
        val workLink = SamlibLinkFormat.normalizeLink(workAnchor.attr("href")) ?: return null
        val title = workAnchor.text().trim().ifBlank { return null }

        // автор: ссылка не на произведение и не на cgi-bin (best_show)
        val authorAnchor =
            li.select("a[href]").firstOrNull {
                val h = it.attr("href")
                !h.contains(SamlibLinkFormat.WORK_SUFFIX, true) &&
                    !h.contains("cgi-bin", true) &&
                    !h.contains(
                        "best_show",
                        true,
                    )
            }
        val authorLink = SamlibLinkFormat.normalizeLink(authorAnchor?.attr("href"))
        val authorName = authorAnchor?.text()?.trim()?.ifBlank { null }

        // экспертная оценка (только на /rating/expert/)
        val expert = li.selectFirst("a[href*=best_show]")?.text()?.let(::parseRateVotes)

        val small = li.selectFirst("small")
        var size: Int? = null
        var rate: BigDecimal? = null
        var votes: Int? = null
        small?.select("b")?.forEach { b ->
            val t = b.text().trim()
            when {
                SIZE.matches(t) -> size = t.removeSuffix("k").toIntOrNull()
                RATE_VOTES.matches(t) ->
                    parseRateVotes(t)?.let {
                        rate = it.first
                        votes = it.second
                    }
            }
        }
        val genres =
            small
                ?.ownText()
                ?.replace("Оценка:", "")
                ?.replace(Regex("""[\s\u00A0]+"""), " ")
                ?.trim()
                ?.ifBlank { null }

        return Work(
            link = workLink,
            title = title,
            authorLink = authorLink,
            authorShortName = authorName,
            size = size,
            rate = rate,
            votes = votes,
            expertRate = expert?.first,
            expertVotes = expert?.second,
            customGenres = genres,
            annotation =
                row
                    .selectFirst("dd")
                    ?.text()
                    ?.trim()
                    ?.ifBlank { null },
        )
    }

    private fun parseRateVotes(raw: String): Pair<BigDecimal, Int>? {
        val m = RATE_VOTES.find(raw.trim()) ?: return null
        val rate = m.groupValues[1].toBigDecimalOrNull() ?: return null
        val votes = m.groupValues[2].toIntOrNull() ?: return null
        return rate to votes
    }
}
