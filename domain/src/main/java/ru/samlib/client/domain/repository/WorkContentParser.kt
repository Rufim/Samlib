package ru.samlib.client.domain.repository

import ru.samlib.client.domain.model.ContentBlock

/** Разбор сырого HTML произведения в блоки читалки. */
interface WorkContentParser {
    fun parse(rawHtml: String): List<ContentBlock>
}
