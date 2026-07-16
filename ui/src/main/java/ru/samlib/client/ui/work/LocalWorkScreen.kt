package ru.samlib.client.ui.work

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.ui.R

@Composable
fun LocalWorkScreen(
    uri: String,
    onBack: () -> Unit,
    scrollTo: Int = -1,
    onOpenSettings: () -> Unit = {},
    onOpenSearch: (fromIndex: Int) -> Unit = {},
    viewModel: LocalWorkViewModel = koinViewModel { parametersOf(uri, scrollTo) },
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    val openFileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            uri?.let {
                // Сохраняете URI и загружаете книгу
                viewModel.load(uri.toString())
            }
        }

    LaunchedEffect(Unit) {
        viewModel.observeSettings()
        // Проверяем, нужен ли запрос разрешения (file:// во внешнем хранилище)
        if (viewModel.needsStoragePermission(uri)) {
            // Извлекаем путь из file:// URI
            val realUri = uri.toUri()
            val filePath =
                realUri.path?.removePrefix("/storage/emulated/0/")
                    ?: context.getString(R.string.reader_path_not_found)
            Toast
                .makeText(
                    context,
                    context.getString(R.string.reader_no_file_access),
                    Toast.LENGTH_SHORT,
                ).show()
            Toast
                .makeText(
                    context,
                    context.getString(R.string.reader_full_path, filePath),
                    Toast.LENGTH_LONG,
                ).show()
            openFileLauncher.launch(arrayOf("*/*"))
        } else {
            viewModel.load()
        }
    }

    viewModel.collectSideEffect { effect ->
        val text =
            when (effect) {
                is WorkSideEffect.Message -> effect.text
                is WorkSideEffect.Saved -> context.getString(R.string.reader_saved, effect.fileName)
                WorkSideEffect.SaveFailed -> context.getString(R.string.reader_save_error)
                WorkSideEffect.OpenFailed -> context.getString(R.string.reader_open_failed)
                WorkSideEffect.Fb2Downloading -> context.getString(R.string.reader_fb2_downloading)
                WorkSideEffect.Fb2Failed -> context.getString(R.string.reader_fb2_failed)
            }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
    BackHandler {
        viewModel.onLeave()
        onBack()
    }
    ReaderScaffold(
        state = state,
        // локальная книга: доступны только поиск по тексту и перевод; пункты samlib скрыты
        menu =
            ReaderMenuVisibility(
                search = true,
                goAuthor = false,
                observe = false,
                illustrations = false,
                downloadFb2 = false,
                save = false,
                translate = true,
            ),
        onAction = { action ->
            when (action) {
                ReaderAction.Back -> {
                    viewModel.onLeave()
                    onBack()
                }

                ReaderAction.Retry -> viewModel.load()
                is ReaderAction.SetFontSize -> viewModel.setFontSize(action.sp)
                is ReaderAction.SetFont -> viewModel.setFont(action.font)
                is ReaderAction.SetMonoFont -> viewModel.setMonoFont(action.font)
                is ReaderAction.SetTheme -> viewModel.setTheme(action.theme)
                is ReaderAction.SavePosition -> viewModel.savePosition(action.index)
                ReaderAction.FlushPosition -> viewModel.flushPosition()
                ReaderAction.TogglePin -> viewModel.togglePin()
                is ReaderAction.SetJustify -> viewModel.setJustify(action.value)
                is ReaderAction.SetBgColor -> viewModel.setBgColor(action.color)
                is ReaderAction.SetTextColor -> viewModel.setTextColor(action.color)
                is ReaderAction.SetAutoScrollSpeed -> viewModel.setAutoScrollSpeed(action.speed)
                is ReaderAction.SetTtsRate -> viewModel.setTtsRate(action.rate)
                is ReaderAction.SetTtsPitch -> viewModel.setTtsPitch(action.pitch)
                is ReaderAction.StartTts -> viewModel.startTts(action.fromBlock, action.fromOffset)
                is ReaderAction.ToggleTranslate -> viewModel.toggleTranslate(action.fromIndex)
                ReaderAction.OpenSettings -> onOpenSettings()
                is ReaderAction.OpenSearch -> onOpenSearch(action.fromIndex)
                ReaderAction.OpenExternal -> viewModel.openExternal()
                // пункты, скрытые для локальной книги (menu = false) — недостижимы из меню
                ReaderAction.GoAuthor, ReaderAction.Save, ReaderAction.OpenIllustrations,
                ReaderAction.DownloadFb2, ReaderAction.ToggleObserve,
                -> Unit
            }
        },
    )
}
