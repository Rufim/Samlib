package ru.samlib.client.ui.common

import ru.samlib.client.domain.samlib.SamlibLinkFormat
import ru.samlib.client.domain.samlib.SamlibUrls
import java.net.URI

/** Куда ведёт samlib-ссылка (или внешняя/неподдерживаемая). */
sealed interface SamlibTarget {
    data class Work(
        val link: String,
    ) : SamlibTarget

    data class Author(
        val link: String,
    ) : SamlibTarget

    data class Comments(
        val link: String,
    ) : SamlibTarget

    data class Illustrations(
        val link: String,
    ) : SamlibTarget

    data class External(
        val url: String,
    ) : SamlibTarget

    data class Unsupported(
        val link: String,
    ) : SamlibTarget
}

// Служебные разделы samlib, для которых нет своего экрана → заглушка.
private val SERVICE = Regex("^/(cgi-bin|rating|janr|long|type|stat|i/info|zhurnal|gb|about)")
private val AUTHOR_DIR = Regex("^/[a-z]/[a-zA-Z0-9_]+/")

/**
 * Классифицирует ссылку: samlib-страница → соответствующий экран, внешний хост →
 * External, неизвестный тип samlib → Unsupported (заглушка). [authorDir] — для
 * относительных ссылок (каталог автора текущей страницы).
 */
fun classifySamlibLink(
    href: String,
    authorDir: String? = null,
): SamlibTarget {
    val h = href.trim()
    if (h.isEmpty()) return SamlibTarget.External(href)
    val uri = runCatching { URI(h) }.getOrNull()
    val host = uri?.host
    if (host != null) {
        val isSamlib =
            host.equals(SamlibUrls.HOST, ignoreCase = true) ||
                host.endsWith(".${SamlibUrls.HOST}", ignoreCase = true)
        if (!isSamlib) return SamlibTarget.External(href)
        val norm =
            SamlibLinkFormat.normalizeLink(uri.path.orEmpty()) ?: return SamlibTarget.External(href)
        return classifyPath(norm)
    }
    // Непрозрачная схема без хоста (mailto:, tel:, javascript:) → внешняя.
    if (uri?.isOpaque == true) return SamlibTarget.External(href)
    val resolved =
        if (authorDir != null && !h.startsWith("/")) {
            authorDir.trimEnd('/') + "/" + h
        } else {
            h
        }
    val norm = SamlibLinkFormat.normalizeLink(resolved) ?: return SamlibTarget.External(href)
    return classifyPath(norm)
}

private fun classifyPath(norm: String): SamlibTarget =
    when {
        norm.startsWith("${SamlibLinkFormat.COMMENT_PREFIX}/") -> SamlibTarget.Comments(norm)
        norm.startsWith("${SamlibLinkFormat.ILLUSTRATIONS_PREFIX}/") -> SamlibTarget.Illustrations(norm)
        SERVICE.containsMatchIn(norm) -> SamlibTarget.Unsupported(norm)
        norm.endsWith(SamlibLinkFormat.WORK_SUFFIX) -> SamlibTarget.Work(norm)
        AUTHOR_DIR.containsMatchIn(norm) -> SamlibTarget.Author(norm)
        else -> SamlibTarget.Unsupported(norm)
    }

/** Приводит относительную ссылку читалки к абсолютному пути (по каталогу автора). */
fun resolveSamlibHref(
    href: String,
    authorDir: String?,
): String {
    val h = href.trim()
    val uri = runCatching { URI(h) }.getOrNull()
    if (uri?.host != null || uri?.isOpaque == true || h.startsWith("/") || authorDir == null) return h
    return authorDir.trimEnd('/') + "/" + h
}
