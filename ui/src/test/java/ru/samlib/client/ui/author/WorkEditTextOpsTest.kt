package ru.samlib.client.ui.author

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.repository.DraftBlock

class WorkEditTextOpsTest {
    private fun apply(
        old: String,
        edit: ReplaceEdit,
    ): String = old.substring(0, edit.start) + edit.replacement + old.substring(edit.endOld)

    @Test
    fun `правка всегда переводит old в new`() {
        val cases =
            listOf(
                "привет мир" to "<b>привет</b> мир",
                "начало конец" to "начало ВСТАВКА конец",
                "абв" to "где",
                "тот же текст" to "тот же текст",
                "" to "новый",
                "старый" to "",
                "оболочка<b></b>" to "оболочка<b>текст</b>",
            )
        for ((old, new) in cases) {
            val edit = computeReplaceEdit(old, new)
            assertEquals(new, apply(old, edit))
        }
    }

    @Test
    fun `оборачивание выделения тегами — не трогает нетронутый хвост текста`() {
        val edit = computeReplaceEdit("привет мир", "<b>привет</b> мир")
        assertTrue(edit.endOld < "привет мир".length)
    }

    @Test
    fun `вставка в середину — правка не с самого начала текста`() {
        val edit = computeReplaceEdit("начало конец", "начало ВСТАВКА конец")
        assertTrue(edit.start > 0)
    }

    @Test
    fun `идентичные строки — пустая правка без изменений`() {
        val edit = computeReplaceEdit("тот же текст", "тот же текст")
        assertEquals(edit.start, edit.endOld)
        assertEquals("", edit.replacement)
    }

    @Test
    fun `находит блок, чей диапазон содержит offset`() {
        val blocks =
            listOf(
                DraftBlock(ContentBlock.Preformatted("a"), 0..9),
                DraftBlock(ContentBlock.Preformatted("b"), 10..19),
                DraftBlock(ContentBlock.Preformatted("c"), 20..29),
            )
        assertEquals(0, draftBlockIndexForOffset(blocks, 5))
        assertEquals(1, draftBlockIndexForOffset(blocks, 10))
        assertEquals(2, draftBlockIndexForOffset(blocks, 29))
    }

    @Test
    fun `offset за пределами последнего блока даёт последний блок`() {
        val blocks = listOf(DraftBlock(ContentBlock.Preformatted("a"), 0..9))
        assertEquals(0, draftBlockIndexForOffset(blocks, 999))
    }

    @Test
    fun `пустой список блоков даёт -1`() {
        assertEquals(-1, draftBlockIndexForOffset(emptyList(), 0))
    }

    @Test
    fun `короткий запрос не даёт результатов`() {
        assertEquals(DraftSearchResult.EMPTY, findDraftHits("привет мир привет", "п"))
    }

    @Test
    fun `находит все непересекающиеся вхождения без учёта регистра`() {
        val text = "Привет мир, привет снова"
        val result = findDraftHits(text, "привет")
        assertEquals(2, result.hits.size)
        assertTrue(!result.truncated)
        for (hit in result.hits) {
            assertEquals("привет", text.substring(hit.first, hit.last + 1).lowercase())
        }
    }

    @Test
    fun `пустой текст или запрос не находит совпадений`() {
        assertEquals(DraftSearchResult.EMPTY, findDraftHits("", "ищи"))
        assertEquals(DraftSearchResult.EMPTY, findDraftHits("текст", ""))
    }

    @Test
    fun `обрезает результаты по MAX_SEARCH_HITS и ставит truncated`() {
        val text = "а".repeat(1200)
        val result = findDraftHits(text, "аа")
        assertTrue(result.truncated)
        assertEquals(500, result.hits.size)
    }
}
