package ru.samlib.client.ui.work

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.samlib.client.domain.model.ContentBlock
import ru.samlib.client.domain.model.TtsPhrase
import ru.samlib.client.domain.settings.ReaderFont
import ru.samlib.client.ui.common.htmlToAnnotatedString
import ru.samlib.client.ui.theme.AppColors

/**
 * Текст блока для озвучки (TTS); неозвучиваемые блоки → null.
 * Для абзаца используется тот же текст, что отображается (htmlToAnnotatedString),
 * чтобы офсеты onRangeStart совпадали с подсветкой на экране (без trim).
 */
fun ContentBlock.ttsText(): String? =
    when (this) {
        is ContentBlock.Paragraph -> htmlToAnnotatedString(html).text.ifBlank { null }
        is ContentBlock.Preformatted -> text.ifBlank { null }
        is ContentBlock.Heading -> text.ifBlank { null }
        is ContentBlock.Table ->
            rows
                .joinToString(". ") { row ->
                    row.joinToString(", ") { htmlToAnnotatedString(it).text }
                }.ifBlank { null }

        else -> null
    }

// Как в legacy: конец предложения — [.!?;…]+ + возможная закрывающая кавычка/скобка.
private val SENTENCE_END = Regex("[.!?;…]+[>\"»)\\]}]?")
private const val MAX_PHRASE = 800

// Парные скобки/кавычки: пока открыта хоть одна — знак внутри не считается концом предложения.
private const val OPEN_BRACKETS = "([{<«"
private const val CLOSE_BRACKETS = ")]}>»"

/** Разбивка текста блока на предложения с сохранением офсетов (для подсветки). */
private fun splitSentences(text: String): List<Pair<Int, String>> {
    val out = ArrayList<Pair<Int, String>>()

    fun addCapped(
        offset: Int,
        seg: String,
    ) {
        var o = offset
        var s = seg
        while (s.length > MAX_PHRASE) {
            out.add(o to s.substring(0, MAX_PHRASE))
            o += MAX_PHRASE
            s = s.substring(MAX_PHRASE)
        }
        if (s.isNotEmpty()) out.add(o to s)
    }

    var start = 0
    var depth = 0
    var scanned = 0

    // Доучитывает скобки в text[scanned until to]; вызывается лениво по мере прохода.
    fun advanceDepth(to: Int) {
        while (scanned < to) {
            val c = text[scanned]
            if (c in OPEN_BRACKETS) {
                depth++
            } else if (c in CLOSE_BRACKETS && depth > 0) {
                depth--
            }
            scanned++
        }
    }
    for (m in SENTENCE_END.findAll(text)) {
        val end = m.range.last + 1
        // Точка между цифрами — число или дата (3.14, 01.02.2020), не конец предложения.
        if (m.value == "." &&
            m.range.first > 0 &&
            text[m.range.first - 1].isDigit() &&
            end < text.length &&
            text[end].isDigit()
        ) {
            continue
        }
        advanceDepth(end)
        // Внутри незакрытых скобок/кавычек не рвём; защита снимается на непарной скобке,
        // когда фраза разрослась до MAX_PHRASE — иначе весь блок склеился бы в одну.
        if (depth > 0 && end - start < MAX_PHRASE) continue
        // Знак + закрывающая скобка посреди предложения — «(вот это да!) промолчала»:
        // строчная буква дальше значит, что предложение продолжается.
        if (m.value.last() !in ".!?;…") {
            var j = end
            while (j < text.length && text[j].isWhitespace()) j++
            if (j < text.length && text[j].isLowerCase()) continue
        }
        if (end > start) {
            addCapped(start, text.substring(start, end))
            start = end
        }
    }
    if (start < text.length) addCapped(start, text.substring(start))
    return out
}

/** Разбивает озвучиваемые блоки на предложения-фразы для TTS. */
fun ttsPhrases(blocks: List<ContentBlock>): List<TtsPhrase> {
    val out = ArrayList<TtsPhrase>()
    blocks.forEachIndexed { i, block ->
        val text = block.ttsText() ?: return@forEachIndexed
        for ((offset, seg) in splitSentences(text)) {
            if (seg.isNotBlank()) out.add(TtsPhrase(i, offset, seg.length))
        }
    }
    return out
}

/**
 * Смещение (px) для listState.animateScrollBy(), чтобы читаемая TTS-фраза — от верха первой
 * строки до низа последней (фраза может занимать несколько строк) — целиком поместилась
 * в видимую область; null — уже помещается, скролл не нужен.
 * Выравнивание всегда идёт по ВЕРХУ фразы (а не фиксированная страница вниз): иначе после
 * скролла фраза может оказаться выше верхней границы экрана и пропасть из виду.
 */
fun ttsAutoScrollDelta(
    phraseTop: Float,
    phraseBottom: Float,
    viewportStart: Float,
    viewportEnd: Float,
    margin: Float,
): Float? {
    val overflowsBottom = phraseBottom > viewportEnd - margin
    val overflowsTop = phraseTop < viewportStart
    return if (overflowsBottom || overflowsTop) phraseTop - viewportStart else null
}

private fun AnnotatedString.highlighted(
    start: Int,
    end: Int,
    color: Color,
): AnnotatedString {
    if (start < 0 || end <= start) return this
    val s = start.coerceIn(0, length)
    val e = end.coerceIn(s, length)
    if (e <= s) return this
    return buildAnnotatedString {
        append(this@highlighted)
        addStyle(SpanStyle(background = color), s, e)
    }
}

@Composable
fun ReaderFont.rememberFamily(): FontFamily {
    val asset = this.asset
    return if (asset == null) {
        when (this) {
            ReaderFont.SANS -> FontFamily.SansSerif
            ReaderFont.MONO -> FontFamily.Monospace
            else -> FontFamily.Serif
        }
    } else {
        val context = LocalContext.current
        remember(asset) {
            runCatching { FontFamily(Font(asset, context.assets)) }.getOrDefault(FontFamily.Serif)
        }
    }
}

/**
 * Тап по тексту: возвращает символьный офсет; ссылку (если есть) отдаёт listener'у.
 * pointerInput заключаем на [anno]/[linkListener] (меняются лишь при смене текста), а НЕ на
 * [onWordTap]: тап сам вызывает рекомпозицию (смена состояния озвучки) → новая лямбда →
 * pointerInput перезапускался бы и повторно обрабатывал то же касание (кратный start).
 * [onWordTap] обязан быть СТАБИЛЬНОЙ ссылкой (rememberUpdatedState в BlockView).
 */
private fun Modifier.tapWord(
    layout: () -> TextLayoutResult?,
    anno: AnnotatedString?,
    linkListener: LinkInteractionListener?,
    onWordTap: (Int) -> Unit,
): Modifier =
    this.pointerInput(anno, linkListener) {
        detectTapGestures { pos ->
            val l = layout() ?: return@detectTapGestures
            val offset = l.getOffsetForPosition(pos).coerceIn(0, (anno?.length ?: Int.MAX_VALUE))
            val link =
                anno?.getLinkAnnotations(offset, offset)?.firstOrNull()?.item as? LinkAnnotation.Url
            if (link != null && linkListener != null) linkListener.onClick(link) else onWordTap(offset)
        }
    }

/** Параметры отрисовки текста блока читалки. */
@Immutable
data class ReaderTextStyle(
    val fontSizeSp: Int, // размер шрифта, sp
    val font: ReaderFont, // основной шрифт
    val monoFont: ReaderFont = ReaderFont.MONO, // шрифт для <pre> (моноширинный)
    val textColor: Color, // цвет текста
    val justify: Boolean = false, // выравнивание по ширине
)

/** Подсветка озвучиваемого TTS слова/фразы; по умолчанию — нет подсветки. */
@Immutable
data class WordHighlight(
    val wordStart: Int = -1, // начало диапазона (символы); -1 — нет
    val wordEnd: Int = -1, // конец диапазона; -1 — нет
    val color: Color = AppColors.TtsHighlightDefault, // цвет подсветки
)

@Composable
fun BlockView(
    block: UiContentBlock,
    style: ReaderTextStyle,
    highlight: WordHighlight = WordHighlight(),
    linkListener: LinkInteractionListener? = null,
    onWordTap: (Int) -> Unit = {},
    // раскладка активного (озвучиваемого) блока — для расчёта позиции фразы при скролле
    onLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    val family = style.font.rememberFamily()
    var layout by remember(block) { mutableStateOf<TextLayoutResult?>(null) }
    // Стабильная ссылка на onWordTap: pointerInput не должен перезапускаться при рекомпозиции.
    val latestOnWordTap by rememberUpdatedState(onWordTap)
    val stableOnWordTap = remember { { offset: Int -> latestOnWordTap(offset) } }
    when (block) {
        is UiContentBlock.Paragraph -> {
            val anno = remember(block.html) { htmlToAnnotatedString(block.html, linkListener) }
            Text(
                text = anno.highlighted(highlight.wordStart, highlight.wordEnd, highlight.color),
                fontFamily = family,
                fontSize = style.fontSizeSp.sp,
                color = style.textColor,
                textAlign = if (style.justify) TextAlign.Justify else TextAlign.Start,
                onTextLayout = {
                    layout = it
                    onLayout?.invoke(it)
                },
                modifier =
                    Modifier
                        // vertical = 6.dp читает ReaderScaffold (padPx) при расчёте
                        // автоскролла TTS — менять синхронно
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .tapWord({ layout }, anno, linkListener, stableOnWordTap),
            )
        }

        is UiContentBlock.Preformatted ->
            Text(
                text =
                    AnnotatedString(block.text).highlighted(
                        highlight.wordStart,
                        highlight.wordEnd,
                        highlight.color,
                    ),
                // pre сохраняет выравнивание пробелами → отдельный моноширинный шрифт
                fontFamily = style.monoFont.rememberFamily(),
                fontSize = style.fontSizeSp.sp,
                color = style.textColor,
                onTextLayout = {
                    layout = it
                    onLayout?.invoke(it)
                },
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .tapWord({ layout }, null, null, stableOnWordTap),
            )

        is UiContentBlock.Heading ->
            Text(
                text =
                    AnnotatedString(block.text).highlighted(
                        highlight.wordStart,
                        highlight.wordEnd,
                        highlight.color,
                    ),
                fontFamily = family,
                fontWeight = FontWeight.Bold,
                fontSize = (style.fontSizeSp + 4).sp,
                color = style.textColor,
                onTextLayout = { layout = it },
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .tapWord({ layout }, null, null, stableOnWordTap),
            )

        is UiContentBlock.ImageBlock ->
            AsyncImage(
                model = block.url,
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

        UiContentBlock.Divider -> HorizontalDivider(Modifier.padding(16.dp))
        is UiContentBlock.Table -> TableView(block, style, family, linkListener)
    }
}

/** Таблица: равноширинные ячейки с рамкой; ссылки внутри ячеек работают. */
@Composable
private fun TableView(
    block: UiContentBlock.Table,
    style: ReaderTextStyle,
    family: FontFamily,
    linkListener: LinkInteractionListener?,
) {
    val textColor = style.textColor
    val borderColor = textColor.copy(alpha = 0.4f)
    val columns = block.rows.maxOf { it.size }
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, borderColor),
    ) {
        block.rows.forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) HorizontalDivider(color = borderColor)
            Row(Modifier.height(IntrinsicSize.Min)) {
                for (col in 0 until columns) {
                    if (col > 0) {
                        VerticalDivider(Modifier.fillMaxHeight(), color = borderColor)
                    }
                    val html = row.getOrNull(col).orEmpty()
                    Text(
                        text = remember(html) { htmlToAnnotatedString(html, linkListener) },
                        fontFamily = family,
                        fontSize = style.fontSizeSp.sp,
                        color = textColor,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
