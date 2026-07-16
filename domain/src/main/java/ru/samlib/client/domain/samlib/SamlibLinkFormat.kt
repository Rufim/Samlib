package ru.samlib.client.domain.samlib

/**
 * Форматы ссылок сайта samlib.ru, на которые независимо полагаются парсеры, репозитории,
 * ViewModel и навигация — единая точка вместо разбросанных строк-литералов.
 */
object SamlibLinkFormat {
    /** Суффикс ссылки на страницу произведения (work), напр. "/a/author/work.shtml". */
    const val WORK_SUFFIX = ".shtml"

    /** Префикс ссылки на страницу обсуждения (комментариев): "/comment" + work-ссылка без [WORK_SUFFIX]. */
    const val COMMENT_PREFIX = "/comment"

    /** Префикс раздела иллюстраций (без завершающего "/"). */
    const val ILLUSTRATIONS_PREFIX = "/img"

    /** Обрезает домен samlib из абсолютной ссылки и схлопывает повторяющиеся "/". */
    fun normalizeLink(href: String?): String? {
        if (href.isNullOrBlank()) return null
        var link = href.trim()
        val idx = link.indexOf(SamlibUrls.HOST)
        if (idx >= 0) link = link.substring(idx + SamlibUrls.HOST.length)
        link = link.replace(Regex("/+"), "/")
        if (!link.startsWith("/")) link = "/$link"
        return link.ifBlank { null }
    }
}
