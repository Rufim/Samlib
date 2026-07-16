package ru.samlib.client.data.parser

import android.util.Base64
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.repository.WorkContentParser
import java.util.zip.ZipInputStream

/** Разбор EPUB (zip: OPF → spine из XHTML-глав) в блоки читалки. Текст глав по порядку. */
class EpubParser(
    private val contentParser: WorkContentParser = DefaultWorkContentParser(),
) {
    data class EpubDocument(
        val title: String?,
        val author: String?,
        val blocks: List<ContentBlock>,
    )

    fun parse(bytes: ByteArray): EpubDocument {
        val entries = readZip(bytes)
        val containerXml =
            entries["META-INF/container.xml"]?.toString(Charsets.UTF_8)
                ?: throw ParseException("EPUB: нет container.xml")
        val opfPath =
            Jsoup
                .parse(containerXml, "", Parser.xmlParser())
                .selectFirst("rootfile")
                ?.attr("full-path")
                ?: throw ParseException("EPUB: не найден OPF")
        val opfBytes = entries[opfPath] ?: throw ParseException("EPUB: OPF не читается")
        val opf = Jsoup.parse(opfBytes.toString(Charsets.UTF_8), "", Parser.xmlParser())

        val metadata = opf.selectFirst("metadata")
        val title =
            metadata
                ?.selectFirst("title")
                ?.text()
                ?.trim()
                ?.ifBlank { null }
        val author =
            metadata
                ?.selectFirst("creator")
                ?.text()
                ?.trim()
                ?.ifBlank { null }
        // manifest: id → href
        val manifest = opf.select("manifest > item").associate { it.attr("id") to it.attr("href") }
        val baseDir = opfPath.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }

        val blocks = mutableListOf<ContentBlock>()
        val spine = opf.select("spine > itemref")
        for (ref in spine) {
            val href = manifest[ref.attr("idref")] ?: continue
            val path = normalize(baseDir + href)
            val chapterBytes = entries[path] ?: continue
            val chapterDir =
                path.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }
            val html = Jsoup.parse(chapterBytes.toString(Charsets.UTF_8))
            // картинки главы → data-uri из ZIP (относительные пути внутри архива)
            html.select("img[src], image").forEach { img ->
                val attr = if (img.hasAttr("src")) "src" else "xlink:href"
                val rel = img.attr(attr)
                if (rel.isNotBlank() && !rel.startsWith("data:")) {
                    val imgPath = normalize(chapterDir + rel.substringBefore('#'))
                    entries[imgPath]?.let { imgBytes ->
                        val mime = mimeOf(imgPath)
                        val b64 = Base64.encodeToString(imgBytes, Base64.NO_WRAP)
                        img.attr("src", "data:$mime;base64,$b64")
                    }
                }
            }
            // Заголовок главы для оглавления: если в теле нет своего <h1..6>, берём <title> из head.
            val bodyHasHeading = html.body().selectFirst("h1, h2, h3, h4, h5, h6") != null
            if (!bodyHasHeading) {
                html.selectFirst("title")?.text()?.trim()?.ifBlank { null }?.let {
                    blocks.add(ContentBlock.Heading(2, it))
                }
            }
            contentParser.parse(html.body().html()).forEach { blocks.add(it) }
        }
        return EpubDocument(title, author, blocks)
    }

    private fun mimeOf(path: String): String =
        when (path.substringAfterLast('.').lowercase()) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }

    private fun normalize(path: String): String {
        val parts = ArrayDeque<String>()
        path.split('/').forEach { p ->
            when (p) {
                "", "." -> {}
                ".." -> if (parts.isNotEmpty()) parts.removeLast()
                else -> parts.addLast(p)
            }
        }
        return parts.joinToString("/")
    }

    private fun readZip(bytes: ByteArray): Map<String, ByteArray> {
        val map = LinkedHashMap<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) map[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }
        return map
    }
}
