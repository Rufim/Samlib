package ru.samlib.client.ui.work

import androidx.compose.runtime.Immutable
import ru.samlib.client.domain.model.ContentBlock

/** Стабильная для Compose-рекомпозиции копия [ContentBlock] — держит домен без зависимости от Compose. */
@Immutable
sealed interface UiContentBlock {
    data class Paragraph(
        val html: String,
    ) : UiContentBlock

    data class Preformatted(
        val text: String,
    ) : UiContentBlock

    data class Heading(
        val level: Int,
        val text: String,
    ) : UiContentBlock

    data class ImageBlock(
        val url: String,
    ) : UiContentBlock

    data object Divider : UiContentBlock

    data class Table(
        val rows: List<List<String>>,
    ) : UiContentBlock
}

fun ContentBlock.toUi(): UiContentBlock =
    when (this) {
        is ContentBlock.Paragraph -> UiContentBlock.Paragraph(html)
        is ContentBlock.Preformatted -> UiContentBlock.Preformatted(text)
        is ContentBlock.Heading -> UiContentBlock.Heading(level, text)
        is ContentBlock.ImageBlock -> UiContentBlock.ImageBlock(url)
        ContentBlock.Divider -> UiContentBlock.Divider
        is ContentBlock.Table -> UiContentBlock.Table(rows)
    }

fun UiContentBlock.toDomain(): ContentBlock =
    when (this) {
        is UiContentBlock.Paragraph -> ContentBlock.Paragraph(html)
        is UiContentBlock.Preformatted -> ContentBlock.Preformatted(text)
        is UiContentBlock.Heading -> ContentBlock.Heading(level, text)
        is UiContentBlock.ImageBlock -> ContentBlock.ImageBlock(url)
        UiContentBlock.Divider -> ContentBlock.Divider
        is UiContentBlock.Table -> ContentBlock.Table(rows)
    }

fun List<ContentBlock>.toUi(): List<UiContentBlock> = map { it.toUi() }

fun List<UiContentBlock>.toDomain(): List<ContentBlock> = map { it.toDomain() }
