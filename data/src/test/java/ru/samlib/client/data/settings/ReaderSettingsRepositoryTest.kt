package ru.samlib.client.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.samlib.client.domain.settings.ReaderFont
import ru.samlib.client.domain.settings.ReaderTheme
import java.io.File

class ReaderSettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun store(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { File(tmp.newFolder(), "reader.preferences_pb") },
        )

    @Test
    fun `значения по умолчанию`() =
        runTest {
            val repo = DefaultReaderSettingsRepository(store())
            val s = repo.settings.first()
            assertEquals(18, s.fontSizeSp)
            assertEquals(ReaderFont.SERIF, s.font)
            assertEquals(ReaderTheme.SYSTEM, s.theme)
        }

    @Test
    fun `сохранение и чтение настроек`() =
        runTest {
            val repo = DefaultReaderSettingsRepository(store())
            repo.setFontSize(24)
            repo.setFont(ReaderFont.MONO)
            repo.setTheme(ReaderTheme.SEPIA)
            val s = repo.settings.first()
            assertEquals(24, s.fontSizeSp)
            assertEquals(ReaderFont.MONO, s.font)
            assertEquals(ReaderTheme.SEPIA, s.theme)
        }

    @Test
    fun `сохранение выравнивания и цветов`() =
        runTest {
            val repo = DefaultReaderSettingsRepository(store())
            repo.setJustify(true)
            repo.setFont(ReaderFont.GEORGIA)
            repo.setBgColor(0xFF102030.toInt())
            repo.setTextColor(0xFFAABBCC.toInt())
            val s = repo.settings.first()
            assertEquals(true, s.justify)
            assertEquals(ReaderFont.GEORGIA, s.font)
            assertEquals(0xFF102030.toInt(), s.bgColor)
            assertEquals(0xFFAABBCC.toInt(), s.textColor)
        }

    @Test
    fun `сброс цвета возвращает null`() =
        runTest {
            val repo = DefaultReaderSettingsRepository(store())
            repo.setBgColor(0xFF010203.toInt())
            assertEquals(0xFF010203.toInt(), repo.settings.first().bgColor)
            repo.setBgColor(null)
            assertEquals(null, repo.settings.first().bgColor)
        }

    @Test
    fun `сохранение настроек озвучки`() =
        runTest {
            val repo = DefaultReaderSettingsRepository(store())
            repo.setTtsRate(1.5f)
            repo.setTtsPitch(0.8f)
            repo.setTtsEngine("com.google.tts")
            repo.setTtsVoice("ru-ru-x-1")
            val s = repo.settings.first()
            assertEquals(1.5f, s.ttsRate)
            assertEquals(0.8f, s.ttsPitch)
            assertEquals("com.google.tts", s.ttsEngine)
            assertEquals("ru-ru-x-1", s.ttsVoice)
        }

    @Test
    fun `sleepTimerMinutes сохраняется и ограничивается диапазоном 0-90`() =
        runTest {
            val repo = DefaultReaderSettingsRepository(store())
            assertEquals(0, repo.settings.first().sleepTimerMinutes)
            repo.setSleepTimer(30)
            assertEquals(30, repo.settings.first().sleepTimerMinutes)
            repo.setSleepTimer(200)
            assertEquals(90, repo.settings.first().sleepTimerMinutes)
            repo.setSleepTimer(-5)
            assertEquals(0, repo.settings.first().sleepTimerMinutes)
        }

    @Test
    fun `размер шрифта ограничен диапазоном 12-30`() =
        runTest {
            val repo = DefaultReaderSettingsRepository(store())
            repo.setFontSize(999)
            assertEquals(30, repo.settings.first().fontSizeSp)
            repo.setFontSize(1)
            assertEquals(12, repo.settings.first().fontSizeSp)
        }

    @Test
    fun `язык голоса по умолчанию русский и сохраняется`() =
        runTest {
            val repo = DefaultReaderSettingsRepository(store())
            assertEquals("ru", repo.settings.first().voiceLanguage)
            repo.setVoiceLanguage("en")
            assertEquals("en", repo.settings.first().voiceLanguage)
        }

    @Test
    fun `язык-источник перевода по умолчанию авто (null) и сохраняется`() =
        runTest {
            val repo = DefaultReaderSettingsRepository(store())
            assertEquals(null, repo.settings.first().translateSource)
            repo.setTranslateSource("en")
            assertEquals("en", repo.settings.first().translateSource)
            repo.setTranslateSource(null)
            assertEquals(null, repo.settings.first().translateSource)
        }

    @Test
    fun `запись лога в файл по умолчанию выключена и сохраняется`() =
        runTest {
            val repo = DefaultReaderSettingsRepository(store())
            assertEquals(false, repo.fileLogging.first())
            repo.setFileLogging(true)
            assertEquals(true, repo.fileLogging.first())
            repo.setFileLogging(false)
            assertEquals(false, repo.fileLogging.first())
        }
}
