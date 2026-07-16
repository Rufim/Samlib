package ru.samlib.client.ui.comments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentQuotesTest {
    @Test
    fun `сегменты делятся на свой текст и цитаты`() {
        val text = "> 10. Иван\n> текст цитаты\nМой ответ"
        val segs = segmentBody(text)
        assertEquals(2, segs.size)
        assertTrue(segs[0] is BodySegment.Quote)
        assertTrue(segs[1] is BodySegment.Own)
    }

    @Test
    fun `цитата в цитате строит вложенное дерево с именами авторов`() {
        val lines =
            listOf(
                "> 364. Киндеев Алексей Григорьевич",
                "> > 363. Волков",
                "> > > Всё равно не понял.",
                "> > Пебе. Он же пб.",
            )
        val root = buildQuoteTree(lines)

        // Верхний уровень — цитата 364 с именем автора.
        assertEquals(364, root.number)
        assertEquals("Киндеев Алексей Григорьевич", root.name)

        // Единственный потомок — вложенная цитата 363 (Волков).
        val nested = root.items.filterIsInstance<QuoteItem.Nested>()
        assertEquals(1, nested.size)
        val volkov = nested[0].node
        assertEquals(363, volkov.number)
        assertEquals("Волков", volkov.name)

        // Внутри Волкова: вложенная цитата (текст без номера) + своя строка «Пебе…».
        val volkovNested = volkov.items.filterIsInstance<QuoteItem.Nested>()
        val volkovLines = volkov.items.filterIsInstance<QuoteItem.Line>()
        assertEquals(1, volkovNested.size)
        assertTrue(volkovLines.any { it.text.startsWith("Пебе") })

        val deepest = volkovNested[0].node
        assertNull(deepest.number)
        assertTrue(
            deepest.items
                .filterIsInstance<QuoteItem.Line>()
                .any { it.text.startsWith("Всё равно") },
        )
    }
}
