package ru.samlib.client.data.translate

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.samlib.client.domain.repository.LanguagePair
import ru.samlib.client.domain.repository.Translator
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Он-девайс перевод через ML Kit: определение языка + перевод (модели качаются при первом использовании). */
class DefaultTranslator : Translator {
    private suspend fun <T> Task<T>.await(): T =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { if (cont.isActive) cont.resume(it) }
            addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
            addOnCanceledListener { cont.cancel() }
        }

    override suspend fun detectLanguage(text: String): String? {
        val client = LanguageIdentification.getClient()
        return try {
            client.identifyLanguage(text.take(2000)).await().takeIf { it != "und" }
        } finally {
            client.close()
        }
    }

    override suspend fun translate(
        texts: List<String>,
        langs: LanguagePair,
        onProgress: suspend (index: Int, translated: String) -> Unit,
    ): List<String> {
        val src =
            TranslateLanguage.fromLanguageTag(langs.source)
                ?: throw IllegalArgumentException("Перевод с языка «${langs.source}» не поддерживается")
        val dst =
            TranslateLanguage.fromLanguageTag(langs.target)
                ?: throw IllegalArgumentException("Перевод на язык «${langs.target}» не поддерживается")
        val translator =
            Translation.getClient(
                TranslatorOptions
                    .Builder()
                    .setSourceLanguage(src)
                    .setTargetLanguage(dst)
                    .build(),
            )
        return try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            texts.mapIndexed { i, text ->
                val translated = if (text.isBlank()) "" else translator.translate(text).await()
                onProgress(i, translated)
                translated
            }
        } finally {
            translator.close()
        }
    }

    override suspend fun translateStreaming(
        texts: List<String>,
        langs: LanguagePair,
        indices: List<Int>,
        onTranslated: suspend (index: Int, translated: String) -> Unit,
    ) {
        val src =
            TranslateLanguage.fromLanguageTag(langs.source)
                ?: throw IllegalArgumentException("Перевод с языка «${langs.source}» не поддерживается")
        val dst =
            TranslateLanguage.fromLanguageTag(langs.target)
                ?: throw IllegalArgumentException("Перевод на язык «${langs.target}» не поддерживается")
        val translator =
            Translation.getClient(
                TranslatorOptions
                    .Builder()
                    .setSourceLanguage(src)
                    .setTargetLanguage(dst)
                    .build(),
            )
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            for (i in indices) {
                val text = texts[i]
                onTranslated(i, if (text.isBlank()) "" else translator.translate(text).await())
            }
        } finally {
            translator.close()
        }
    }

    override fun availableLanguages(): List<String> = TranslateLanguage.getAllLanguages()
}
