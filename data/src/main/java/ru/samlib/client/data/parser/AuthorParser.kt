package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.model.Category
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import ru.samlib.client.domain.samlib.SamlibUrls
import java.math.BigDecimal
import java.util.Date

class AuthorParser {
    fun parse(
        html: String,
        authorLink: String,
    ): Author {
        val link = SamlibLinkFormat.normalizeLink(authorLink) ?: authorLink
        val authorDir = authorDir(authorLink)

        val doc =
            try {
                Jsoup.parse(html)
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать страницу автора", e)
            }

        val h3El = doc.selectFirst("h3")
        val head =
            h3El
                ?.html()
                ?.substringBefore("<br")
                ?.let { Jsoup.parseBodyFragment(it).text().trim() }
        val fullName = head?.substringBefore(":")?.trim()?.ifBlank { null }
        val annotation = head?.substringAfter(":", "")?.trim()?.ifBlank { null }

        val acc = HeadAcc()
        region(html, "Блок шапки", "Блок управления разделом")?.let {
            parseHead(
                it,
                authorDir,
                acc,
            )
        }

        val worksRegion = region(html, "Блок ссылок на произведения", "Подножие") ?: html
        val categories = parseCategories(worksRegion, authorDir)

        val flatWorks = categories.flatMap { it.works }
        if (fullName.isNullOrBlank() && flatWorks.isEmpty()) {
            throw ParseException("Страница автора не распознана")
        }

        return Author(
            link = link,
            fullName = fullName,
            annotation = annotation,
            sectionAnnotation = acc.sectionAnnotation,
            about = acc.about,
            email = acc.email,
            dateBirth = acc.dateBirth,
            address = acc.address,
            lastUpdateDate = acc.lastUpdateDate,
            size = acc.size,
            workCount = acc.workCount ?: flatWorks.size,
            rate = acc.rate,
            votes = acc.votes,
            views = acc.views,
            friends = acc.friends,
            friendsOf = acc.friendsOf,
            authorSiteUrl = acc.authorSiteUrl,
            recommendations = acc.recommendations,
            avatarUrl = acc.avatarUrl,
            categories = categories,
            works = flatWorks,
        )
    }

    /** Разбор произведений из фрагмента (регион страницы или подстраница раздела). */
    fun parseWorksFromFragment(
        html: String,
        authorLink: String,
    ): List<Work> {
        val authorDir = authorDir(authorLink)
        val region = region(html, "Блок ссылок на произведения", "Подножие") ?: html
        return parseCategories(region, authorDir).flatMap { it.works }
    }

    private fun authorDir(authorLink: String): String {
        val link = SamlibLinkFormat.normalizeLink(authorLink) ?: authorLink
        // Базовый каталог автора: для страниц-подразделов (…/index_N.shtml) отбрасываем имя файла,
        // иначе относительные ссылки произведений разрешаются неправильно.
        return when {
            link.endsWith("/") -> link
            link.endsWith(SamlibLinkFormat.WORK_SUFFIX) -> link.substringBeforeLast('/') + "/"
            else -> "$link/"
        }
    }

    private class HeadAcc {
        var sectionAnnotation: String? = null
        var about: String? = null
        var email: String? = null
        var dateBirth: Date? = null
        var address: String? = null
        var lastUpdateDate: Date? = null
        var size: Int? = null
        var workCount: Int? = null
        var rate: BigDecimal? = null
        var votes: Int? = null
        var views: Int? = null
        var friends: Int? = null
        var friendsOf: Int? = null
        var authorSiteUrl: String? = null
        var recommendations: List<Work> = emptyList()
        var avatarUrl: String? = null
    }

    private class CatBuilder(
        val title: String?,
        val link: String?,
    ) {
        val works = mutableListOf<Work>()
    }

    private fun region(
        html: String,
        start: String,
        end: String,
    ): String? {
        val s = html.indexOf(start)
        if (s < 0) return null
        val e = html.indexOf(end, s)
        return if (e < 0) html.substring(s) else html.substring(s, e)
    }

    private fun parseHead(
        region: String,
        authorDir: String,
        acc: HeadAcc,
    ) {
        val frag = Jsoup.parseBodyFragment(region)
        val img = frag.selectFirst("img[src]")?.attr("src")?.trim()
        if (!img.isNullOrEmpty() &&
            Regex("""\.(jpe?g|gif|png)$""", RegexOption.IGNORE_CASE).containsMatchIn(img) &&
            !img.contains("clear.gif") &&
            !img.contains("sp.gif")
        ) {
            acc.avatarUrl =
                when {
                    img.startsWith("http") -> img
                    img.startsWith("/") -> "${SamlibUrls.BASE}$img"
                    else -> "${SamlibUrls.BASE}$authorDir$img"
                }
        }
        for (li in frag.select("ul li")) {
            try {
                parseInfoRow(li, acc)
            } catch (_: Exception) {
                // пропускаем битую строку
            }
        }
        val ai = region.indexOf("Аннотация к разделу:")
        if (ai >= 0) {
            val after = region.substring(ai + "Аннотация к разделу:".length)
            acc.sectionAnnotation =
                Jsoup
                    .parseBodyFragment(after)
                    .selectFirst("i")
                    ?.text()
                    ?.trim()
                    ?.ifBlank { null }
        }
        val ri = region.indexOf("Начните знакомство")
        if (ri >= 0) {
            val line = region.substring(ri).substringBefore("\n")
            val recs = mutableListOf<Work>()
            for (a in Jsoup.parseBodyFragment(line).select("a[href\$=.shtml]")) {
                val href = a.attr("href").trim()
                if (href.isEmpty() || href.startsWith("/")) continue
                val title = a.text().trim()
                if (title.isEmpty()) continue
                val full = SamlibLinkFormat.normalizeLink(authorDir + href) ?: continue
                recs.add(Work(link = full, title = title, authorLink = authorDir))
            }
            acc.recommendations = recs
        }
    }

    private fun parseInfoRow(
        li: Element,
        acc: HeadAcc,
    ) {
        val u = li.selectFirst("u")
        val label =
            li
                .selectFirst("b")
                ?.text()
                ?.trim()
                ?.removeSuffix(":")
                ?.trim()
                .orEmpty()
        val content = li.ownText().trim()
        when {
            u != null -> acc.email = u.text().trim().ifBlank { null }
            label == "WWW" ->
                acc.authorSiteUrl =
                    li.selectFirst("a")?.attr("href")?.ifBlank { null }

            label == "Родился" -> acc.dateBirth = ParserUtils.parseDate(content)
            label == "Живет" -> acc.address = content.ifBlank { null }
            label == "Обновлялось" -> acc.lastUpdateDate = ParserUtils.parseDate(content)
            label == "Объем" -> {
                val parts = content.split("/")
                acc.size = ParserUtils.parseSizeKb(parts.getOrNull(0))
                acc.workCount = parts.getOrNull(1)?.trim()?.toIntOrNull()
            }

            label == "Рейтинг" -> {
                val parts = content.split("*")
                acc.rate = parts.getOrNull(0)?.trim()?.toBigDecimalOrNull()
                acc.votes = parts.getOrNull(1)?.trim()?.toIntOrNull()
            }

            label.startsWith("Посетителей") || label.startsWith("Посещений") ->
                acc.views = content.toIntOrNull()

            label.contains("Friend") -> {
                val parts = content.split("/")
                if (parts.size >= 2) {
                    acc.friends = parts[0].trim().toIntOrNull()
                    acc.friendsOf = parts[1].trim().toIntOrNull()
                } else {
                    acc.friendsOf = content.toIntOrNull()
                }
            }
        }
    }

    private fun parseCategories(
        region: String,
        authorDir: String,
    ): List<Category> {
        val builders = mutableListOf<CatBuilder>()
        var current: CatBuilder? = null
        val root = CatBuilder(title = null, link = null)

        // <DL>...</DL> может быть отформатирован на нескольких физических строках —
        // схлопываем внутренние переводы строк, чтобы блок разбирался как единое целое.
        val normalizedRegion =
            Regex("(?is)<DL>\\s*<DT>.*?</DL>").replace(region) { it.value.replace("\n", " ") }

        for (rawLine in normalizedRegion.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            try {
                val lineDoc = Jsoup.parseBodyFragment(line)
                when {
                    line.contains("font size=+1") || line.contains("<h3") -> {
                        val header =
                            lineDoc.selectFirst("font[size=+1]") ?: lineDoc.selectFirst("h3")
                        val title =
                            header
                                ?.text()
                                ?.trim()
                                ?.removeSuffix(":")
                                ?.trim()
                                ?.ifBlank { null }
                        // Раздел-подстраница: заголовок группы со ссылкой index_N.shtml (работы на отдельной странице).
                        val idxA = lineDoc.selectFirst("a[href^=index]")
                        val catLink =
                            idxA?.let {
                                SamlibLinkFormat.normalizeLink(
                                    authorDir + it.attr("href").trim(),
                                )
                            }
                        current =
                            CatBuilder(title = title, link = catLink).also { builders.add(it) }
                    }

                    line.startsWith("</small>") && lineDoc.selectFirst("a[href^=index]") != null -> {
                        val a = lineDoc.selectFirst("a[href^=index]")!!
                        val catLink = SamlibLinkFormat.normalizeLink(authorDir + a.attr("href").trim())
                        current =
                            CatBuilder(title = a.text().trim().ifBlank { null }, link = catLink)
                                .also { builders.add(it) }
                    }

                    line.contains("<DL", ignoreCase = true) -> {
                        parseWorkDl(lineDoc, authorDir)?.let { (current ?: root).works.add(it) }
                    }
                }
            } catch (_: Exception) {
                // пропускаем битую строку
            }
        }

        val result = mutableListOf<Category>()
        if (root.works.isNotEmpty()) result.add(Category(title = null, works = root.works.toList()))
        builders.forEach {
            result.add(
                Category(
                    title = it.title,
                    link = it.link,
                    works = it.works.toList(),
                ),
            )
        }
        return result
    }

    private fun parseWorkDl(
        doc: Element,
        authorDir: String,
    ): Work? {
        val a = doc.selectFirst("a[href\$=.shtml]") ?: return null
        val href = a.attr("href").trim()
        if (href.isEmpty() || href.startsWith("/")) return null
        val title = a.text().trim()
        if (title.isEmpty()) return null
        val link = SamlibLinkFormat.normalizeLink(authorDir + href) ?: return null

        var size: Int? = null
        var rate: BigDecimal? = null
        var votes: Int? = null
        for (b in doc.select("b")) {
            val t = b.ownText().trim()
            when {
                t.matches(Regex("""\d+k \d+\.\d+\*\d+""")) -> {
                    val parts = t.split(" ")
                    size = ParserUtils.parseSizeKb(parts[0])
                    val r = parts[1].split("*")
                    rate = r[0].toBigDecimalOrNull()
                    votes = r.getOrNull(1)?.toIntOrNull()
                }

                t.matches(Regex("""\d+\.\d+\*\d+""")) -> {
                    val r = t.split("*")
                    rate = r[0].toBigDecimalOrNull()
                    votes = r.getOrNull(1)?.toIntOrNull()
                }

                t.matches(Regex("""\d+k""")) -> size = ParserUtils.parseSizeKb(t)
            }
        }

        val small = doc.selectFirst("small")
        val genre =
            small
                ?.ownText()
                ?.replace("Оценка:", "")
                ?.trim()
                ?.trim('"')
                ?.trim()
                ?.ifBlank { null }
        val commentCount =
            small
                ?.selectFirst("a[href*=/comment/]")
                ?.text()
                ?.substringAfter(":", "")
                ?.trim()
                ?.takeWhile { it.isDigit() }
                ?.toIntOrNull()
        val annotation =
            doc
                .selectFirst("dd")
                ?.html()
                ?.trim()
                ?.ifBlank { null }

        return Work(
            link = link,
            title = title,
            authorLink = authorDir,
            size = size,
            rate = rate,
            votes = votes,
            customGenres = genre,
            commentCount = commentCount,
            annotation = annotation,
        )
    }
}
