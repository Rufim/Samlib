package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import ru.samlib.client.domain.samlib.SamlibUrls
import java.net.URLDecoder

/**
 * Разбор выдачи веб-поиска (DuckDuckGo Lite, запрос `site:samlib.ru …`) в произведения.
 * Google-поиск из легаси нерабочий (AJAX Search API закрыт, прямой парсинг блокируется),
 * поэтому надёжный полнотекстовый поиск по сайту идёт через DuckDuckGo Lite.
 */
class WebSearchParser {
    private companion object {
        // Страница автора/раздела: /x/name/ (без .shtml). Служебные пути отсекаем.
        val AUTHOR_DIR = Regex("^/[a-z]/[a-zA-Z0-9_]+/$")
        val SERVICE = Regex("^/(cgi-bin|rating|comment|img|janr|long|type)")
    }

    /** Финальная страница «ничего не найдено» (а не промежуточная при загрузке). */
    fun isNoResults(html: String): Boolean =
        html.contains("No results", ignoreCase = true) ||
            html.contains("No more results", ignoreCase = true) ||
            html.contains("Ничего не найдено", ignoreCase = true)

    /** Страница-челлендж DuckDuckGo (капча «выберите уточек») вместо результатов. */
    fun isCaptcha(html: String): Boolean =
        html.contains("anomaly", ignoreCase = true) ||
            html.contains("confirm this search was made by a human", ignoreCase = true) ||
            html.contains("Select all squares", ignoreCase = true)

    fun parse(html: String): List<Work> {
        val doc =
            try {
                Jsoup.parse(html)
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать результаты поиска", e)
            }
        val seen = HashSet<String>()
        return doc.select("a[href*=${SamlibUrls.HOST}]").mapNotNull { a ->
            val text = a.text().trim()
            if (text.isBlank()) return@mapNotNull null
            // DDG-html оборачивает ссылки в /l/?uddg=<url> — разворачиваем; lite даёт прямые.
            val href = a.attr("href")
            val target =
                if (href.contains("uddg=")) {
                    runCatching {
                        URLDecoder.decode(href.substringAfter("uddg=").substringBefore("&"), "UTF-8")
                    }.getOrDefault(href)
                } else {
                    href
                }
            val path = SamlibLinkFormat.normalizeLink(target) ?: return@mapNotNull null
            if (SERVICE.containsMatchIn(path)) return@mapNotNull null
            // произведение (.shtml) либо страница автора (/x/name/); прочее — мимо
            val isWork = path.endsWith(SamlibLinkFormat.WORK_SUFFIX)
            val isAuthor = AUTHOR_DIR.matches(path)
            if (!isWork && !isAuthor) return@mapNotNull null
            if (!seen.add(path)) return@mapNotNull null
            // заголовок вида «Автор. Название» → разделяем
            val author = if (text.contains(". ")) text.substringBefore(". ").trim() else null
            val title = if (text.contains(". ")) text.substringAfter(". ").trim() else text
            Work(link = path, title = title.ifBlank { text }, authorShortName = author)
        }
    }
}
