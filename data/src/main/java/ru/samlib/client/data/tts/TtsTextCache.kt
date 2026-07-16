package ru.samlib.client.data.tts

import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.model.TtsPhrase
import ru.samlib.client.domain.repository.BlockTextExtractor

/** Кэш plain-текста текущего блока: [BlockTextExtractor] парсит HTML, не гоняем его на каждую фразу. */
class TtsTextCache(
    private val textExtractor: BlockTextExtractor,
) {
    private var blocks: List<ContentBlock> = emptyList()
    private var memoBlockIdx = -1
    private var memoPlain = ""

    /** Блоки книги (общая ссылка, без копии) — из них режем текст фраз на лету. */
    fun setBlocks(blocks: List<ContentBlock>) {
        this.blocks = blocks
        memoBlockIdx = -1
        memoPlain = ""
    }

    fun clear() = setBlocks(emptyList())

    private fun blockPlain(blockIndex: Int): String {
        if (blockIndex != memoBlockIdx) {
            memoPlain = blocks.getOrNull(blockIndex)?.let { textExtractor.plainText(it) }.orEmpty()
            memoBlockIdx = blockIndex
        }
        return memoPlain
    }

    /** Текст фразы, вырезанный из блока по координатам (без хранения копии). */
    fun phraseText(p: TtsPhrase): String {
        val plain = blockPlain(p.blockIndex)
        val start = p.offset.coerceIn(0, plain.length)
        val end = (p.offset + p.length).coerceIn(start, plain.length)
        return plain.substring(start, end)
    }
}
