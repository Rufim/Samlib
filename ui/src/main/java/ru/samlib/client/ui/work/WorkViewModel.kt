package ru.samlib.client.ui.work

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
import ru.samlib.client.domain.repository.AuthorRepository
import ru.samlib.client.domain.repository.ReaderSettingsRepository
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.Translator
import ru.samlib.client.domain.repository.TtsPlaybackControls
import ru.samlib.client.domain.repository.TtsSettings
import ru.samlib.client.domain.repository.TtsStartRequest
import ru.samlib.client.domain.repository.WorkRepository
import ru.samlib.client.domain.repository.errorText
import ru.samlib.client.domain.settings.LastBookPrefix
import ru.samlib.client.domain.settings.ReaderFont
import ru.samlib.client.domain.settings.ReaderTheme
import ru.samlib.client.domain.usecase.DownloadFb2UseCase
import ru.samlib.client.domain.usecase.LoadWorkUseCase
import ru.samlib.client.domain.usecase.OpenExternallyUseCase
import ru.samlib.client.domain.usecase.SaveWorkUseCase

class WorkViewModel(
    private val link: String,
    // индекс блока для перехода из поиска по книге (-1 = использовать сохранённую позицию)
    private val scrollTo: Int,
    private val workRepo: WorkRepository,
    private val settingsRepo: ReaderSettingsRepository,
    private val translator: Translator,
    private val authorRepo: AuthorRepository,
    private val loadWork: LoadWorkUseCase,
    private val saveWork: SaveWorkUseCase,
    private val openExternally: OpenExternallyUseCase,
    private val downloadFb2: DownloadFb2UseCase,
    private val inBookSearchMemory: InBookSearchMemory,
    val ttsServiceLauncher: TtsServiceLauncher,
    private val player: TtsPlaybackControls,
) : ViewModel(),
    ContainerHost<WorkState, WorkSideEffect> {
    override val container = viewModelScope.container<WorkState, WorkSideEffect>(WorkState())

    /** Оригинальные блоки на время показа перевода. */
    private var originalBlocks: List<ContentBlock>? = null

    // Разовый переход из поиска: применяем только к первому успешному load(). Иначе повторный
    // вызов load() в рамках того же ViewModel (Retry, возврат на экран без пересоздания VM)
    // снова прыгал бы на старый результат поиска, «залипая» и стирая реальный прогресс чтения.
    private var pendingScrollTo = scrollTo

    // Фразы для TTS по текущим блокам — считаются в фоне один раз при загрузке/переводе, а не на
    // каждый тап «Озвучить» (regex-разбивка всей книги может занимать секунды на больших книгах).
    private var phrasesDeferred: Deferred<List<TtsPhrase>>? = null

    /** Отменяем предыдущий незавершённый старт озвучки — см. [TtsPlaybackControls.reserveStart]. */
    private var ttsStartJob: Job? = null

    private fun schedulePhraseSplit(blocks: List<ContentBlock>) {
        phrasesDeferred = viewModelScope.async(Dispatchers.Default) { ttsPhrases(blocks) }
    }

    fun load() =
        intent {
            // запоминаем как последнюю открытую книгу — для авто-открытия при следующем старте
            settingsRepo.setLastBook("${LastBookPrefix.WORK}$link")
            reduce { state.copy(loading = true, error = null) }
            when (val result = loadWork(link, pendingScrollTo)) {
                is SamlibResult.Success -> {
                    pendingScrollTo = -1
                    val d = result.data
                    reduce {
                        state.copy(
                            loading = false,
                            title = d.title,
                            blocks = d.blocks.toUi(),
                            // rawContent намеренно не держим в памяти (у больших книг это лишняя копия);
                            // для «Сохранить»/«Открыть внешне» берём из кеша через currentRawContent()
                            pinned = d.pinned,
                            startIndex = d.startIndex,
                            error = if (d.blocks.isEmpty()) "Не удалось загрузить текст" else null,
                        )
                    }
                    schedulePhraseSplit(d.blocks)
                }

                is SamlibResult.Error ->
                    reduce {
                        state.copy(loading = false, error = result.type.errorText())
                    }
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
                                workLink = link,
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

    fun savePosition(index: Int) =
        intent {
            workRepo.saveReadingPosition(link, index)
        }

    /** Перенести закешированную позицию в БД (onPause/уход с экрана — см. ReaderScaffold). */
    fun flushPosition() = intent { workRepo.flushReadingPosition(link) }

    /** Выход из книги (кнопка «назад») — сбрасываем авто-открытие при следующем старте. */
    fun onLeave() =
        intent {
            settingsRepo.clearLastBook()
            // память поиска по книге не должна пережить сам выход из книги
            inBookSearchMemory.clear()
        }

    /**
     * Сырое содержимое книги для «Сохранить»/«Открыть в другой читалке»: берём из кеша,
     * а если его нет — перезагружаем. В памяти постоянно не держим.
     */
    private suspend fun currentRawContent(): String {
        workRepo.cachedRawContent(link)?.takeIf { it.isNotBlank() }?.let { return it }
        return (workRepo.getWork(link) as? SamlibResult.Success)?.data?.rawContent ?: ""
    }

    /** Сохранить книгу в «Загрузки». */
    fun save() =
        intent {
            saveWork(SaveWorkUseCase.Params(state.title, currentRawContent())).fold(
                { postSideEffect(WorkSideEffect.Saved(it)) },
                { postSideEffect(WorkSideEffect.SaveFailed) },
            )
        }

    /** Открыть книгу в другой читалке. */
    fun openExternal() =
        intent {
            runCatching { openExternally(state.title, currentRawContent()) }
                .onFailure { postSideEffect(WorkSideEffect.OpenFailed) }
        }

    /** Поставить в очередь скачивание FB2-архива книги. */
    fun downloadFb2() =
        intent {
            val ok = downloadFb2(DownloadFb2UseCase.Params(link, state.title)).isSuccess
            postSideEffect(if (ok) WorkSideEffect.Fb2Downloading else WorkSideEffect.Fb2Failed)
        }

    fun setFontSize(size: Int) = intent { settingsRepo.setFontSize(size) }

    fun setFont(font: ReaderFont) = intent { settingsRepo.setFont(font) }

    fun setMonoFont(font: ReaderFont) = intent { settingsRepo.setMonoFont(font) }

    fun setTheme(theme: ReaderTheme) = intent { settingsRepo.setTheme(theme) }

    fun togglePin() =
        intent {
            val next = !state.pinned
            workRepo.setHistoryPinned(link, next)
            reduce { state.copy(pinned = next) }
        }

    /** Каталог автора книги: "/x/xxx/kniga.shtml" → "/x/xxx/". */
    private fun workDir(): String = link.substringBeforeLast('/', "") + "/"

    fun observeObserved() =
        intent {
            authorRepo.observeWorkObserved(link, workDir()).collect { observed ->
                reduce { state.copy(observed = observed) }
            }
        }

    fun toggleObserveWork() = intent { authorRepo.toggleObserveWork(link, workDir(), state.title) }

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
            // перевод уже показан → перевести заново на новый язык
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
