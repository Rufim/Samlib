package ru.samlib.client.domain.repository

import ru.samlib.client.domain.model.ContentBlock

/** Один блок превью черновика: распарсенный [ContentBlock] и диапазон символов в исходном тексте. */
data class DraftBlock(
    val block: ContentBlock,
    val rawRange: IntRange,
)

/** Режет сырой текст черновика на куски по границам переносов строк для превью в редакторе. */
interface WorkDraftChunker {
    fun chunk(text: String): List<DraftBlock>
}
