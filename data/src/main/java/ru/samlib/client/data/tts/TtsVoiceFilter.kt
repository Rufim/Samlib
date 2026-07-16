package ru.samlib.client.data.tts

import java.util.Locale

/**
 * Совпадает ли локаль голоса с языком [languageTag] (BCP-47/ISO2, как отдаёт ML Kit
 * `translateTarget`, напр. «ru»/«en»). Движки по-разному кодируют язык: Google TTS часто
 * отдаёт ISO3 («rus»/«eng»), другие — ISO2 («ru»/«en»). Сравниваем оба кода в обе стороны,
 * иначе прямое `==` молча выбрасывает часть голосов языка.
 */
internal fun voiceMatchesLanguage(
    locale: Locale,
    languageTag: String,
): Boolean {
    val target = languageTag.trim().lowercase(Locale.ROOT)
    if (target.isEmpty()) return false
    val lang = locale.language.lowercase(Locale.ROOT)
    val targetIso3 = runCatching { Locale(target).isO3Language.lowercase(Locale.ROOT) }.getOrNull()
    val localeIso3 = runCatching { locale.isO3Language.lowercase(Locale.ROOT) }.getOrNull()
    return lang == target ||
        (targetIso3 != null && lang == targetIso3) ||
        (localeIso3 != null && (localeIso3 == target || localeIso3 == targetIso3))
}

/** Русская ли локаль голоса — частный случай [voiceMatchesLanguage] для языка оригинала. */
internal fun isRussianLocale(locale: Locale): Boolean = voiceMatchesLanguage(locale, "ru")
