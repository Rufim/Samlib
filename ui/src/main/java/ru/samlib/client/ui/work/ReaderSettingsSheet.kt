package ru.samlib.client.ui.work

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Быстрая шторка по свайпу снизу: настройки текста и цветов. Всё — через единый ReaderAction. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: UiReaderSettings,
    onAction: (ReaderAction) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            FontSizeSection(settings) { onAction(ReaderAction.SetFontSize(it)) }
            FontSection(settings) { onAction(ReaderAction.SetFont(it)) }
            MonoFontSection(settings) { onAction(ReaderAction.SetMonoFont(it)) }
            JustifySection(settings) { onAction(ReaderAction.SetJustify(it)) }
            ThemeSection(settings) { onAction(ReaderAction.SetTheme(it)) }
            ColorsSection(
                settings,
                onBgColor = { onAction(ReaderAction.SetBgColor(it)) },
                onTextColor = { onAction(ReaderAction.SetTextColor(it)) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
