package ru.samlib.client.ui.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.domain.model.Comment
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.ErrorBox
import ru.samlib.client.ui.common.LoadingBox
import ru.samlib.client.ui.common.htmlToAnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    link: String,
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit = {},
    viewModel: CommentsViewModel = koinViewModel { parametersOf(link) },
) {
    val state by viewModel.collectAsState()
    LaunchedEffect(link) { viewModel.load() }
    LaunchedEffect(Unit) { viewModel.observeAuth() }
    // Ссылки в тексте комментариев → единый роутер (как в работах/авторах).
    val linkListener =
        remember(onOpenLink) {
            LinkInteractionListener { annotation ->
                val url = (annotation as? LinkAnnotation.Url)?.url ?: return@LinkInteractionListener
                onOpenLink(url)
            }
        }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val postedMessage = stringResource(R.string.comments_posted)
    viewModel.collectSideEffect { effect ->
        val msg =
            when (effect) {
                is CommentsSideEffect.Posted -> postedMessage
                is CommentsSideEffect.PostError -> effect.message
            }
        scope.launch { snackbar.showSnackbar(msg) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.comments_title)) },
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
        bottomBar = {
            CommentComposer(
                state =
                    CommentComposerState(
                        loggedIn = state.loggedIn,
                        sending = state.sending,
                        replyTo = state.replyTo?.number,
                        name = state.authorName,
                        email = state.authorEmail,
                        url = state.authorUrl,
                    ),
                onName = viewModel::setAuthorName,
                onEmail = viewModel::setAuthorEmail,
                onUrl = viewModel::setAuthorUrl,
                onCancelReply = { viewModel.setReply(null) },
                onSend = viewModel::send,
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

            state.comments.isEmpty() ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.comments_empty)) }

            else -> {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                ) {
                    items(state.comments, key = { it.number }) { c ->
                        CommentItem(
                            c,
                            loggedIn = state.loggedIn,
                            linkListener = linkListener,
                            onReply = { viewModel.setReply(c) },
                            onDelete = { viewModel.delete(c) },
                        )
                        HorizontalDivider()
                    }
                    if (state.canLoadMore) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadMore() }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.loadingMore) {
                                    CircularProgressIndicator()
                                } else {
                                    Text(
                                        stringResource(R.string.comments_show_more),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Состояние формы ввода комментария (без колбэков). */
@Immutable
data class CommentComposerState(
    val loggedIn: Boolean, // вошёл ли пользователь (аноним обязан заполнить поля)
    val sending: Boolean, // идёт отправка — блокирует кнопку
    val replyTo: Int?, // MSGID, на который отвечаем; null — новый комментарий
    val name: String, // имя автора
    val email: String, // e-mail
    val url: String, // ссылка на сайт
)

@Composable
private fun CommentComposer(
    state: CommentComposerState,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onUrl: (String) -> Unit,
    onCancelReply: () -> Unit,
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    // Аноним обязан заполнить данные → раскрыто; вошедший — свёрнуто (подставлено из аккаунта).
    var showFields by remember(state.loggedIn) { mutableStateOf(!state.loggedIn) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        if (!state.loggedIn) {
            Text(
                stringResource(R.string.comments_login_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Text(
            "${if (showFields) "▾" else "▸"} " +
                stringResource(
                    R.string.comments_your_name,
                    state.name.ifBlank { stringResource(R.string.comments_name_not_set) },
                ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { showFields = !showFields }
                    .padding(vertical = 6.dp),
        )
        if (showFields) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onName,
                label = { Text(stringResource(R.string.comments_name_label)) },
                singleLine = true,
                isError = state.name.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = onEmail,
                label = { Text(stringResource(R.string.comments_email_label)) },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
            )
            OutlinedTextField(
                value = state.url,
                onValueChange = onUrl,
                label = { Text(stringResource(R.string.comments_url_label)) },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
            )
        }
        if (state.replyTo != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.comments_reply_to, state.replyTo),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCancelReply) { Text(stringResource(R.string.common_cancel)) }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.comments_placeholder)) },
                modifier = Modifier.weight(1f),
                maxLines = 4,
            )
            IconButton(
                onClick = {
                    onSend(text)
                    text = ""
                },
                enabled = !state.sending && text.isNotBlank() && state.name.isNotBlank(),
            ) {
                if (state.sending) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.comments_send),
                    )
                }
            }
        }
    }
}

/** Свёрнутый по умолчанию спойлер цитаты; в заголовке — номер и имя автора (если есть). */
@Composable
private fun QuoteNodeView(
    node: QuoteNode,
    linkListener: LinkInteractionListener,
) {
    var open by remember { mutableStateOf(false) }
    val who =
        when {
            node.number != null && node.name != null -> "${node.number}. ${node.name}"
            node.number != null -> "#${node.number}"
            node.name != null -> node.name
            else -> null
        }
    val label =
        if (who != null) {
            stringResource(
                R.string.comments_quote_from,
                who,
            )
        } else {
            stringResource(R.string.comments_quote)
        }
    Column(Modifier.fillMaxWidth()) {
        Text(
            "${if (open) "▾" else "▸"} $label",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { open = !open }
                    .padding(top = 4.dp, bottom = 2.dp),
        )
        if (open) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
            ) {
                node.items.forEachIndexed { index, item ->
                    key(index) {
                        when (item) {
                            is QuoteItem.Line ->
                                Text(
                                    htmlToAnnotatedString(item.text, linkListener),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(top = 1.dp),
                                )

                            is QuoteItem.Nested ->
                                Box(Modifier.padding(start = 8.dp)) {
                                    QuoteNodeView(item.node, linkListener)
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentBody(
    text: String,
    linkListener: LinkInteractionListener,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        segmentBody(text).forEach { seg ->
            when (seg) {
                is BodySegment.Quote -> QuoteNodeView(buildQuoteTree(seg.lines), linkListener)
                is BodySegment.Own ->
                    seg.lines.forEach { line ->
                        if (line.isNotBlank()) {
                            Text(
                                htmlToAnnotatedString(line, linkListener),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun CommentItem(
    c: Comment,
    loggedIn: Boolean = false,
    linkListener: LinkInteractionListener,
    onReply: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            "${c.number}. ${c.nickName ?: stringResource(R.string.comments_anonymous)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        c.date?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (c.body.isBlank() && c.deleted) {
            Text(
                stringResource(R.string.comments_deleted),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            CommentBody(c.body, linkListener, modifier = Modifier.padding(top = 4.dp))
        }
        if (loggedIn && (c.canReply || c.canDelete) && c.msgid != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (c.canReply) {
                    TextButton(onClick = onReply) { Text(stringResource(R.string.comments_reply)) }
                }
                if (c.canDelete) {
                    TextButton(onClick = onDelete) {
                        Text(
                            stringResource(R.string.common_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
