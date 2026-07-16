package ru.samlib.client.data.parser

import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.repository.DraftBlock
import ru.samlib.client.domain.repository.WorkContentParser
import ru.samlib.client.domain.repository.WorkDraftChunker

/**
 * Режет сырой текст черновика (`WorkEditState.text`, тот же формат, что редактирует `FormatToolbar`)
 * на куски по границам переносов строк, затем каждый кусок прогоняет через [WorkContentParser]
 * (оборачивая в `<pre>`, как делает сервер при отдаче страницы) — так теги b/i/u/h3/ul/li/a/table
 * рендерятся, а куски достаточно малы для virtualization LazyColumn на больших текстах.
 * Граница — любая последовательность из одного или более `\n` (а не только пустая строка):
 * если резать только по пустым строкам, черновик, где каждый `<dd>`-абзац отделён одиночным
 * переносом (частый формат переводных текстов), достанется парсеру одним куском и превратится
 * в десятки [ContentBlock] с ОДНИМ и тем же общим [DraftBlock.rawRange] — тогда переход по offset
 * (поиск/синхронизация позиции) всегда попадал бы на первый из них, а не на нужный.
 * Кусок без внутренних переносов длиннее [MAX_CHUNK] режется дополнительно по ближайшему
 * пробелу — иначе один сплошной абзац в несколько МБ дал бы один нечанкованный блок.
 */
class DefaultWorkDraftChunker(
    private val parser: WorkContentParser,
) : WorkDraftChunker {
    override fun chunk(text: String): List<DraftBlock> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<DraftBlock>()
        for (range in paragraphRanges(text)) {
            val piece = text.substring(range.first, range.last + 1)
            if (piece.isBlank()) continue
            for (block in parser.parse("<pre>$piece</pre>")) {
                result.add(DraftBlock(block, range))
            }
        }
        return result
    }

    private fun paragraphRanges(text: String): List<IntRange> {
        val breaks = PARAGRAPH_BREAK.findAll(text).map { it.range }.toList()
        val paragraphs = mutableListOf<IntRange>()
        var start = 0
        for (b in breaks) {
            if (b.first > start) paragraphs.add(start until b.first)
            start = b.last + 1
        }
        if (start < text.length) paragraphs.add(start until text.length)
        return paragraphs.flatMap { splitLong(text, it) }
    }

    // Диапазоны из paragraphRanges уже не содержат '\n' внутри (это и есть граница) —
    // резать сверхдлинный кусок можно только по пробелу.
    private fun splitLong(
        text: String,
        range: IntRange,
    ): List<IntRange> {
        if (range.last - range.first + 1 <= MAX_CHUNK) return listOf(range)
        val result = mutableListOf<IntRange>()
        var start = range.first
        while (start <= range.last) {
            var end = (start + MAX_CHUNK - 1).coerceAtMost(range.last)
            if (end < range.last) {
                val spacePos = text.lastIndexOf(' ', end).takeIf { it > start } ?: -1
                if (spacePos > start) end = spacePos
            }
            result.add(start..end)
            start = end + 1
        }
        return result
    }

    private companion object {
        val PARAGRAPH_BREAK = Regex("\n+")
        const val MAX_CHUNK = 4000
    }
}
