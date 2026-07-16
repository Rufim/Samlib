package ru.samlib.client.ui.newest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.ErrorBox
import ru.samlib.client.ui.common.FilterScreen
import ru.samlib.client.ui.common.LoadingBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewestScreen(
    onAuthor: (String) -> Unit,
    onOpenWork: (String) -> Unit = {},
    onMenu: () -> Unit = {},
    viewModel: NewestViewModel = koinViewModel(),
) {
    val state by viewModel.collectAsState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            is NewestSideEffect.NavigateToAuthor -> onAuthor(effect.link)
        }
    }
    LaunchedEffect(Unit) { viewModel.load() }

    var showFilter by remember { mutableStateOf(false) }
    if (showFilter) {
        FilterScreen(
            initial = state.filter,
            onApply = {
                viewModel.applyFilter(it)
                showFilter = false
            },
            onDismiss = { showFilter = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.newest_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.common_menu),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilter = true }) {
                        Icon(
                            painterResource(R.drawable.ic_filter),
                            contentDescription = stringResource(R.string.filter_title),
                            tint =
                                if (state.filter.active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
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
                val shown = state.items
                // Догружаем следующие страницы, если под фильтром видно мало элементов
                // (иначе фильтр работал бы только по уже загруженной странице).
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val last =
                            listState.layoutInfo.visibleItemsInfo
                                .lastOrNull()
                                ?.index ?: 0
                        last >= shown.size - 5
                    }
                }
                LaunchedEffect(shouldLoadMore, state.canLoadMore, state.loadingMore, shown.size) {
                    if (shouldLoadMore && state.canLoadMore && !state.loadingMore) {
                        viewModel.loadMore()
                    }
                }

                // Состояние для SwipeRefresh
                val swipeRefreshState = rememberPullToRefreshState()
                var isRefreshing by remember { mutableStateOf(false) }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    state = swipeRefreshState,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.padding(padding), // отступы от Scaffold
                ) {
                    LazyColumn(
                        state = listState,
                        modifier =
                            Modifier
                                .fillMaxSize(),
                    ) {
                        items(shown, key = { it.link }) { work ->
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenWork(work.link) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    work.title.orEmpty(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                work.customGenres?.let {
                                    if (it.isNotBlank()) {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    work.authorShortName?.let { name ->
                                        SuggestionChip(
                                            onClick = { work.authorLink?.let(viewModel::onAuthorClick) },
                                            enabled = work.authorLink != null,
                                            label = { Text(name) },
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        listOfNotNull(
                                            work.size?.let { "${it}k" },
                                            work.dateText,
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
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
}
