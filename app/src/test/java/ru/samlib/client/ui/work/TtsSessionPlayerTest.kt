package ru.samlib.client.ui.work

import android.os.Looper
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.samlib.client.domain.repository.TtsControls
import ru.samlib.client.domain.repository.TtsPlaybackState

private class FakeControls : TtsControls {
    override val state = MutableStateFlow(TtsPlaybackState())
    var resumed = 0
    var paused = 0
    var stopped = 0

    override fun resume() {
        resumed++
    }

    override fun pause() {
        paused++
    }

    override fun stop() {
        stopped++
    }
}

@RunWith(RobolectricTestRunner::class)
class TtsSessionPlayerTest {
    private fun player(controls: TtsControls): TtsSessionPlayer =
        TtsSessionPlayer(
            controls,
            Looper.getMainLooper(),
            CoroutineScope(Dispatchers.Unconfined),
            "Озвучка",
            "Остановка чтения в %1\$s",
        )

    @Test
    fun `состояние маппится из TtsPlaybackState`() {
        val c = FakeControls()
        c.state.value = TtsPlaybackState(active = true, playing = true, title = "Книга")
        val p = player(c)
        p.prime() // до прайминга плеер сообщает STATE_IDLE (для корректного постинга уведомления)
        assertEquals(Player.STATE_READY, p.playbackState)
        assertTrue(p.playWhenReady)
    }

    @Test
    fun `команды делегируются в TtsControls`() {
        val c = FakeControls()
        c.state.value = TtsPlaybackState(active = true, playing = true, title = "Книга")
        val p = player(c)
        p.pause()
        assertEquals(1, c.paused)
        p.play()
        assertEquals(1, c.resumed)
        p.stop()
        assertEquals(1, c.stopped)
    }

    @Test
    fun `время таймера сна попадает в метаданные (artist)`() {
        val c = FakeControls()
        c.state.value =
            TtsPlaybackState(
                active = true,
                playing = true,
                title = "Книга",
                sleepUntilMillis = 5 * 60_000L,
            )
        val p = player(c)
        p.prime()
        val artist = p.mediaMetadata.artist?.toString()
        assertTrue(
            "artist=$artist",
            artist != null && Regex("""Остановка чтения в \d\d:\d\d""").matches(artist),
        )
    }

    @Test
    fun `без таймера сна artist пуст`() {
        val c = FakeControls()
        c.state.value = TtsPlaybackState(active = true, playing = true, title = "Книга")
        val p = player(c)
        p.prime()
        assertEquals(null, p.mediaMetadata.artist)
    }
}
