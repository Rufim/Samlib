package ru.samlib.client.domain.model

/**
 * Модель данных, без Compose-зависимости. Стабильная для рекомпозиции копия —
 * `ru.samlib.client.ui.work.UiContentBlock` (`:ui`), маппинг — в State ViewModel'ей читалки.
 */
sealed interface ContentBlock {
    data class Paragraph(
        val html: String,
    ) : ContentBlock

    data class Preformatted(
        val text: String,
    ) : ContentBlock

    data class Heading(
        val level: Int,
        val text: String,
    ) : ContentBlock

    data class ImageBlock(
        val url: String,
    ) : ContentBlock

    data object Divider : ContentBlock

    /** Таблица: строки → ячейки с inline-HTML (th парсер оборачивает в <b>). */
    data class Table(
        val rows: List<List<String>>,
    ) : ContentBlock
}
