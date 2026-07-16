package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.samlib.client.domain.model.Image
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import ru.samlib.client.domain.samlib.SamlibUrls

/**
 * Парсер страницы иллюстраций (`/img/.../index.shtml`). Каждая картинка — в своей
 * `table[width=640]`: `<td><img src=/img/.../x.jpg w h><br><b>title</b> <i>ann</i>
 * <br><small>x.jpg:WxH, 112k</small></td>`.
 */
class IllustrationsParser {
    companion object {
        private val SIZE = Regex(""",\s*(\d+)k""")
    }

    fun parse(html: String): List<Image> {
        val doc =
            try {
                Jsoup.parse(html)
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать иллюстрации", e)
            }
        return doc
            .select("table[width=640] img[src]")
            .filter { it.attr("src").contains("${SamlibLinkFormat.ILLUSTRATIONS_PREFIX}/", ignoreCase = true) }
            .mapIndexedNotNull(::parseImage)
    }

    private fun parseImage(
        index: Int,
        img: Element,
    ): Image? {
        val src = SamlibLinkFormat.normalizeLink(img.attr("src")) ?: return null
        val td = img.closest("td")
        return Image(
            number = index,
            url = SamlibUrls.BASE + src,
            title =
                td
                    ?.selectFirst("b")
                    ?.text()
                    ?.trim()
                    ?.ifBlank { null },
            annotation =
                td
                    ?.selectFirst("i")
                    ?.text()
                    ?.trim()
                    ?.ifBlank { null },
            width = img.attr("width").toIntOrNull(),
            height = img.attr("height").toIntOrNull(),
            sizeKb =
                td
                    ?.selectFirst("small")
                    ?.text()
                    ?.let {
                        SIZE
                            .find(it)
                            ?.groupValues
                            ?.get(1)
                            ?.toIntOrNull()
                    },
        )
    }
}
