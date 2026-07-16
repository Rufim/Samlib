package ru.samlib.client.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.domain.model.Author
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.UpdatedBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val followedDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowedScreen(
    onAuthor: (String) -> Unit,
    onOpenWork: (String) -> Unit = {},
    onMenu: () -> Unit = {},
    viewModel: FollowedViewModel = koinViewModel(),
) {
    val state by viewModel.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.observe()
        viewModel.observeBooks()
    }
    val ctx = LocalContext.current

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) {
                runCatching {
                    ctx.contentResolver
                        .openOutputStream(uri)
                        ?.use { it.write(viewModel.exportText().toByteArray()) }
                }
                Toast
                    .makeText(ctx, ctx.getString(R.string.followed_exported), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val text =
                    runCatching {
                        ctx.contentResolver
                            .openInputStream(uri)
                            ?.use { it.readBytes().toString(Charsets.UTF_8) }
                    }.getOrNull().orEmpty()
                if (text.isNotBlank()) viewModel.import(text)
            }
        }

    // Снек-бар с отменой удаления — как в Истории (HistoryScreen).
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // stringResource недоступен внутри suspend-лямбды collectSideEffect — берём строки заранее.
    val authorDeletedMessage = stringResource(R.string.followed_author_deleted)
    val bookDeletedMessage = stringResource(R.string.followed_book_deleted)
    val undoLabel = stringResource(R.string.followed_undo)
    viewModel.collectSideEffect { effect ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val job =
                launch {
                    delay(5000)
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
            val message =
                when (effect) {
                    is FollowedSideEffect.AuthorDeleted -> authorDeletedMessage
                    is FollowedSideEffect.BookDeleted -> bookDeletedMessage
                }
            val result =
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Indefinite,
                )
            job.cancel()
            if (result == SnackbarResult.ActionPerformed) {
                when (effect) {
                    is FollowedSideEffect.AuthorDeleted ->
                        viewModel.restoreAuthor(
                            effect.author,
                            effect.works,
                        )

                    is FollowedSideEffect.BookDeleted -> viewModel.restoreBook(effect.work)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.followed_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.followed_menu),
                        )
                    }
                },
                actions = {
                    // импорт/экспорт — в меню с тремя точками
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.followed_more),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.followed_import)) },
                            onClick = {
                                menuOpen = false
                                importLauncher.launch(
                                    arrayOf(
                                        "text/plain",
                                        "*/*",
                                    ),
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.followed_export)) },
                            onClick = {
                                menuOpen = false
                                exportLauncher.launch("samlib_followed.txt")
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (state.items.isEmpty() && state.books.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.followed_empty))
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                if (state.items.isNotEmpty()) {
                    item(key = "hdr:authors") { SectionTitle(stringResource(R.string.followed_authors)) }
                    items(state.items, key = { "a:${it.link}" }) { author ->
                        SwipeToDelete(onDelete = { viewModel.unfollow(author.link) }) {
                            AuthorRow(
                                author = author,
                                onOpen = { onAuthor(author.link) },
                                onAction = {
                                    if (author.hasUpdates) {
                                        viewModel.markSeen(author.link)
                                    } else {
                                        viewModel.unfollow(author.link)
                                    }
                                },
                            )
                        }
                        HorizontalDivider()
                    }
                }
                if (state.books.isNotEmpty()) {
                    item(key = "hdr:books") { SectionTitle(stringResource(R.string.followed_books)) }
                    items(state.books, key = { "b:${it.link}" }) { book ->
                        SwipeToDelete(onDelete = { viewModel.deleteBook(book.toDomain()) }) {
                            BookRow(
                                book = book,
                                onOpen = { onOpenWork(book.link) },
                                onAction = {
                                    if (book.changed) {
                                        viewModel.markBookSeen(book.link)
                                    } else {
                                        viewModel.deleteBook(book.toDomain())
                                    }
                                },
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

/** Свайп-удаление в обе стороны — как в Истории. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                    onDelete()
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
        content()
    }
}

@Composable
private fun AuthorRow(
    author: Author,
    onOpen: () -> Unit,
    onAction: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onOpen)
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(author.fullName ?: author.link, style = MaterialTheme.typography.titleMedium)
                if (author.hasUpdates) UpdatedBadge(modifier = Modifier.padding(start = 6.dp))
            }
            author.lastUpdateDate?.let {
                Text(
                    stringResource(R.string.followed_changed_date, followedDateFormat.format(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = onAction) {
            Text(
                if (author.hasUpdates) {
                    stringResource(R.string.followed_seen)
                } else {
                    stringResource(R.string.followed_unfollow)
                },
            )
        }
    }
}

@Composable
private fun BookRow(
    book: UiObservedWork,
    onOpen: () -> Unit,
    onAction: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onOpen)
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(book.title ?: book.link, style = MaterialTheme.typography.titleMedium)
                if (book.changed) UpdatedBadge(modifier = Modifier.padding(start = 6.dp))
            }
            Text(
                listOfNotNull(
                    book.authorName,
                    book.updateDate?.let {
                        stringResource(
                            R.string.followed_changed_date,
                            followedDateFormat.format(Date(it)),
                        )
                    },
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onAction) {
            Text(
                if (book.changed) {
                    stringResource(R.string.followed_seen)
                } else {
                    stringResource(R.string.followed_unfollow)
                },
            )
        }
    }
}
