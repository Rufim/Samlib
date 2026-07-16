package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import ru.samlib.client.domain.model.Comment
import ru.samlib.client.domain.samlib.SamlibLinkFormat

/**
 * Парсер страницы комментариев (`/comment/...`). HTML плоский: комментарии
 * разделены `<hr noshade>`, каждый начинается с `<small>N.</small> <b>Ник</b>
 * <small><i>дата</i></small>`, далее тело с `<br>` и цитатами (`&gt;`).
 */
class CommentsParser {
    companion object {
        /** Значение параметра OPERATION для удаления комментария (SamlibHttpClient.commentAction). */
        const val OPERATION_DELETE = "delete"

        private val ROW = Regex("""<small>\s*(\d+)\.\s*</small>""")
        private val HR = Regex("""(?i)<hr\s+noshade""")
        private val BR = Regex("""(?i)<br\s*/?>""")
        private val TAG = Regex("""<[^>]+>""")

        // первая цитата в теле ответа — ссылка на родительский комментарий: <font color=...>NNN. Ник</font>
        private val PARENT = Regex("""(?i)<font\s+color=[^>]*>\s*(\d+)\.""")
        private val MSGID = Regex("""(?i)MSGID=(\d+)""")
    }

    fun parse(html: String): List<Comment> {
        val doc =
            try {
                // прогоняем через Jsoup только для валидации/ранней ошибки на мусоре
                Jsoup.parse(html)
                html
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать комментарии", e)
            }
        val markers = ROW.findAll(doc).toList()
        return markers.mapIndexedNotNull { i, m ->
            val start = m.range.first
            val end = if (i + 1 < markers.size) markers[i + 1].range.first else doc.length
            var seg = doc.substring(start, end)
            HR.find(seg)?.let { seg = seg.substring(0, it.range.first) }
            parseComment(m.groupValues[1].toIntOrNull() ?: return@mapIndexedNotNull null, seg)
        }
    }

    private fun parseComment(
        number: Int,
        seg: String,
    ): Comment {
        val fragment = Jsoup.parseBodyFragment(seg)
        val nick =
            fragment
                .selectFirst("b")
                ?.text()
                ?.trim()
                ?.ifBlank { null }
        val date =
            fragment
                .selectFirst("small i")
                ?.text()
                ?.trim()
                ?.ifBlank { null }
        val userLink = SamlibLinkFormat.normalizeLink(fragment.selectFirst("b a[href]")?.attr("href"))
        val deleted = seg.contains("Удалено")

        // тело — всё после второго </small> (номер + дата), либо после первого
        val firstClose = seg.indexOf("</small>")
        val secondClose = if (firstClose >= 0) seg.indexOf("</small>", firstClose + 8) else -1
        val bodyHtml =
            when {
                secondClose >= 0 -> seg.substring(secondClose + 8)
                firstClose >= 0 -> seg.substring(firstClose + 8)
                else -> seg
            }
        val parent =
            PARENT
                .find(bodyHtml)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        val msgid = MSGID.find(seg)?.groupValues?.get(1)
        return Comment(
            number = number,
            nickName = nick,
            date = date,
            body = htmlToText(bodyHtml),
            userLink = userLink,
            deleted = deleted,
            parentNumber = parent,
            msgid = msgid,
            canReply = seg.contains("ответить", ignoreCase = true),
            canDelete = seg.contains("OPERATION=$OPERATION_DELETE", ignoreCase = true),
        )
    }

    private fun htmlToText(html: String): String {
        var t = BR.replace(html, "\n")
        t = TAG.replace(t, "")
        t = Parser.unescapeEntities(t, false)
        return t
            .lines()
            .map { it.replace(Regex("""[ \t ]+"""), " ").trim() }
            .joinToString("\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }
}
