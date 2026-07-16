package ru.samlib.client.data.parser

import android.util.Log
import org.jsoup.Jsoup
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.samlib.SamlibLinkFormat

class WorkParser {
    // Подвал страницы: «Размещен: 09/11/2010, изменен: 15/03/2024. 186k.»
    private val changedDateRegex = Regex("""изменен:\s*(\d{2}/\d{2}/\d{4})""")
    private val footerSizeRegex = Regex("""изменен:\s*\d{2}/\d{2}/\d{4}\.\s*(\d+)k""")

    fun parse(
        html: String,
        workLink: String,
    ): Work {
        val doc =
            try {
                Jsoup.parse(html)
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать произведение", e)
            }
        val link = SamlibLinkFormat.normalizeLink(workLink) ?: workLink

        // Заголовок: <title> вида «Автор. Название произведения».
        val rawTitle = doc.selectFirst("title")?.text()?.trim()
        val title =
            rawTitle
                ?.let { if (it.contains(". ")) it.substringAfterLast(". ").trim() else it }
                ?.ifBlank { null }
        // Автор — часть <title> до последней «. » («Автор. Название»).
        val author =
            rawTitle
                ?.let { if (it.contains(". ")) it.substringBeforeLast(". ").trim() else null }
                ?.ifBlank { null }
        // Жанр(ы) произведения — ссылки на /janr/index_janr_<цифра>… (навигационные
        // вроде index_janr_time23 не совпадают, т.к. после janr_ идёт буква).
        val genres =
            doc
                .select("a[href~=/janr/index_janr_\\d]")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
                .ifBlank { null }

        val rawContent = extractBody(html)

        if (title.isNullOrBlank() && rawContent.isBlank()) {
            throw ParseException("Страница произведения не распознана")
        }
        val updateDate =
            changedDateRegex
                .find(html)
                ?.groupValues
                ?.get(1)
                ?.let { ParserUtils.parseDate(it) }
        val size =
            footerSizeRegex
                .find(html)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        return Work(
            link = link,
            title = title,
            authorShortName = author,
            customGenres = genres,
            rawContent = rawContent,
            updateDate = updateDate,
            size = size,
        )
    }

    private fun extractBody(html: String): String =
        extractSamlibContent(html) ?: run {
            // Маркеры страницы не найдены — либо это не samlib, либо сайт изменил
            // вёрстку; логируем, иначе деградация до «всего body» пройдёт молча.
            Log.w("WorkParser", "маркеры контента не найдены, fallback на <body>")
            Jsoup
                .parse(html)
                .selectFirst("body")
                ?.html()
                .orEmpty()
        }

    companion object {
        const val START_MARKER = "Собственно произведение"
        const val END_MARKER = "Блок описания произведения (слева внизу)"

        /**
         * Тело произведения samlib расположено между HTML-комментариями
         * «Собственно произведение» и «Блок описания произведения (слева внизу)»
         * (см. legacy WorkParser Splitter). Возвращает null, если это не страница samlib.
         */
        fun extractSamlibContent(html: String): String? {
            val start = html.indexOf(START_MARKER)
            val end = html.indexOf(END_MARKER)
            if (start >= 0 && end > start) {
                val afterStartComment =
                    html.indexOf("-->", start).let { if (it >= 0) it + 3 else start }
                val endCommentStart =
                    html
                        .lastIndexOf("<!--", end)
                        .let { if (it in (afterStartComment + 1) until end) it else end }
                return html.substring(afterStartComment, endCommentStart).trim()
            }
            return null
        }
    }
}
