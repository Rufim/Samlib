package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import java.util.Calendar
import java.util.Date

class NewestParser {
    companion object {
        private val TIME_ONLY = Regex("""^\d{2}:\d{2}$""")
        private val DAY_MONTH = Regex("""^\d{2}/\d{2}$""")
    }

    fun parse(
        html: String,
        skip: Int = 0,
        size: Int = Int.MAX_VALUE,
    ): List<Work> {
        val doc =
            try {
                Jsoup.parse(html)
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать страницу новинок", e)
            }
        val rows = doc.select("table tbody td[width=600] table tbody tr")
        val result = mutableListOf<Work>()
        // первые 2 строки — заголовки таблицы
        val start = 2 + skip
        var i = start
        while (i < rows.size && result.size < size) {
            parseRow(rows[i])?.let { result.add(it) }
            i++
        }
        return result
    }

    private fun parseRow(row: Element): Work? {
        val cells = row.select("td")
        if (cells.size < 3) return null
        // [0] заголовок «"..."» + <small><b>..k</b> жанры</small> + ссылка
        val titleCell = cells[0]
        val rawTitle = titleCell.text()
        val title =
            rawTitle
                .substringAfter('"', "")
                .substringBeforeLast('"', "")
                .replace("\n", "")
                .trim()
                .ifBlank { rawTitle.trim() }
        val small = titleCell.selectFirst("small")
        val sizeStr = small?.selectFirst("b")?.ownText()
        val size = ParserUtils.parseSizeKb(sizeStr)
        val genres = small?.ownText()?.trim()?.ifBlank { null }
        val workHref =
            SamlibLinkFormat.normalizeLink(titleCell.selectFirst("a[href]")?.attr("href"))
                ?: return null
        // [1] автор
        val authorCell = cells[1]
        val authorHref =
            SamlibLinkFormat.normalizeLink(
                authorCell.selectFirst("a[href]")?.attr("href")?.replace("indexdate.shtml", ""),
            )
        val authorName = authorCell.text().trim()
        // [2] дата: полная «dd/MM/yyyy», либо (как отдаёт список новинок) «HH:mm» для
        // обновлений за сегодня, либо «dd/MM» без года для более ранних обновлений
        val rawDate = cells[2].text().trim()
        val updateDate = parseCellDate(rawDate)

        return Work(
            link = workHref,
            title = title,
            size = size,
            authorLink = authorHref,
            authorShortName = authorName,
            updateDate = updateDate,
            dateText = rawDate.ifBlank { null },
            customGenres = genres,
        )
    }

    private fun parseCellDate(raw: String): Date? {
        if (raw.isBlank()) return null
        return when {
            TIME_ONLY.matches(raw) -> {
                val (hour, minute) = raw.split(":").map(String::toInt)
                Calendar
                    .getInstance()
                    .apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
            }

            DAY_MONTH.matches(raw) -> {
                val year = Calendar.getInstance().get(Calendar.YEAR)
                val parsed = ParserUtils.parseDate("$raw/$year", "dd/MM/yyyy")
                if (parsed != null) {
                    val cal = Calendar.getInstance().apply { time = parsed }
                    val now = Calendar.getInstance()
                    if (cal.timeInMillis - now.timeInMillis > 24L * 60 * 60 * 1000) {
                        cal.add(Calendar.YEAR, -1)
                    }
                    cal.time
                } else {
                    null
                }
            }

            else -> ParserUtils.parseDate(raw, "dd/MM/yyyy")
        }
    }
}
