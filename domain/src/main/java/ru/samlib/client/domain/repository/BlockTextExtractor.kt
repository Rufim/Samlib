package ru.samlib.client.domain.repository

import ru.samlib.client.domain.model.ContentBlock

/**
 * Текст блока без разметки (для TTS/полнотекстового поиска). Реализация обязана возвращать
 * тот же текст, что видит пользователь на экране (совпадение офсетов подсветки/озвучки) —
 * поэтому интерфейс живёт в `:domain`, а не unit конкретного парсера.
 */
interface BlockTextExtractor {
    fun plainText(block: ContentBlock): String?
}
