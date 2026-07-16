package ru.samlib.client.domain.repository

/** Пара языков перевода (коды TranslateLanguage / BCP-47). */
data class LanguagePair(
    val source: String, // исходный язык
    val target: String, // язык перевода
)

/** Он-девайс перевод: определение языка + перевод текста. */
interface Translator {
    /** BCP-47 код языка текста («en», «ru», …) или null, если не определён. */
    suspend fun detectLanguage(text: String): String?

    /**
     * Переводит [texts] с [langs].source на [langs].target. [onProgress] — индекс
     * завершённого элемента. Возвращает переводы в исходном порядке; для пустых строк —
     * пустые строки.
     */
    suspend fun translate(
        texts: List<String>,
        langs: LanguagePair,
        onProgress: suspend (index: Int, translated: String) -> Unit = { _, _ -> },
    ): List<String>

    /**
     * Потоковый перевод: элементы [texts] переводятся в порядке [indices], каждый готовый
     * блок отдаётся в [onTranslated]. Пустые строки не переводятся.
     */
    suspend fun translateStreaming(
        texts: List<String>,
        langs: LanguagePair,
        indices: List<Int>,
        onTranslated: suspend (index: Int, translated: String) -> Unit,
    )

    /** Языки, доступные для перевода (BCP-47 коды). */
    fun availableLanguages(): List<String>
}
