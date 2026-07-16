package ru.samlib.client.ui.work

import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.repository.BlockTextExtractor

/** [BlockTextExtractor] поверх [ttsText] — переедет в `:ui` вместе с Compose-рендерингом (шаг 6). */
class ComposeBlockTextExtractor : BlockTextExtractor {
    override fun plainText(block: ContentBlock): String? = block.ttsText()
}
