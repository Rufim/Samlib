package ru.samlib.client.data.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class IllustrationsParserTest {
    private fun fixture(name: String): String =
        javaClass
            .getResourceAsStream("/fixtures/$name")!!
            .readBytes()
            .toString(Charset.forName("windows-1251"))

    @Test
    fun `парсит иллюстрации`() {
        val images = IllustrationsParser().parse(fixture("illustrations.html"))

        assertTrue("непустой список", images.isNotEmpty())
        val img = images.first()
        assertTrue("полный url картинки", img.url.startsWith("http://samlib.ru/img/"))
        assertTrue("есть размеры", (img.width ?: 0) > 0 && (img.height ?: 0) > 0)
    }
}
