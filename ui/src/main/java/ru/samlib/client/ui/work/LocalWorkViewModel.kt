package ru.samlib.client.ui.work

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.model.TtsPhrase
import ru.samlib.client.domain.repository.LocalFileRepository
import ru.samlib.client.domain.repository.ReaderSettingsRepository
import ru.samlib.client.domain.repository.SavedBookInfo
import ru.samlib.client.domain.repository.SavedBooksRepository
import ru.samlib.client.domain.repository.Translator
import ru.samlib.client.domain.repository.TtsPlaybackControls
import ru.samlib.client.domain.repository.TtsSettings
import ru.samlib.client.domain.repository.TtsStartRequest
import ru.samlib.client.domain.repository.WorkRepository
import ru.samlib.client.domain.settings.LastBookPrefix
import ru.samlib.client.domain.settings.ReaderFont
import ru.samlib.client.domain.settings.ReaderTheme
import ru.samlib.client.domain.usecase.OpenExternallyUseCase
import java.io.File

class LocalWorkViewModel(
    private val uri: String,
    // индекс блока для перехода из поиска по книге (-1 = использовать сохранённую позицию)
    private val scrollTo: Int,
    private val localRepo: LocalFileRepository,
    private val workRepo: WorkRepository,
    private val settingsRepo: ReaderSettingsRepository,
    private val savedBooks: SavedBooksRepository,
    private val translator: Translator,
    private val openExternally: OpenExternallyUseCase,
    private val inBookSearchMemory: InBookSearchMemory,
    val ttsServiceLauncher: TtsServiceLauncher,
    private val player: TtsPlaybackControls,
) : ViewModel(),
    ContainerHost<WorkState, WorkSideEffect> {
    override val container = viewModelScope.container<WorkState, WorkSideEffect>(WorkState())

    /** Оригинальные блоки на время показа перевода. */
    private var originalBlocks: List<ContentBlock>? = null

    /** Устойчивый URI: content:// с разовым доступом заменяется на file://-копию в кеше. */
    private var effectiveUri: String = uri

    // Разовый переход из поиска: применяем только к первому успешному load(). Иначе повторный
    // вызов load() в рамках того же ViewModel снова прыгал бы на старый результат поиска,
    // «залипая» и стирая реальный прогресс чтения.
    private var pendingScrollTo = scrollTo

    // Фразы для TTS по текущим блокам — считаются в фоне один раз при загрузке/переводе, а не на
    // каждый тап «Озвучить» (regex-разбивка всей книги может занимать секунды на больших книгах).
    private var phrasesDeferred: Deferred<List<TtsPhrase>>? = null

    /** Отменяем предыдущий незавершённый старт озвучки — см. [TtsPlaybackControls.reserveStart]. */
    private var ttsStartJob: Job? = null

    private fun schedulePhraseSplit(blocks: List<ContentBlock>) {
        phrasesDeferred = viewModelScope.async(Dispatchers.Default) { ttsPhrases(blocks) }
    }

    /** URI открытого файла — для «Открыть в другой читалке» (после [load] указывает на локальную копию). */
    private fun sourceUri(): String = effectiveUri

    /** Открыть локальный файл в другой читалке (в оригинальном формате). */
    fun openExternal() =
        intent {
            runCatching { openExternally.localFile(sourceUri(), state.title, state.format) }
                .onFailure { postSideEffect(WorkSideEffect.OpenFailed) }
        }

    fun load(uri: String? = null) =
        intent {
            reduce { state.copy(loading = true, error = null) }
            try {
                effectiveUri = localRepo.ensureLocalCopy(uri ?: this@LocalWorkViewModel.uri)
                // авто-открытие при старте — только если файл переживёт перезапуск (file:// или SAF)
                if (effectiveUri.startsWith("${ContentResolver.SCHEME_FILE}:") ||
                    localRepo.hasPersistedAccess(effectiveUri)
                ) {
                    settingsRepo.setLastBook("${LastBookPrefix.LOCAL}$effectiveUri")
                }
                val doc = localRepo.load(effectiveUri)
                // переход из поиска по книге имеет приоритет над сохранённой позицией, но только
                // при первом успешном открытии — см. pendingScrollTo
                val start = if (pendingScrollTo >= 0) pendingScrollTo else workRepo.getReadingPosition(effectiveUri)
                pendingScrollTo = -1
                // Успешно открытую книгу заносим в «Сохранённые» с метаданными из тегов
                // и в общую историю (ключ — URI, маршрутизация открытия — по схеме URI).
                // Историю пишем ДО чтения pinned, чтобы строка уже существовала.
                if (doc.blocks.isNotEmpty()) {
                    runCatching {
                        savedBooks.record(
                            SavedBookInfo(
                                effectiveUri,
                                doc.title,
                                doc.author,
                                doc.format,
                                System.currentTimeMillis(),
                            ),
                        )
                    }
                    runCatching { workRepo.addLocalToHistory(effectiveUri, doc.title, doc.author) }
                }
                val pinned = runCatching { workRepo.isHistoryPinned(effectiveUri) }.getOrDefault(false)
                reduce {
                    state.copy(
                        loading = false,
                        title = doc.title,
                        format = doc.format,
                        blocks = doc.blocks.toUi(),
                        pinned = pinned,
                        startIndex = start.coerceIn(0, (doc.blocks.size - 1).coerceAtLeast(0)),
                        error = if (doc.blocks.isEmpty()) "Не удалось открыть файл" else null,
                        resolvedLink = effectiveUri,
                    )
                }
                schedulePhraseSplit(doc.blocks)
            } catch (e: Exception) {
                reduce { state.copy(loading = false, error = "Не удалось открыть файл") }
            }
        }

    /** openSheet-решение остаётся у ReaderScaffold — здесь только запуск озвучки движком. */
    fun startTts(
        fromBlock: Int,
        fromOffset: Int = 0,
    ) {
        val ticket = player.reserveStart()
        ttsStartJob?.cancel()
        ttsStartJob =
            viewModelScope.launch {
                val items = phrasesDeferred?.await().orEmpty()
                if (items.isEmpty()) return@launch
                intent {
                    player.start(
                        request =
                            TtsStartRequest(
                                title = state.title,
                                phrases = items,
                                blocks = state.blocks.toDomain(),
                                fromBlock = fromBlock,
                                fromOffset = fromOffset,
                                workLink = state.resolvedLink,
                                ticket = ticket,
                            ),
                        settings =
                            TtsSettings(
                                rate = state.settings.ttsRate,
                                pitch = state.settings.ttsPitch,
                                voice = state.settings.ttsVoice,
                                engine = state.settings.ttsEngine,
                                sleepTimerMinutes = state.settings.sleepTimerMinutes,
                            ),
                    )
                    ttsServiceLauncher.start()
                }
            }
    }

    fun observeSettings() =
        intent {
            settingsRepo.settings.collect { s -> reduce { state.copy(settings = s.toUi()) } }
        }

    /** Закрепить/открепить локальную книгу в истории (ключ — URI файла). */
    fun togglePin() =
        intent {
            val next = !state.pinned
            workRepo.setHistoryPinned(effectiveUri, next)
            reduce { state.copy(pinned = next) }
        }

    fun savePosition(index: Int) = intent { workRepo.saveReadingPosition(effectiveUri, index) }

    /** Перенести закешированную позицию в БД (onPause/уход с экрана — см. ReaderScaffold). */
    fun flushPosition() = intent { workRepo.flushReadingPosition(effectiveUri) }

    fun onLeave() =
        intent {
            settingsRepo.clearLastBook()
            // память поиска по книге не должна пережить сам выход из книги
            inBookSearchMemory.clear()
        }

    fun setFontSize(size: Int) = intent { settingsRepo.setFontSize(size) }

    fun setFont(font: ReaderFont) = intent { settingsRepo.setFont(font) }

    fun setMonoFont(font: ReaderFont) = intent { settingsRepo.setMonoFont(font) }

    fun setTheme(theme: ReaderTheme) = intent { settingsRepo.setTheme(theme) }

    fun setJustify(justify: Boolean) = intent { settingsRepo.setJustify(justify) }

    fun setBgColor(color: Int?) = intent { settingsRepo.setBgColor(color) }

    fun setTextColor(color: Int?) = intent { settingsRepo.setTextColor(color) }

    fun setHighlightColor(color: Int) = intent { settingsRepo.setHighlightColor(color) }

    fun setAutoScrollSpeed(speed: Int) = intent { settingsRepo.setAutoScrollSpeed(speed) }

    fun setTtsRate(rate: Float) = intent { settingsRepo.setTtsRate(rate) }

    fun setTtsPitch(pitch: Float) = intent { settingsRepo.setTtsPitch(pitch) }

    fun setTtsEngine(engine: String?) = intent { settingsRepo.setTtsEngine(engine) }

    fun setTtsVoice(voice: String?) = intent { settingsRepo.setTtsVoice(voice) }

    /** Языки, на которые умеет переводить ML Kit (BCP-47). */
    fun translateLanguages(): List<String> = translator.availableLanguages()

    fun setTranslateTarget(lang: String) =
        intent {
            settingsRepo.setTranslateTarget(lang)
            if (state.translated && !state.translating) {
                originalBlocks?.let { blocks ->
                    reduce {
                        state.copy(
                            blocks = blocks.toUi(),
                            translated = false,
                            sourceLang = null,
                        )
                    }
                    schedulePhraseSplit(blocks)
                }
                originalBlocks = null
                runTranslate(lang, fromIndex = 0)
            }
        }

    /** Быстрая проверка, требует ли URI разрешения на чтение хранилища. */
    fun needsStoragePermission(path: String): Boolean {
        if (!path.startsWith("${ContentResolver.SCHEME_FILE}://")) return false

        val uri = Uri.parse(path)
        val path = uri.path ?: return true
        val file = File(path)

        // Быстрые проверки
        if (!file.exists() || !file.canRead()) return true

        // Реальная попытка чтения
        return try {
            file.inputStream().use { it.read() }
            false
        } catch (e: Throwable) {
            Log.w("FileAccess", "SecurityException: нет прав для $path", e)
            true
        }
    }

    /** Включает перевод с видимой позиции [fromIndex] либо возвращает оригинал. */
    fun toggleTranslate(fromIndex: Int) =
        intent {
            if (state.translating) return@intent
            if (state.translated) {
                originalBlocks?.let { blocks ->
                    reduce {
                        state.copy(
                            blocks = blocks.toUi(),
                            translated = false,
                            sourceLang = null,
                        )
                    }
                    schedulePhraseSplit(blocks)
                }
                originalBlocks = null
                return@intent
            }
            runTranslate(state.settings.translateTarget, fromIndex)
        }

    private suspend fun Syntax<WorkState, WorkSideEffect>.runTranslate(
        target: String,
        fromIndex: Int,
    ) {
        // WorkTranslation работает с доменными ContentBlock — только они кладутся в TranslateResult/
        // originalBlocks, а не Ui-дубль из state.
        val blocks = state.blocks.toDomain()
        if (blocks.isEmpty()) return
        originalBlocks = blocks
        reduce { state.copy(translating = true, translateDone = 0, translateTotal = blocks.size) }
        val result =
            WorkTranslation.translate(translator, blocks, target, fromIndex) { partial, done ->
                // Видимый текст подменяется пакетами (см. UI_BATCH), счётчик прогресса — на каждый блок.
                if (done % WorkTranslation.UI_BATCH == 0 || done == blocks.size) {
                    reduce { state.copy(blocks = partial.toUi(), translated = true, translateDone = done) }
                } else {
                    reduce { state.copy(translateDone = done) }
                }
            }
        when (result) {
            is TranslateResult.Ok -> {
                reduce {
                    state.copy(
                        blocks = result.blocks.toUi(),
                        translated = true,
                        translating = false,
                        sourceLang = result.source,
                    )
                }
                schedulePhraseSplit(result.blocks)
            }

            is TranslateResult.Fail -> {
                originalBlocks?.let { orig ->
                    reduce {
                        state.copy(
                            blocks = orig.toUi(),
                            translated = false,
                            translating = false,
                        )
                    }
                    schedulePhraseSplit(orig)
                }
                originalBlocks = null
                postSideEffect(WorkSideEffect.Message(result.message))
            }
        }
    }
}
