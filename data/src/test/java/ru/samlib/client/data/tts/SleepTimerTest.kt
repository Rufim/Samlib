package ru.samlib.client.data.tts

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerTest {
    @Test
    fun `срабатывает по истечении минут`() =
        runTest {
            val timer = SleepTimer(this)
            var fired = 0
            timer.schedule(1) { fired++ }
            advanceTimeBy(59_000)
            assertEquals(0, fired)
            advanceTimeBy(2_000)
            assertEquals(1, fired)
        }

    @Test
    fun `reset откладывает срабатывание`() =
        runTest {
            val timer = SleepTimer(this)
            var fired = 0
            timer.schedule(1) { fired++ }
            advanceTimeBy(50_000)
            timer.reset()
            advanceTimeBy(50_000) // суммарно 100с, но после reset прошло только 50с
            assertEquals(0, fired)
            advanceTimeBy(11_000)
            assertEquals(1, fired)
        }

    @Test
    fun `cancel отменяет`() =
        runTest {
            val timer = SleepTimer(this)
            var fired = 0
            timer.schedule(1) { fired++ }
            timer.cancel()
            advanceTimeBy(120_000)
            assertEquals(0, fired)
        }

    @Test
    fun `ноль минут не запускает таймер`() =
        runTest {
            val timer = SleepTimer(this)
            var fired = 0
            timer.schedule(0) { fired++ }
            advanceTimeBy(600_000)
            assertEquals(0, fired)
        }
}
