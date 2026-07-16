package ru.samlib.client.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.model.WorkFilter
import ru.samlib.client.domain.model.applyFilter
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.FilterScreen
import ru.samlib.client.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ярко-синий для закреплённых (скрепка). */
private val PinnedBlue = AppColors.PinnedBlue

private val historyDateFormat = SimpleDateFormat("dd.MM", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenWork: (String) -> Unit,
    onMenu: () -> Unit = {},
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val state by viewModel.collectAsState()
    LaunchedEffect(Unit) { viewModel.observe() }

    var filter by remember { mutableStateOf(WorkFilter()) }
    var showFilter by remember { mutableStateOf(false) }
    if (showFilter) {
        FilterScreen(
            initial = filter,
            showQuery = false,
            onApply = {
                filter = it
                showFilter = false
            },
            onDismiss = { showFilter = false },
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // stringResource недоступен внутри suspend-лямбды collectSideEffect — берём строки заранее.
    val deletedMessage = stringResource(R.string.history_deleted)
    val undoLabel = stringResource(R.string.history_undo)
    viewModel.collectSideEffect { effect ->
        when (effect) {
            is HistorySideEffect.Deleted ->
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val job =
                        launch {
                            delay(5000)
                            snackbarHostState.currentSnackbarData?.dismiss()
                        }
                    val result =
                        snackbarHostState.showSnackbar(
                            message = deletedMessage,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Indefinite,
                        )
                    job.cancel()
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restore(effect.work)
                    }
                }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.history_menu),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilter = true }) {
                        Icon(
                            painterResource(R.drawable.ic_filter),
                            contentDescription = stringResource(R.string.history_filter),
                            tint =
                                if (filter.active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                    IconButton(onClick = { viewModel.toggleOnlyPinned() }) {
                        Icon(
                            painterResource(R.drawable.ic_attach),
                            contentDescription = stringResource(R.string.history_only_pinned),
                            tint =
                                if (state.onlyPinned) {
                                    PinnedBlue
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                    IconButton(onClick = { viewModel.clear() }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.history_clear),
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
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.history_search)) },
            )
            val shown = state.items.applyFilter(filter)
            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.history_empty))
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(shown, key = { it.link }) { work ->
                        val dismissState =
                            rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart ||
                                        it == SwipeToDismissBoxValue.StartToEnd
                                    ) {
                                        viewModel.delete(work.link)
                                        true
                                    } else {
                                        false
                                    }
                                },
                            )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                )
                            },
                        ) {
                            HistoryItem(work, onOpenWork) { viewModel.togglePin(work.link) }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    work: Work,
    onOpenWork: (String) -> Unit,
    onTogglePin: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onOpenWork(work.link) }
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(work.title.orEmpty(), style = MaterialTheme.typography.titleMedium)
            work.customGenres?.let {
                if (it.isNotBlank()) {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                listOfNotNull(
                    work.authorShortName?.ifBlank { null },
                    work.openedDate?.takeIf { it > 0 }?.let { historyDateFormat.format(Date(it)) },
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IconButton(onClick = onTogglePin) {
            Icon(
                painterResource(R.drawable.ic_attach),
                contentDescription =
                    if (work.pinned) {
                        stringResource(R.string.history_unpin)
                    } else {
                        stringResource(R.string.history_pin)
                    },
                tint =
                    if (work.pinned) {
                        PinnedBlue
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}
