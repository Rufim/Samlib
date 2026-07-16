package ru.samlib.client.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.repository.DraftBlock

class WorkDraftChunkerTest {
    private val chunker = DefaultWorkDraftChunker(DefaultWorkContentParser())

    @Test
    fun `пустой текст даёт пустой список блоков`() {
        assertEquals(emptyList<DraftBlock>(), chunker.chunk(""))
        assertEquals(emptyList<DraftBlock>(), chunker.chunk("   \n  "))
    }

    @Test
    fun `короткий текст без пустых строк — один блок на весь диапазон`() {
        val text = "Просто один абзац без форматирования."
        val blocks = chunker.chunk(text)
        assertEquals(1, blocks.size)
        assertEquals(0..text.length - 1, blocks[0].rawRange)
        assertTrue(blocks[0].block is ContentBlock.Preformatted)
    }

    @Test
    fun `текст с пустыми строками режется на отдельные блоки по абзацам`() {
        val text = "Абзац один\n\nАбзац два\n\nАбзац три"
        val blocks = chunker.chunk(text)
        assertEquals(3, blocks.size)
        assertEquals(
            "Абзац один",
            text.substring(blocks[0].rawRange.first, blocks[0].rawRange.last + 1),
        )
        assertEquals(
            "Абзац два",
            text.substring(blocks[1].rawRange.first, blocks[1].rawRange.last + 1),
        )
        assertEquals(
            "Абзац три",
            text.substring(blocks[2].rawRange.first, blocks[2].rawRange.last + 1),
        )
    }

    @Test
    fun `сверхдлинный абзац без пустых строк режется на несколько кусков без потерь`() {
        val text = (1..2000).joinToString(" ") { "слово$it" }
        val blocks = chunker.chunk(text)
        assertTrue("ожидали несколько кусков, получили ${blocks.size}", blocks.size > 1)
        assertEquals(0, blocks.first().rawRange.first)
        assertEquals(text.length - 1, blocks.last().rawRange.last)
        for (i in 1 until blocks.size) {
            assertEquals(blocks[i - 1].rawRange.last + 1, blocks[i].rawRange.first)
        }
    }

    @Test
    fun `абзацы через одиночный перенос строки — каждый свой диапазон, а не общий на весь текст`() {
        // формат переводных текстов: каждый <dd>-абзац на отдельной строке без пустых строк между ними
        val text = "<dd>&nbsp;первый\n<dd>&nbsp;второй\n<dd>&nbsp;третий"
        val blocks = chunker.chunk(text)
        assertTrue(
            "ожидали минимум 3 разных диапазона, получили ${
                blocks.map { it.rawRange }.distinct()
            }",
            blocks.map { it.rawRange }.distinct().size >= 3,
        )
        assertEquals(
            "<dd>&nbsp;первый",
            text.substring(blocks[0].rawRange.first, blocks[0].rawRange.last + 1),
        )
    }

    @Test
    fun `теги форматирования внутри куска рендерятся так же, как в читалке`() {
        val text = "Обычный текст с <b>жирным</b> словом."
        val blocks = chunker.chunk(text)
        assertTrue(
            blocks.any {
                it.block is ContentBlock.Paragraph &&
                    (it.block as ContentBlock.Paragraph).html.contains(
                        "<b>жирным</b>",
                    )
            },
        )
    }
}
