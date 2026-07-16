package ru.samlib.client.ui.author

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.model.Work
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.ErrorBox
import ru.samlib.client.ui.common.LoadingBox
import ru.samlib.client.ui.common.UpdatedBadge
import ru.samlib.client.ui.common.htmlToAnnotatedString
import ru.samlib.client.ui.common.resolveSamlibHref
import ru.samlib.client.ui.theme.SamlibTheme
import java.math.BigDecimal

/** Действия экрана автора — единый канал навигации/редактирования вместо пачки колбэков. */
sealed interface AuthorAction {
    data object Back : AuthorAction

    data class OpenWork(
        val link: String,
    ) : AuthorAction

    data class OpenAuthor(
        val link: String,
    ) : AuthorAction

    data class OpenComments(
        val link: String,
    ) : AuthorAction

    data class EditAbout(
        val link: String,
    ) : AuthorAction

    data class EditDir(
        val link: String,
    ) : AuthorAction

    data class EditGroups(
        val link: String,
    ) : AuthorAction

    data class AddWork(
        val link: String,
    ) : AuthorAction

    data class EditWork(
        val link: String,
        val file: String,
    ) : AuthorAction

    data class OpenLink(
        val href: String,
    ) : AuthorAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorScreen(
    link: String,
    onAction: (AuthorAction) -> Unit,
    viewModel: AuthorViewModel = koinViewModel { parametersOf(link) },
) {
    val state by viewModel.collectAsState()
    viewModel.collectSideEffect { effect ->
        when (effect) {
            is AuthorSideEffect.OpenWork -> onAction(AuthorAction.OpenWork(effect.link))
            is AuthorSideEffect.OpenComments -> onAction(AuthorAction.OpenComments(effect.link))
            is AuthorSideEffect.EditWork -> onAction(AuthorAction.EditWork(effect.link, effect.file))
        }
    }
    LaunchedEffect(Unit) {
        viewModel.observeFollow()
        viewModel.observeUpdates()
        viewModel.checkOwnPage()
        viewModel.load()
    }

    val linkListener =
        remember(onAction, link) {
            LinkInteractionListener { annotation ->
                val url = (annotation as? LinkAnnotation.Url)?.url ?: return@LinkInteractionListener
                onAction(AuthorAction.OpenLink(resolveSamlibHref(url, link)))
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.author?.fullName ?: stringResource(R.string.author_title_fallback),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(AuthorAction.Back) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.author_back),
                        )
                    }
                },
                actions = {
                    if (state.isOwnPage) {
                        val menuOpen =
                            androidx.compose.runtime.remember {
                                androidx.compose.runtime.mutableStateOf(false)
                            }
                        IconButton(onClick = { menuOpen.value = true }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.author_edit_section),
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen.value,
                            onDismissRequest = { menuOpen.value = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.author_menu_dir_info)) },
                                onClick = {
                                    menuOpen.value = false
                                    onAction(AuthorAction.EditDir(link))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.author_menu_groups)) },
                                onClick = {
                                    menuOpen.value = false
                                    onAction(AuthorAction.EditGroups(link))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.author_menu_about)) },
                                onClick = {
                                    menuOpen.value = false
                                    onAction(AuthorAction.EditAbout(link))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.author_menu_add_work)) },
                                onClick = {
                                    menuOpen.value = false
                                    onAction(AuthorAction.AddWork(link))
                                },
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.toggleFollow() }) {
                        Icon(
                            imageVector = if (state.isFollowed) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription =
                                if (state.isFollowed) {
                                    stringResource(R.string.author_unfollow)
                                } else {
                                    stringResource(R.string.author_follow)
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
                val author = state.author
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                ) {
                    if (author != null) {
                        item { AuthorHeader(author, viewModel::onWorkClick) }
                        author.categories.forEachIndexed { index, category ->
                            val title = category.title
                            val catLink = category.link
                            if (title != null) {
                                item(key = "cat:$index:$title") {
                                    SectionHeader(
                                        title = title,
                                        collapsed = title in state.collapsed,
                                        isLink = catLink != null,
                                        updated = title in state.changedCategories,
                                        onClick = {
                                            if (catLink != null) {
                                                onAction(AuthorAction.OpenAuthor(catLink))
                                            } else {
                                                viewModel.toggleSection(title)
                                            }
                                        },
                                    )
                                }
                            }
                            // подстраница-раздел (со ссылкой) открывается отдельно — работы не инлайним
                            if (catLink == null && (title == null || title !in state.collapsed)) {
                                items(category.works, key = { "work:$index:${it.link}" }) { work ->
                                    WorkRow(
                                        work,
                                        linkListener,
                                        onComments = { viewModel.onCommentsClick(work.link) },
                                        updated = work.link in state.changedLinks,
                                        // редактирование своего произведения (деривация имени файла — во VM)
                                        onEdit =
                                            if (state.isOwnPage) {
                                                { viewModel.onEditWorkClick(work.link) }
                                            } else {
                                                null
                                            },
                                    ) { viewModel.onWorkClick(work.link) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AuthorHeader(
    author: Author,
    onWorkClick: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (author.avatarUrl != null) {
                AsyncImage(
                    model = author.avatarUrl,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                )
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.fillMaxWidth()) {
                author.size?.let { StatRow(stringResource(R.string.author_stat_size), "${it}k") }
                author.workCount?.let {
                    StatRow(
                        stringResource(R.string.author_stat_works),
                        it.toString(),
                    )
                }
                author.rate?.let { r ->
                    StatRow(
                        stringResource(R.string.author_stat_rating),
                        r.toString() + (author.votes?.let { " ($it)" } ?: ""),
                    )
                }
                author.views?.let {
                    StatRow(
                        stringResource(R.string.author_stat_views),
                        it.toString(),
                    )
                }
                author.address?.let { StatRow(stringResource(R.string.author_stat_lives), it) }
            }
        }
        author.sectionAnnotation?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        author.about?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (author.recommendations.isNotEmpty()) {
            Text(
                stringResource(R.string.author_recommendations_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            author.recommendations.forEach { rec ->
                if (!rec.title.isNullOrBlank()) {
                    Text(
                        rec.title.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onWorkClick(rec.link) }
                                .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun SectionHeader(
    title: String,
    collapsed: Boolean,
    isLink: Boolean,
    updated: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                // раздел-подстраница открывается как список (→), обычный раздел сворачивается (▾/▸)
                "$title  ${if (isLink) {
                    "→"
                } else if (collapsed) {
                    "▸"
                } else {
                    "▾"
                }}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (updated) {
                UpdatedBadge(
                    text = stringResource(R.string.author_updated_badge),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun WorkRow(
    work: Work,
    linkListener: LinkInteractionListener,
    onComments: () -> Unit,
    updated: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                work.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (updated) {
                UpdatedBadge(
                    text = stringResource(R.string.author_updated_badge),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.author_edit_work),
                    )
                }
            }
        }
        val meta =
            buildList {
                work.size?.let { add("${it}k") }
                work.rate?.let { add("★ $it") }
                work.customGenres?.let { if (it.isNotBlank()) add(it) }
            }
        if (meta.isNotEmpty()) {
            Text(meta.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
        }
        work.annotation?.let {
            Text(
                htmlToAnnotatedString(it, linkListener),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        work.commentCount?.let { n ->
            AssistChip(
                onClick = onComments,
                label = { Text(stringResource(R.string.author_comments_count, n)) },
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
    HorizontalDivider()
}

@Preview(showBackground = true)
@Composable
private fun AuthorWorkRowPreview() {
    SamlibTheme {
        WorkRow(
            work =
                Work(
                    link = "/i/ivanov_i/rasskaz.shtml",
                    title = "Название произведения",
                    size = 45,
                    rate = BigDecimal("4.2"),
                    customGenres = "Приключения",
                    annotation = "Аннотация произведения для предпросмотра строки списка.",
                    commentCount = 3,
                ),
            linkListener = LinkInteractionListener {},
            onComments = {},
            onClick = {},
        )
    }
}
