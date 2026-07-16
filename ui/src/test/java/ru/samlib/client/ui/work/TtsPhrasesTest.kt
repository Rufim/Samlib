package ru.samlib.client.ui.work

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.samlib.client.domain.model.ContentBlock

class TtsPhrasesTest {
    /** Режем текст одним блоком и возвращаем фразы как строки (по offset/length). */
    private fun phrases(text: String): List<String> =
        ttsPhrases(listOf(ContentBlock.Preformatted(text)))
            .map { text.substring(it.offset, it.offset + it.length) }

    @Test
    fun `обычные предложения разбиваются по знакам конца`() {
        assertEquals(
            listOf("Первое.", " Второе!", " Третье?"),
            phrases("Первое. Второе! Третье?"),
        )
    }

    @Test
    fun `десятичное число не рвёт предложение`() {
        assertEquals(
            listOf("Пи равно 3.14 и это важно."),
            phrases("Пи равно 3.14 и это важно."),
        )
    }

    @Test
    fun `дата с точками остаётся в одной фразе`() {
        assertEquals(
            listOf("Встреча 01.02.2020 состоится.", " Потом ещё."),
            phrases("Встреча 01.02.2020 состоится. Потом ещё."),
        )
    }

    @Test
    fun `точки внутри круглых скобок не рвут предложение`() {
        assertEquals(
            listOf("Он сказал (см. рис. 2) и ушёл."),
            phrases("Он сказал (см. рис. 2) и ушёл."),
        )
    }

    @Test
    fun `точки внутри квадратных скобок не рвут предложение`() {
        assertEquals(
            listOf("Текст [прим. ред.] продолжается."),
            phrases("Текст [прим. ред.] продолжается."),
        )
    }

    @Test
    fun `точки внутри фигурных скобок не рвут предложение`() {
        assertEquals(
            listOf("Код {ver. 2} работает."),
            phrases("Код {ver. 2} работает."),
        )
    }

    @Test
    fun `кавычки-ёлочки — разрыв только после закрывающей`() {
        assertEquals(
            listOf("Он сказал: «Привет. Пока.»", " И ушёл."),
            phrases("Он сказал: «Привет. Пока.» И ушёл."),
        )
    }

    @Test
    fun `восклицание внутри скобок не рвёт предложение`() {
        assertEquals(
            listOf("Она (вот это да!) промолчала."),
            phrases("Она (вот это да!) промолчала."),
        )
    }

    @Test
    fun `фразы покрывают весь текст без потерь`() {
        val text = "Раз (два. три) 4.5 конец! «Шесть. Семь.» Восемь."
        val parts = ttsPhrases(listOf(ContentBlock.Preformatted(text)))
        assertEquals(
            text,
            parts.joinToString("") { text.substring(it.offset, it.offset + it.length) },
        )
    }

    @Test
    fun `непарная скобка не склеивает блок бесконечно`() {
        // непарная «(» — защита скобок отключается после MAX_PHRASE, фразы остаются ограниченными
        val sentence = "Какое-то предложение средней длины для наполнения текста. "
        val text = "(" + sentence.repeat(30)
        val parts = phrases(text)
        assert(parts.size > 1) { "ожидалось несколько фраз, получено ${parts.size}" }
        assert(parts.all { it.length <= 800 }) { "фраза длиннее 800 символов" }
    }
}
