package ru.samlib.client.ui.rating

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.domain.model.RatingRow
import ru.samlib.client.domain.model.RatingSection
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.ErrorBox
import ru.samlib.client.ui.common.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingScreen(
    section: RatingSection,
    onMenu: () -> Unit = {},
    onAuthor: (String) -> Unit = {},
    onOpenWork: (String) -> Unit = {},
    onOpenComments: (String) -> Unit = {},
    viewModel: RatingViewModel = koinViewModel(key = section.name) { parametersOf(section) },
) {
    val state by viewModel.collectAsState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            is RatingSideEffect.NavigateToAuthor -> onAuthor(effect.link)
            is RatingSideEffect.NavigateToWork -> onOpenWork(effect.link)
            is RatingSideEffect.NavigateToComments -> onOpenComments(effect.link)
        }
    }
    LaunchedEffect(section) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section.title) },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.common_menu),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null ->
                ErrorBox(
                    state.error!!,
                    viewModel::load,
                    Modifier.padding(padding),
                )

            else -> {
                val listState = rememberLazyListState()
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val last =
                            listState.layoutInfo.visibleItemsInfo
                                .lastOrNull()
                                ?.index ?: 0
                        last >= state.rows.size - 5
                    }
                }
                LaunchedEffect(shouldLoadMore, state.canLoadMore, state.loadingMore) {
                    if (shouldLoadMore && state.canLoadMore && !state.loadingMore) {
                        viewModel.loadMore()
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                ) {
                    itemsIndexed(state.rows, key = { index, _ -> index }) { _, row ->
                        when (row) {
                            is RatingRow.AuthorItem -> AuthorRow(row.author, onAuthor)
                            is RatingRow.WorkItem -> WorkRow(row.work, onOpenWork, onAuthor)
                            is RatingRow.DiscussionItem ->
                                DiscussionRow(
                                    row.discussion,
                                    { link -> viewModel.onCommentsClick(link) },
                                    onAuthor,
                                )
                        }
                        HorizontalDivider()
                    }
                    if (state.loadingMore) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }
                        }
                    }
                }
            }
        }
    }
}
