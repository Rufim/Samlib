package ru.samlib.client.ui.work

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkSearchScreen(
    link: String?,
    uri: String?,
    // текущая позиция чтения на момент открытия поиска (-1 — неизвестна/не нужна)
    fromIndex: Int = -1,
    onBack: () -> Unit,
    onOpenResult: (blockIndex: Int) -> Unit,
    viewModel: WorkSearchViewModel =
        koinViewModel(key = "search:${link ?: uri}") {
            parametersOf(link, uri, fromIndex)
        },
) {
    val state by viewModel.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    val listState = rememberLazyListState()
    // Решение "куда прокрутить" принимает ViewModel детерминированно (см. её kdoc) — экран
    // только применяет одноразовую команду и сразу гасит её, чтобы не сработать повторно.
    LaunchedEffect(state.scrollCommand) {
        val cmd = state.scrollCommand ?: return@LaunchedEffect
        listState.scrollToItem(cmd.index, cmd.offset)
        viewModel.scrollCommandConsumed()
    }
    // Сохраняем позицию списка в app-память, чтобы вернуть её при повторном открытии поиска.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (i, off) -> viewModel.rememberScroll(i, off) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.worksearch_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text(stringResource(R.string.worksearch_query_label)) },
                singleLine = true,
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.worksearch_clear),
                            )
                        }
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                state.loading -> LoadingBox()
                state.error != null ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(state.error!!, Modifier.padding(24.dp))
                    }

                else -> {
                    val hint =
                        when {
                            state.query.trim().length < SearchDefaults.MIN_QUERY ->
                                stringResource(
                                    R.string.worksearch_min_chars,
                                )
                            state.hits.isEmpty() -> stringResource(R.string.worksearch_no_results)
                            else ->
                                stringResource(
                                    R.string.worksearch_found,
                                    "${state.hits.size}${if (state.truncated) "+" else ""}",
                                )
                        }
                    Text(
                        hint,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(state.hits) { hit ->
                            SearchHitRow(hit, state.blockCount) { onOpenResult(hit.blockIndex) }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHitRow(
    hit: SearchHit,
    blockCount: Int,
    onClick: () -> Unit,
) {
    val text =
        buildAnnotatedString {
            append(hit.before)
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                ),
            ) {
                append(hit.match)
            }
            append(hit.after)
        }
    // Позиция находки относительно всего текста книги — в нижнем правом углу карточки.
    val percent = if (blockCount > 0) (hit.blockIndex + 1).toFloat() / blockCount * 100f else 0f
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = text, maxLines = 3)
        Text(
            text = "%.2f%%".format(percent),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
