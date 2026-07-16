package ru.samlib.client.ui.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsAutoScrollTest {
    @Test
    fun `фраза целиком помещается — скролл не нужен`() {
        // строка фразы 100..120, вьюпорт 0..800, до низа далеко
        assertNull(
            ttsAutoScrollDelta(
                phraseTop = 100f,
                phraseBottom = 120f,
                viewportStart = 0f,
                viewportEnd = 800f,
                margin = 10f,
            ),
        )
    }

    @Test
    fun `однострочная фраза у нижнего края — скролл выравнивает её верх по верху экрана`() {
        // низ фразы (795) залезает в margin-зону перед низом вьюпорта (800 - 10 = 790)
        val delta =
            ttsAutoScrollDelta(
                phraseTop = 780f,
                phraseBottom = 795f,
                viewportStart = 0f,
                viewportEnd = 800f,
                margin = 10f,
            )
        assertEquals(780f, delta)
    }

    @Test
    fun `многострочная фраза — триггер по низу последней строки, а не по верху первой`() {
        // верх фразы (300) далеко от низа экрана, но фраза длинная и её низ (850) уже вышел за вьюпорт (0..800)
        // старая логика (только по верху первой строки) это бы пропустила
        val delta =
            ttsAutoScrollDelta(
                phraseTop = 300f,
                phraseBottom = 850f,
                viewportStart = 0f,
                viewportEnd = 800f,
                margin = 10f,
            )
        assertEquals(300f, delta)
    }

    @Test
    fun `после применения дельты верх фразы точно совпадает с верхом вьюпорта — без перелёта наверх`() {
        val phraseTop = 780f
        val delta =
            ttsAutoScrollDelta(
                phraseTop = phraseTop,
                phraseBottom = 795f,
                viewportStart = 0f,
                viewportEnd = 800f,
                margin = 10f,
            )
        requireNotNull(delta)
        // новая позиция верха строки после скролла = phraseTop - delta
        assertEquals(0f, phraseTop - delta)
    }

    @Test
    fun `фраза выше вьюпорта (перемотка назад) — тоже подтягивается к верху`() {
        val delta =
            ttsAutoScrollDelta(
                phraseTop = -50f,
                phraseBottom = -20f,
                viewportStart = 0f,
                viewportEnd = 800f,
                margin = 10f,
            )
        assertEquals(-50f, delta)
    }
}
