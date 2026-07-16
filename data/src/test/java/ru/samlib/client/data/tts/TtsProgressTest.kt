package ru.samlib.client.data.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class TtsProgressTest {
    @Test
    fun `сохраняет только при смене блока`() {
        val saved = mutableListOf<Pair<String, Int>>()
        val tracker = TtsProgressTracker("/a/b.shtml") { link, block -> saved += link to block }

        var last = -1
        last = tracker.onBlock(0, last) // новый блок 0
        last = tracker.onBlock(0, last) // тот же блок — не сохраняем
        last = tracker.onBlock(1, last) // новый блок 1
        last = tracker.onBlock(1, last) // тот же — нет

        assertEquals(listOf("/a/b.shtml" to 0, "/a/b.shtml" to 1), saved)
    }

    @Test
    fun `без ссылки ничего не сохраняет`() {
        val saved = mutableListOf<Pair<String, Int>>()
        val tracker = TtsProgressTracker(null) { link, block -> saved += link to block }
        var last = -1
        last = tracker.onBlock(0, last)
        last = tracker.onBlock(1, last)
        assertEquals(emptyList<Pair<String, Int>>(), saved)
    }
}
