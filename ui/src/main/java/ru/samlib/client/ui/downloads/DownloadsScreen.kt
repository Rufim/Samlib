package ru.samlib.client.ui.downloads

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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ru.samlib.client.domain.model.SavedItem
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.LoadingBox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onOpenLocalFile: (String) -> Unit,
    onMenu: () -> Unit = {},
    viewModel: DownloadsViewModel = koinViewModel(),
) {
    val state by viewModel.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.downloads_title)) },
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
            state.files.isEmpty() ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.downloads_empty)) }

            else ->
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    items(state.files, key = { it.uri }) { item ->
                        // Удаление свайпом — как в Истории.
                        val dismissState =
                            rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart ||
                                        it == SwipeToDismissBoxValue.StartToEnd
                                    ) {
                                        viewModel.delete(item.uri)
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
                            SavedFileRow(item, onOpen = { onOpenLocalFile(item.uri) })
                        }
                        HorizontalDivider()
                    }
                }
        }
    }
}

@Composable
private fun SavedFileRow(
    item: SavedItem,
    onOpen: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onOpen)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            item.author?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.date > 0) {
                Text(
                    dateFormat.format(Date(item.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
