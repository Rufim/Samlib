package ru.samlib.client.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.domain.model.ContentBlock
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocParserTest {
    private fun docx(
        documentXml: String,
        coreXml: String? = null,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            if (coreXml != null) {
                zip.putNextEntry(ZipEntry("docProps/core.xml"))
                zip.write(coreXml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun `docx разбирается в абзацы из w-p, w-t`() {
        val xml =
            """
            <?xml version="1.0"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>Первый абзац </w:t></w:r><w:r><w:t>продолжение.</w:t></w:r></w:p>
                <w:p><w:r><w:t>Второй абзац.</w:t></w:r></w:p>
                <w:p></w:p>
              </w:body>
            </w:document>
            """.trimIndent()
        val doc = DocParser().parseDocx(docx(xml))
        assertEquals(2, doc.blocks.size)
        val first = doc.blocks[0] as ContentBlock.Paragraph
        assertEquals("Первый абзац продолжение.", first.html)
    }

    @Test
    fun `docx берёт заголовок из core-xml`() {
        val xml =
            """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:p><w:r><w:t>Текст</w:t></w:r></w:p></w:body></w:document>"""
        val core =
            """<cp:coreProperties xmlns:cp="x" xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Моя книга</dc:title></cp:coreProperties>"""
        val doc = DocParser().parseDocx(docx(xml, core))
        assertEquals("Моя книга", doc.title)
    }

    @Test
    fun `doc эвристика достаёт читаемые фрагменты`() {
        // Имитация OLE2: служебные байты + читаемый текст cp1251 + мусор.
        val text = "Это достаточно длинный читаемый абзац из старого документа."
        val cp1251 = charset("windows-1251")
        val bytes =
            byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0, 1, 2, 3) +
                text.toByteArray(cp1251) +
                byteArrayOf(0, 0, 5, 6)
        val doc = DocParser().parseDoc(bytes)
        assertTrue(doc.blocks.isNotEmpty())
        val p = doc.blocks[0] as ContentBlock.Paragraph
        assertTrue(p.html.contains("читаемый абзац"))
    }
}
