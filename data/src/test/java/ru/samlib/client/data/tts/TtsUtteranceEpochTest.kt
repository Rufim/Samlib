package ru.samlib.client.data.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsUtteranceEpochTest {
    @Test
    fun `format отдаёт id с текущей эпохой и позицией`() {
        val epoch = TtsUtteranceEpoch()
        epoch.bump() // эпоха 1
        assertEquals("u1.5", epoch.format(5))
    }

    @Test
    fun `parse возвращает pos для актуальной эпохи`() {
        val epoch = TtsUtteranceEpoch()
        epoch.bump() // эпоха 1
        val id = epoch.format(3)
        assertEquals(3, epoch.parse(id))
    }

    @Test
    fun `parse возвращает null для устаревшей эпохи`() {
        val epoch = TtsUtteranceEpoch()
        epoch.bump() // эпоха 1
        val staleId = epoch.format(3) // "u1.3"
        epoch.bump() // эпоха 2 — как будто случился pause()
        assertNull(epoch.parse(staleId))
    }

    @Test
    fun `parse возвращает null для некорректного id`() {
        val epoch = TtsUtteranceEpoch()
        epoch.bump()
        assertNull(epoch.parse(null))
        assertNull(epoch.parse("u1")) // нет точки
        assertNull(epoch.parse("u1.abc")) // pos не число
        assertNull(epoch.parse("uX.3")) // epoch не число
    }
}
