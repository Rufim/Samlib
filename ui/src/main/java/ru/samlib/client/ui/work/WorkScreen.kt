package ru.samlib.client.ui.work

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.resolveSamlibHref

@Composable
fun WorkScreen(
    link: String,
    onBack: () -> Unit,
    scrollTo: Int = -1,
    onOpenWork: (String) -> Unit = {},
    onOpenAuthor: (String) -> Unit = {},
    onOpenLink: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenIllustrations: () -> Unit = {},
    onOpenSearch: (fromIndex: Int) -> Unit = {},
    viewModel: WorkViewModel = koinViewModel { parametersOf(link, scrollTo) },
) {
    val state by viewModel.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.observeSettings()
        viewModel.observeObserved()
        viewModel.load()
    }
    val context = LocalContext.current
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
    val workDir = remember(link) { link.substringBeforeLast('/', "") + "/" }
    val linkListener =
        remember(onOpenLink, workDir) {
            LinkInteractionListener { annotation ->
                val url = (annotation as? LinkAnnotation.Url)?.url ?: return@LinkInteractionListener
                // относительные ссылки читалки → абсолютный путь; тип определит роутер
                onOpenLink(resolveSamlibHref(url, workDir))
            }
        }
    BackHandler {
        viewModel.onLeave()
        onBack()
    }
    ReaderScaffold(
        state = state,
        // произведение samlib — доступны все пункты меню
        menu =
            ReaderMenuVisibility(
                search = true,
                goAuthor = true,
                observe = true,
                illustrations = true,
                downloadFb2 = true,
                save = true,
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
                is ReaderAction.SetJustify -> viewModel.setJustify(action.value)
                is ReaderAction.SetBgColor -> viewModel.setBgColor(action.color)
                is ReaderAction.SetTextColor -> viewModel.setTextColor(action.color)
                ReaderAction.TogglePin -> viewModel.togglePin()
                ReaderAction.ToggleObserve -> viewModel.toggleObserveWork()
                ReaderAction.GoAuthor -> onOpenAuthor(workDir)
                is ReaderAction.SetAutoScrollSpeed -> viewModel.setAutoScrollSpeed(action.speed)
                is ReaderAction.SetTtsRate -> viewModel.setTtsRate(action.rate)
                is ReaderAction.SetTtsPitch -> viewModel.setTtsPitch(action.pitch)
                is ReaderAction.StartTts -> viewModel.startTts(action.fromBlock, action.fromOffset)
                ReaderAction.OpenSettings -> onOpenSettings()
                ReaderAction.OpenIllustrations -> onOpenIllustrations()
                is ReaderAction.OpenSearch -> onOpenSearch(action.fromIndex)
                is ReaderAction.ToggleTranslate -> viewModel.toggleTranslate(action.fromIndex)
                ReaderAction.Save -> viewModel.save()
                ReaderAction.OpenExternal -> viewModel.openExternal()
                ReaderAction.DownloadFb2 -> viewModel.downloadFb2()
            }
        },
        linkListener = linkListener,
    )
}
