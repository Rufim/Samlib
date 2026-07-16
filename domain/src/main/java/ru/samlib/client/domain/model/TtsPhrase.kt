package ru.samlib.client.domain.model

/**
 * Фраза для TTS без копии текста: только координаты (блок + смещение/длина в plain-тексте блока).
 * Сам текст режется на лету из блока — иначе на большой книге хранилась бы вторая копия всего текста.
 */
data class TtsPhrase(
    val blockIndex: Int,
    val offset: Int,
    val length: Int,
)
