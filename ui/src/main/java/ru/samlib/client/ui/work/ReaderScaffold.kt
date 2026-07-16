package ru.samlib.client.ui.work

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import ru.samlib.client.domain.repository.TtsPlaybackControls
import ru.samlib.client.domain.settings.ReaderFont
import ru.samlib.client.domain.settings.ReaderTheme
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.TextPositionDialog
import ru.samlib.client.ui.theme.colors
import java.util.Locale

/** Шаг таймера автопрокрутки — кадр ~60 fps. */
private const val AUTO_SCROLL_FRAME_MS = 16L

/** Коэффициент перевода настройки скорости (квадратичной) в пиксели за кадр. */
private const val AUTO_SCROLL_SPEED_FACTOR = 0.12f

/** Скроллбар: показ при открытии и пауза простоя перед скрытием. */
private const val SCROLLBAR_FIRST_SHOW_MS = 5_000L
private const val SCROLLBAR_HIDE_DELAY_MS = 1_400L

/**
 * Действия читалки — единый канал вместо ~20 колбэков. Команды — `data object`,
 * действия с данными — `data class`. Видимость пунктов меню задаётся отдельно
 * ([ReaderScaffold] параметр `menu`), а не наличием колбэка.
 */
sealed interface ReaderAction {
    data object Back : ReaderAction

    data object Retry : ReaderAction

    data object TogglePin : ReaderAction

    data object ToggleObserve : ReaderAction

    data object GoAuthor : ReaderAction

    data object Save : ReaderAction

    data object OpenExternal : ReaderAction

    data object OpenSettings : ReaderAction

    data object OpenIllustrations : ReaderAction

    /** [fromIndex] — текущая позиция чтения на момент открытия поиска (для показа находок дальше по тексту). */
    data class OpenSearch(
        val fromIndex: Int,
    ) : ReaderAction

    data object DownloadFb2 : ReaderAction

    data class ToggleTranslate(
        val fromIndex: Int,
    ) : ReaderAction // индекс первого видимого блока

    data class SavePosition(
        val index: Int,
    ) : ReaderAction // индекс блока для сохранения позиции

    /** Перенести закешированную позицию в БД — на уход с экрана (onPause/навигация). */
    data object FlushPosition : ReaderAction

    data class SetFontSize(
        val sp: Int,
    ) : ReaderAction

    data class SetFont(
        val font: ReaderFont,
    ) : ReaderAction

    data class SetMonoFont(
        val font: ReaderFont,
    ) : ReaderAction

    data class SetTheme(
        val theme: ReaderTheme,
    ) : ReaderAction

    data class SetJustify(
        val value: Boolean,
    ) : ReaderAction

    data class SetBgColor(
        val color: Int?,
    ) : ReaderAction

    data class SetTextColor(
        val color: Int?,
    ) : ReaderAction

    data class SetAutoScrollSpeed(
        val speed: Int,
    ) : ReaderAction

    data class SetTtsRate(
        val rate: Float,
    ) : ReaderAction

    data class SetTtsPitch(
        val pitch: Float,
    ) : ReaderAction

    /** Запустить озвучку с [fromBlock]/[fromOffset] — тяжёлая часть (разбивка на фразы,
     * движок) выполняется во ViewModel, см. WorkViewModel/LocalWorkViewModel.startTts. */
    data class StartTts(
        val fromBlock: Int,
        val fromOffset: Int = 0,
    ) : ReaderAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScaffold(
    state: WorkState,
    // видимость пунктов меню-«троеточия»: у локальной книги большинство скрыто
    menu: ReaderMenuVisibility,
    onAction: (ReaderAction) -> Unit,
    linkListener: LinkInteractionListener? = null,
) {
    var showTextSheet by remember { mutableStateOf(false) }
    var showTtsSheet by remember { mutableStateOf(false) }
    var showAutoScrollSheet by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }
    var showPercentDialog by remember { mutableStateOf(false) }
    var showTitleDialog by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    // Экран не гаснет, пока открыта читалка (снимается при уходе с экрана).
    val keepScreenView = LocalView.current
    DisposableEffect(Unit) {
        keepScreenView.keepScreenOn = true
        onDispose { keepScreenView.keepScreenOn = false }
    }
    // Оглавление из блоков-заголовков.
    val chapters =
        remember(state.blocks) {
            state.blocks.mapIndexedNotNull { i, b ->
                if (b is UiContentBlock.Heading) i to b.text else null
            }
        }
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = koinInject<TtsPlaybackControls>()

    // Реактивное сохранение позиции (скролл/озвучка) пишет только в in-memory кеш репозитория —
    // перенос в БД гарантируем явно на onPause (сворачивание) и на уход с экрана (навигация/назад).
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) onAction(ReaderAction.FlushPosition)
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onAction(ReaderAction.FlushPosition)
        }
    }

    val ttsState by player.state.collectAsState()
    val ttsActive = ttsState.active
    val ttsPlaying = ttsState.playing
    val speakBlock = ttsState.blockIndex
    val wStart = ttsState.wordStart
    val wEnd = ttsState.wordEnd

    // Раскладка активного (озвучиваемого) блока — чтобы знать пиксельную позицию фразы.
    var activeLayout by remember(ttsState.blockIndex) { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    // Скролл при озвучке: подсвеченная фраза (может занимать несколько строк) должна
    // целиком оставаться видимой; см. ttsAutoScrollDelta — выравнивание по верху фразы,
    // а не фиксированная страница, иначе фраза может проскочить выше верхней границы.
    LaunchedEffect(ttsState.blockIndex, ttsState.wordStart, ttsState.active) {
        if (!ttsState.active || ttsState.blockIndex < 0) return@LaunchedEffect
        val info = listState.layoutInfo
        val viewportStart = info.viewportStartOffset.toFloat()
        val viewportEnd = info.viewportEndOffset.toFloat()
        val item = info.visibleItemsInfo.firstOrNull { it.index == ttsState.blockIndex }
        val layout = activeLayout
        when {
            // активный блок вне экрана (перемотка/прыжок) — подтянуть к верху
            item == null -> listState.scrollToItem(ttsState.blockIndex)
            layout != null && ttsState.wordStart in 0..layout.layoutInput.text.length -> {
                // ВАЖНО: 6.dp должен совпадать с vertical padding абзаца в BlockViews
                // (Modifier.padding(vertical = 6.dp)) — иначе расчёт строки для
                // автоскролла TTS будет промахиваться мимо реальной позиции.
                val padPx = with(density) { 6.dp.toPx() }
                val textLen = layout.layoutInput.text.length
                val lastIndex = (textLen - 1).coerceAtLeast(0)
                val startOffset = ttsState.wordStart.coerceIn(0, lastIndex)
                val endOffset = ttsState.wordEnd.coerceIn(startOffset, lastIndex)
                val startLine = layout.getLineForOffset(startOffset)
                val endLine = layout.getLineForOffset(endOffset)
                // верх первой и низ последней строки фразы в координатах списка
                val phraseTop = item.offset + padPx + layout.getLineTop(startLine)
                val phraseBottom = item.offset + padPx + layout.getLineBottom(endLine)
                val lineHeight = layout.getLineBottom(startLine) - layout.getLineTop(startLine)
                val delta =
                    ttsAutoScrollDelta(
                        phraseTop,
                        phraseBottom,
                        viewportStart,
                        viewportEnd,
                        lineHeight * 0.5f,
                    )
                if (delta != null) listState.animateScrollBy(delta)
            }
        }
    }

    // Смена таймера остановки чтения в настройках во время активной озвучки перепланирует
    // уже идущий отсчёт (иначе новое значение подхватилось бы только при следующем запуске).
    LaunchedEffect(state.settings.sleepTimerMinutes) {
        player.setSleepTimer(state.settings.sleepTimerMinutes)
    }

    // Пока начальная позиция не восстановлена, сохранять НЕЛЬЗЯ: иначе стартовый
    // firstVisibleItemIndex=0 затрёт сохранённую позицию до того, как мы к ней прокрутим.
    // rememberSaveable (не remember!): уход на другой экран (например, в настройки) и возврат
    // пересоздают composable, но state.startIndex в ViewModel остаётся СТАРЫМ (тем самым, с
    // которого читали в самый первый раз — например, результатом поиска) — если бы флаг не
    // переживал этот цикл, эффект ниже заново скроллил бы туда поверх уже восстановленного
    // rememberSaveable-скролла listState, «залипая» на устаревшей позиции.
    var positionRestored by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.startIndex, state.blocks.size) {
        if (!positionRestored && state.blocks.isNotEmpty()) {
            if (state.startIndex > 0) listState.scrollToItem(state.startIndex)
            positionRestored = true
        }
    }
    LaunchedEffect(autoScroll, state.settings.autoScrollSpeed) {
        if (autoScroll) {
            // Нелинейная скорость: на малых значениях очень медленно, дальше — резко быстрее.
            val s = state.settings.autoScrollSpeed
            val step = AUTO_SCROLL_SPEED_FACTOR * s * s
            while (autoScroll && listState.canScrollForward) {
                listState.scrollBy(step)
                delay(AUTO_SCROLL_FRAME_MS)
            }
            autoScroll = false
        }
    }
    LaunchedEffect(listState, positionRestored) {
        if (!positionRestored) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }.collect { idx ->
            if (player.state.value.active) {
                // Любой скролл (в т.ч. автоскролл TTS) сбрасывает таймер сна — но только пока
                // экран виден. В фоне (после onPause) скролл не продлевает таймер, иначе он
                // никогда не досчитал бы до конца во время фоновой озвучки.
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    player.resetSleepTimer()
                }
            } else {
                onAction(ReaderAction.SavePosition(idx))
            }
        }
    }

    // Прогресс чтения по нижней границе экрана: 0% пока не оторвались от верха,
    // 100% когда виден конец текста (на коротких текстах покажет 100 сразу).
    val progress by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            when {
                info.totalItemsCount == 0 -> 0f
                !listState.canScrollBackward -> 0f
                !listState.canScrollForward -> 1f
                else -> {
                    val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                    ((lastVisible + 1).toFloat() / info.totalItemsCount).coerceIn(0f, 1f)
                }
            }
        }
    }
    // Скролл-бар нужен только если текст не помещается на экране.
    val canScroll by remember {
        derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
    }

    // Перемотка к доле [f] с точностью до сотых процента: доля → индекс блока + внутриблочный
    // сдвиг по средней высоте видимых блоков.
    fun scrollToFraction(f: Float) {
        val info = listState.layoutInfo
        val total = info.totalItemsCount
        if (total <= 0) return
        val exact = f.coerceIn(0f, 1f) * (total - 1)
        val index = exact.toInt().coerceIn(0, total - 1)
        val avgH =
            info.visibleItemsInfo
                .takeIf { it.isNotEmpty() }
                ?.map { it.size }
                ?.average() ?: 0.0
        val offset = ((exact - index) * avgH).toInt()
        scope.launch { listState.scrollToItem(index, offset) }
    }
    // Показывается при открытии (если текст не влезает) минимум 5с и при прокрутке (fade),
    // после чего прячется через паузу простоя (1.4с).
    var scrollbarVisible by remember { mutableStateOf(false) }
    LaunchedEffect(positionRestored) {
        if (!positionRestored) return@LaunchedEffect
        var firstShow = true
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest {
                scrollbarVisible = true
                val hideDelay = if (firstShow) SCROLLBAR_FIRST_SHOW_MS else SCROLLBAR_HIDE_DELAY_MS
                firstShow = false
                delay(hideDelay)
                scrollbarVisible = false
            }
    }

    val dark = isSystemInDarkTheme()
    val (themeBg, themeFg) = state.settings.theme.colors(dark)
    val bg = state.settings.bgColor?.let { Color(it) } ?: themeBg
    val fg = state.settings.textColor?.let { Color(it) } ?: themeFg

    Scaffold(
        containerColor = bg,
        topBar = {
            ReaderTopBar(
                state =
                    ReaderTopBarState(
                        title = state.title,
                        hasChapters = chapters.size >= 2,
                        pinned = state.pinned,
                        observed = state.observed,
                        translate =
                            TranslateStatus(
                                translating = state.translating,
                                done = state.translateDone,
                                total = state.translateTotal,
                                translated = state.translated,
                                targetName = languageName(state.settings.translateTarget),
                            ),
                        menu = menu,
                    ),
                onBack = { onAction(ReaderAction.Back) },
                onTitleClick = { showTitleDialog = true },
                // Если поиск открыли сразу после захода в книгу, listState ещё может не успеть
                // доскроллить до восстановленной позиции (positionRestored=false) — тогда
                // firstVisibleItemIndex==0 и «текущая позиция» подсунула бы начало книги.
                // Пока не восстановлено — берём state.startIndex напрямую.
                onOpenSearch = {
                    val fromIndex = if (positionRestored) listState.firstVisibleItemIndex else state.startIndex
                    onAction(ReaderAction.OpenSearch(fromIndex))
                },
                onOpenChapters = { showChapters = true },
                onOpenSettings = { onAction(ReaderAction.OpenSettings) },
                onMenuAction = { action ->
                    when (action) {
                        ReaderMenuAction.TogglePin -> onAction(ReaderAction.TogglePin)
                        ReaderMenuAction.GoAuthor -> onAction(ReaderAction.GoAuthor)
                        ReaderMenuAction.ToggleObserve -> onAction(ReaderAction.ToggleObserve)
                        ReaderMenuAction.OpenIllustrations -> onAction(ReaderAction.OpenIllustrations)
                        ReaderMenuAction.DownloadFb2 -> onAction(ReaderAction.DownloadFb2)
                        ReaderMenuAction.Save -> onAction(ReaderAction.Save)
                        ReaderMenuAction.OpenExternal -> onAction(ReaderAction.OpenExternal)
                        ReaderMenuAction.ToggleTranslate ->
                            onAction(
                                ReaderAction.ToggleTranslate(
                                    listState.firstVisibleItemIndex,
                                ),
                            )

                        ReaderMenuAction.StartAutoScroll -> {
                            autoScroll = true
                            showAutoScrollSheet = true
                        }

                        ReaderMenuAction.StartSpeak -> {
                            onAction(ReaderAction.StartTts(listState.firstVisibleItemIndex))
                            showTtsSheet = true
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ReaderBody(
                state = state,
                onRetry = { onAction(ReaderAction.Retry) },
                listState = listState,
                bg = bg,
                fg = fg,
                linkListener = linkListener,
                ttsActive = ttsActive,
                ttsPlaying = ttsPlaying,
                speakBlock = speakBlock,
                wordStart = wStart,
                wordEnd = wEnd,
                onWordTap = { index, offset ->
                    if (ttsPlaying) {
                        player.stopPlayback()
                    } else {
                        onAction(ReaderAction.StartTts(index, offset))
                    }
                },
                onActiveLayout = { activeLayout = it },
                progress = progress,
                canScroll = canScroll,
                scrollbarVisible = scrollbarVisible,
                onTapPercent = { showPercentDialog = true },
                onScrollToFraction = { f -> scrollToFraction(f) },
                autoScroll = autoScroll,
                onOpenTtsSheet = { showTtsSheet = true },
                onOpenAutoScrollSheet = { showAutoScrollSheet = true },
                onOpenTextSheet = { showTextSheet = true },
            )
        }
    }

    if (showChapters) {
        ChaptersSheet(
            chapters = chapters,
            onDismiss = { showChapters = false },
            onSelect = { index ->
                showChapters = false
                scope.launch { listState.scrollToItem(index) }
            },
        )
    }

    if (showTitleDialog) {
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showTitleDialog = false
                }) { Text(stringResource(R.string.reader_close)) }
            },
            text = { Text(state.title ?: stringResource(R.string.reader_title_fallback)) },
        )
    }

    if (showPercentDialog) {
        TextPositionDialog(
            currentFraction = progress,
            onDismiss = { showPercentDialog = false },
            onGo = { f ->
                showPercentDialog = false
                scrollToFraction(f)
            },
        )
    }

    if (showTextSheet) {
        ReaderSettingsSheet(
            settings = state.settings,
            onAction = onAction,
            onDismiss = { showTextSheet = false },
        )
    }

    if (showTtsSheet && ttsActive) {
        TtsControlSheet(
            playing = ttsPlaying,
            rate = state.settings.ttsRate,
            pitch = state.settings.ttsPitch,
            onDismiss = { showTtsSheet = false },
            onPlayPause = { if (ttsPlaying) player.pause() else player.resume() },
            onStop = {
                player.stop()
                showTtsSheet = false
            },
            onRateChange = {
                onAction(ReaderAction.SetTtsRate(it))
                player.setRate(it)
            },
            onRateChangeFinished = { player.applySpeechSettings() },
            onPitchChange = {
                onAction(ReaderAction.SetTtsPitch(it))
                player.setPitch(it)
            },
            onPitchChangeFinished = { player.applySpeechSettings() },
        )
    }

    if (showAutoScrollSheet && autoScroll) {
        AutoScrollSheet(
            speed = state.settings.autoScrollSpeed,
            onDismiss = { showAutoScrollSheet = false },
            onStop = {
                autoScroll = false
                showAutoScrollSheet = false
            },
            onSpeedChange = { onAction(ReaderAction.SetAutoScrollSpeed(it)) },
        )
    }
}

/** Название языка по BCP-47 коду на русском (приложение русскоязычное, не зависит от локали системы). */
private fun languageName(code: String): String = Locale(code).getDisplayLanguage(Locale("ru")).ifBlank { code }
