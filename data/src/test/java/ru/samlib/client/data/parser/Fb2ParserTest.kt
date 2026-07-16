package ru.samlib.client.data.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.domain.model.ContentBlock

class Fb2ParserTest {
    private fun bytes(name: String): ByteArray = javaClass.getResourceAsStream("/fixtures/$name")!!.readBytes()

    @Test
    fun `парсит fb2 zip в блоки`() {
        val doc = Fb2Parser().parse(bytes("sample.fb2.zip"))

        assertTrue("есть заголовок", !doc.title.isNullOrBlank())
        assertTrue("много блоков", doc.blocks.size > 20)
        assertTrue("есть абзацы", doc.blocks.any { it is ContentBlock.Paragraph })
        assertTrue(
            "у абзацев есть текст",
            doc.blocks.filterIsInstance<ContentBlock.Paragraph>().any { it.html.length > 20 },
        )
    }
}
