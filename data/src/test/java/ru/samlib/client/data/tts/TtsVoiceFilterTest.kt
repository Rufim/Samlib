package ru.samlib.client.data.tts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class TtsVoiceFilterTest {
    @Test
    fun `ISO2 ru — русская локаль`() {
        assertTrue(isRussianLocale(Locale("ru")))
    }

    @Test
    fun `ISO2 ru-RU — русская локаль`() {
        assertTrue(isRussianLocale(Locale("ru", "RU")))
    }

    @Test
    fun `ISO3 rus — русская локаль (как отдаёт Google TTS)`() {
        assertTrue(isRussianLocale(Locale("rus", "RUS")))
    }

    @Test
    fun `language tag ru-RU — русская локаль`() {
        assertTrue(isRussianLocale(Locale.forLanguageTag("ru-RU")))
    }

    @Test
    fun `английская локаль — не русская`() {
        assertFalse(isRussianLocale(Locale.ENGLISH))
    }

    @Test
    fun `ISO3 eng — не русская`() {
        assertFalse(isRussianLocale(Locale("eng", "USA")))
    }

    @Test
    fun `voiceMatchesLanguage — английская локаль совпадает с en`() {
        assertTrue(voiceMatchesLanguage(Locale.ENGLISH, "en"))
    }

    @Test
    fun `voiceMatchesLanguage — ISO3 eng совпадает с en`() {
        assertTrue(voiceMatchesLanguage(Locale("eng", "USA"), "en"))
    }

    @Test
    fun `voiceMatchesLanguage — французская локаль не совпадает с en`() {
        assertFalse(voiceMatchesLanguage(Locale.FRENCH, "en"))
    }

    @Test
    fun `voiceMatchesLanguage — пустой код языка не совпадает ни с чем`() {
        assertFalse(voiceMatchesLanguage(Locale.ENGLISH, ""))
    }
}
