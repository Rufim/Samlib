package ru.samlib.client.ui.work

import com.google.mlkit.common.MlKitException
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.repository.LanguagePair
import ru.samlib.client.domain.repository.Translator

/** Результат перевода блоков произведения. */
sealed interface TranslateResult {
    data class Ok(
        val blocks: List<ContentBlock>,
        val source: String,
    ) : TranslateResult

    data class Fail(
        val message: String,
    ) : TranslateResult
}

/**
 * Общая для сетевой и локальной читалок логика перевода блоков (ML Kit).
 * Чистая: не трогает состояние — определяет язык, переводит, возвращает результат.
 */
object WorkTranslation {
    /**
     * Каждый сколько переведённых блоков публиковать partial-список в состояние.
     * Reduce с новым списком блоков заставляет читалку пересчитывать оглавление
     * и LazyColumn — на каждый блок это O(n²) по всей книге.
     */
    const val UI_BATCH = 20

    /** Сколько первых непустых блоков брать на пробу для определения языка (ML Kit). */
    private const val DETECT_SAMPLE_BLOCKS = 20

    /** Проба текста для определения языка (ML Kit) — первые непустые блоки, не вся книга. */
    fun sampleText(blocks: List<ContentBlock>): String =
        blocks
            .asSequence()
            .map(::blockText)
            .filter { it.isNotBlank() }
            .take(DETECT_SAMPLE_BLOCKS)
            .joinToString(" ")

    /** plain-текст блока для перевода; ImageBlock/Divider → пусто. */
    fun blockText(b: ContentBlock): String =
        when (b) {
            is ContentBlock.Paragraph ->
                b.html
                    .replace(Regex("(?is)<[^>]+>"), " ")
                    .htmlUnescape()
                    .trim()

            is ContentBlock.Preformatted -> b.text
            is ContentBlock.Heading -> b.text
            is ContentBlock.ImageBlock -> ""
            ContentBlock.Divider -> ""
            // таблицы не переводим поблочно: одна строка перевода не раскладывается по ячейкам
            is ContentBlock.Table -> ""
        }

    /**
     * Определяет язык и переводит блоки на [target], начиная с [fromIndex] (видимый текст
     * первым), затем хвост и начало в фоне. [onProgress] получает частично переведённый
     * список и число готовых блоков — сразу после первых блоков и далее пачками, чтобы
     * видимый текст подменялся немедленно, а список не перерисовывался на каждый блок.
     */
    suspend fun translate(
        translator: Translator,
        blocks: List<ContentBlock>,
        target: String,
        fromIndex: Int = 0,
        onProgress: suspend (partial: List<ContentBlock>, done: Int) -> Unit = { _, _ -> },
    ): TranslateResult {
        val sample = sampleText(blocks)
        return try {
            val source = translator.detectLanguage(sample)
            when {
                source == null -> TranslateResult.Fail("Не удалось определить язык текста")
                source == target -> TranslateResult.Fail("Текст уже на языке перевода ($target)")
                else -> {
                    val start = fromIndex.coerceIn(0, (blocks.size - 1).coerceAtLeast(0))
                    // сначала от видимой позиции до конца, затем шапка файла
                    val order = (start until blocks.size) + (0 until start)
                    val current = blocks.toMutableList()
                    var done = 0
                    translator.translateStreaming(
                        blocks.map(::blockText),
                        LanguagePair(source, target),
                        order,
                    ) { i, t ->
                        current[i] = translatedBlock(blocks[i], t)
                        done++
                        // первые блоки — мгновенно на экран, дальше — каждые 10
                        if (done <= 10 || done % 10 == 0 || done == blocks.size) {
                            onProgress(current.toList(), done)
                        }
                    }
                    TranslateResult.Ok(current.toList(), source)
                }
            }
        } catch (e: MlKitException) {
            TranslateResult.Fail(mlKitErrorMessage(e))
        } catch (e: Exception) {
            TranslateResult.Fail("Перевод не удался: ${e.message ?: "ошибка ML Kit"}")
        }
    }

    /**
     * Человекочитаемое сообщение по коду ошибки ML Kit — вместо одного общего "ошибка ML Kit"
     * для всех случаев. В т.ч. накрывает устройства без Google Play Services (там перевод/
     * определение языка обычно падает с UNAVAILABLE при первом обращении к сервису).
     */
    private fun mlKitErrorMessage(e: MlKitException): String =
        "Перевод не удался: " +
            when (e.errorCode) {
                MlKitException.NETWORK_ISSUE -> "нет сети для загрузки модели перевода"
                MlKitException.NOT_ENOUGH_SPACE -> "не хватает места на устройстве для модели перевода"
                MlKitException.UNAVAILABLE, MlKitException.INTERNAL ->
                    "сервис перевода недоступен на этом устройстве " +
                        "(проверьте, установлены ли и обновлены ли Google Play Services)"
                MlKitException.MODEL_HASH_MISMATCH, MlKitException.MODEL_INCOMPATIBLE_WITH_TFLITE ->
                    "модель перевода повреждена или несовместима — попробуйте позже"
                else -> e.message ?: "ошибка ML Kit (код ${e.errorCode})"
            }

    private fun translatedBlock(
        b: ContentBlock,
        t: String,
    ): ContentBlock =
        when (b) {
            is ContentBlock.Paragraph -> if (t.isBlank()) b else ContentBlock.Paragraph(t.htmlEscape())
            is ContentBlock.Preformatted -> if (t.isBlank()) b else ContentBlock.Preformatted(t)
            is ContentBlock.Heading -> if (t.isBlank()) b else b.copy(text = t)
            is ContentBlock.ImageBlock -> b
            ContentBlock.Divider -> b
            is ContentBlock.Table -> b
        }

    private fun String.htmlUnescape(): String =
        this
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")

    private fun String.htmlEscape(): String =
        this
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}
