package ru.samlib.client.ui.comments

// Чистая (без UI) логика разбора тела комментария на свой текст и цитаты, а также
// построения дерева вложенных цитат по глубине «>» (цитата внутри цитаты).

/** Глубина цитирования по числу ведущих «>» (учитывая пробелы между ними). */
internal fun quoteDepth(line: String): Int {
    var s = line.trimStart()
    var d = 0
    while (s.startsWith(">")) {
        d++
        s = s.removePrefix(">").trimStart()
    }
    return d
}

internal fun stripQuoteMarkers(line: String): String {
    var s = line.trimStart()
    while (s.startsWith(">")) s = s.removePrefix(">").trimStart()
    return s
}

// Заголовок процитированного комментария: «NNN. Имя» → номер + имя автора.
internal val QUOTED_HEADER = Regex("""^(\d+)\.\s*(.*)$""")

internal sealed interface BodySegment {
    data class Own(
        val lines: List<String>,
    ) : BodySegment

    data class Quote(
        val lines: List<String>,
    ) : BodySegment
}

/** Делит текст на чередующиеся сегменты: свой текст и непрерывные блоки цитат. */
internal fun segmentBody(text: String): List<BodySegment> {
    val result = mutableListOf<BodySegment>()
    val buf = mutableListOf<String>()
    var inQuote = false

    fun flush() {
        if (buf.isNotEmpty()) {
            result.add(if (inQuote) BodySegment.Quote(buf.toList()) else BodySegment.Own(buf.toList()))
            buf.clear()
        }
    }
    for (line in text.lines()) {
        if (line.isBlank()) {
            buf.add(line)
            continue
        } // пустые строки не меняют тип сегмента
        val q = line.trimStart().startsWith(">")
        if (q != inQuote) {
            flush()
            inQuote = q
        }
        buf.add(line)
    }
    flush()
    return result
}

/** Элемент содержимого цитаты: строка своего текста или вложенная цитата. */
internal sealed interface QuoteItem {
    data class Line(
        val text: String,
    ) : QuoteItem

    data class Nested(
        val node: QuoteNode,
    ) : QuoteItem
}

/** Узел дерева цитат: автор/номер процитированного + содержимое (текст и вложенные цитаты). */
internal class QuoteNode(
    val number: Int?,
    val name: String?,
    val items: List<QuoteItem>,
)

/** Строит дерево цитат из строк блока по глубине «>» (цитата внутри цитаты). */
internal fun buildQuoteTree(lines: List<String>): QuoteNode {
    val leveled =
        lines.mapNotNull { l ->
            val content = stripQuoteMarkers(l)
            if (content.isBlank()) null else quoteDepth(l) to content
        }
    return buildQuoteNode(leveled)
}

private fun buildQuoteNode(lines: List<Pair<Int, String>>): QuoteNode {
    if (lines.isEmpty()) return QuoteNode(null, null, emptyList())
    val base = lines.minOf { it.first }
    val items = mutableListOf<QuoteItem>()
    var number: Int? = null
    var name: String? = null
    var i = 0
    while (i < lines.size) {
        val (depth, content) = lines[i]
        if (depth == base) {
            val m = QUOTED_HEADER.matchEntire(content)
            if (number == null && name == null && m != null) {
                number = m.groupValues[1].toIntOrNull()
                name = m.groupValues[2].trim().ifBlank { null }
            } else {
                items.add(QuoteItem.Line(content))
            }
            i++
        } else {
            var j = i
            while (j < lines.size && lines[j].first > base) j++
            items.add(QuoteItem.Nested(buildQuoteNode(lines.subList(i, j))))
            i = j
        }
    }
    return QuoteNode(number, name, items)
}
