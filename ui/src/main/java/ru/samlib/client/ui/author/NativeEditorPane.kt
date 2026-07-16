package ru.samlib.client.ui.author

import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import ru.samlib.client.ui.common.LoadingBox
import ru.samlib.client.ui.common.ReaderScrollbar

/**
 * Нативный EditText в NestedScrollView (Compose-поле тормозит на объёмных текстах).
 * Монтируется один раз, как только готов [precomputedText], дальше скрывается
 * ([editorActive] = false → height(0.dp)), а не пересоздаётся — переливка Editable
 * на большом тексте дорога.
 */
@Composable
fun BoxScope.NativeEditorPane(
    initialText: String,
    editorBackground: Int,
    precomputedText: PrecomputedTextCompat?,
    precomputing: Boolean,
    pendingOffset: Int,
    editorActive: Boolean,
    editCanScroll: Boolean,
    editScrollProgress: Float,
    onEditorCreated: (EditText) -> Unit,
    onScrollHostCreated: (NestedScrollView) -> Unit,
    onReleased: (NestedScrollView) -> Unit,
    onScrollChanged: () -> Unit,
    onTapPercent: () -> Unit,
    onScrollToFraction: (Float) -> Unit,
) {
    if (precomputing) {
        LoadingBox(Modifier.fillMaxSize())
        return
    }
    AndroidView(
        factory = { ctx ->
            val editText =
                EditText(ctx).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                    isSingleLine = false
                    gravity = Gravity.TOP or Gravity.START
                    setHorizontallyScrolling(false)
                    setBackgroundColor(editorBackground)
                    inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    if (precomputedText != null) {
                        TextViewCompat.setPrecomputedText(
                            this,
                            precomputedText,
                        )
                    } else {
                        setText(initialText)
                    }
                    onEditorCreated(this)
                }
            // EditText больше не скроллит себя (нет fling) — скролл/инерцию
            // даёт NestedScrollView, EditText растёт по высоте контента.
            NestedScrollView(ctx).apply {
                setBackgroundColor(editorBackground)
                clipChildren = true
                clipToPadding = true
                isFillViewport = true
                addView(editText)
                onScrollHostCreated(this)
                setOnScrollChangeListener { _: NestedScrollView, _: Int, _: Int, _: Int, _: Int ->
                    onScrollChanged()
                }
                editText.post {
                    val layout = editText.layout ?: return@post
                    val safeOffset = pendingOffset.coerceIn(0, editText.text.length)
                    val line = layout.getLineForOffset(safeOffset)
                    scrollTo(0, layout.getLineTop(line))
                    onScrollChanged()
                }
            }
        },
        onRelease = { released -> onReleased(released) },
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (editorActive) Modifier.fillMaxHeight() else Modifier.height(0.dp))
                .padding(horizontal = 8.dp)
                .clipToBounds(),
    )
    if (editorActive && editCanScroll) {
        ReaderScrollbar(
            progress = editScrollProgress,
            color = MaterialTheme.colorScheme.onSurface,
            onTapPercent = onTapPercent,
            onScrollToFraction = onScrollToFraction,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}
