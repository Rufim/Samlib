package ru.samlib.client.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import ru.samlib.client.domain.model.RatingSection
import ru.samlib.client.ui.R
import ru.samlib.client.ui.downloads.DownloadsScreen
import ru.samlib.client.ui.newest.NewestScreen
import ru.samlib.client.ui.rating.RatingScreen
import ru.samlib.client.ui.search.SearchScreen
import ru.samlib.client.ui.theme.AppColors

/** Строка статуса сети в шторке: цветная точка + «онлайн/офлайн», тап — перепроверка. */
@Composable
private fun OfflineStatusRow(
    offline: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (offline) MaterialTheme.colorScheme.error else AppColors.Success
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            if (offline) stringResource(R.string.home_offline) else stringResource(R.string.home_online),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Профиль в шапке шторки: имя пользователя (или «Аноним») + аватарка. */
private data class DrawerProfile(
    val name: String,
    val avatarUrl: String?,
)

/** Шапка профиля: аватар (или человечек) + имя, тап → экран входа. */
@Composable
private fun ProfileRow(
    profile: DrawerProfile,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val avatar = profile.avatarUrl
        if (avatar != null) {
            AsyncImage(
                model = avatar,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape),
            )
        } else {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = profile.name,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(profile.name, style = MaterialTheme.typography.titleMedium)
    }
}

// Ключи вкладок: фиксированные разделы + RatingSection.name для сетевых списков.
private const val KEY_NEWEST = "newest"
private const val KEY_SEARCH = "search"
private const val KEY_HISTORY = "history"
private const val KEY_FOLLOWED = "followed"
private const val KEY_DOWNLOADS = "downloads"

/** Запись меню: либо переключение вкладки (action == null), либо действие-навигация. */
private data class DrawerEntry(
    val key: String,
    val title: String,
    val action: (() -> Unit)? = null,
    val needsNet: Boolean = false,
    val icon: (@Composable () -> Unit)?,
)

// Разделы, доступные офлайн (без обращения к samlib).
private val OFFLINE_KEYS = setOf(KEY_HISTORY, KEY_FOLLOWED, KEY_DOWNLOADS)

/** Иконка раздела каталога (см. легаси: избранное=звезда, обзоры=книга, обсуждения=облачко). */
@Composable
private fun ratingIcon(section: RatingSection) {
    when (section) {
        RatingSection.TOP_AUTHORS -> Icon(Icons.Filled.Person, contentDescription = section.title)
        RatingSection.REVIEWS -> Icon(Icons.Filled.Star, contentDescription = section.title)
        RatingSection.DISCUSSIONS ->
            Icon(painterResource(R.drawable.ic_comment), contentDescription = section.title)

        RatingSection.FAVORITES ->
            Icon(painterResource(R.drawable.ic_bookmark), contentDescription = section.title)
    }
}

/** Действия главного экрана — единый канал навигации вместо пачки колбэков. */
sealed interface HomeAction {
    data class OpenAuthor(
        val link: String,
    ) : HomeAction

    data class OpenWork(
        val link: String,
    ) : HomeAction

    data class OpenComments(
        val link: String,
    ) : HomeAction

    data class OpenLink(
        val href: String,
    ) : HomeAction

    data object OpenSettings : HomeAction

    data object OpenHelp : HomeAction

    data object OpenLogin : HomeAction

    data class OpenLocalFile(
        val uri: String,
    ) : HomeAction
}

@Composable
fun HomeScreen(
    onAction: (HomeAction) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    var selected by rememberSaveable { mutableStateOf(KEY_NEWEST) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openMenu: () -> Unit = { scope.launch { drawerState.open() } }

    val state by viewModel.collectAsState()
    val offline = state.offline
    LaunchedEffect(Unit) {
        viewModel.observeOffline()
        viewModel.observeProfile()
        viewModel.refresh()
    }

    // Профиль для шапки: имя пользователя (или «Аноним») + аватарка (логику собирает VM).
    val anonymousName = stringResource(R.string.home_profile_anonymous)
    val defaultUserName = stringResource(R.string.home_profile_user)
    val profile =
        DrawerProfile(
            name = if (state.loggedIn) state.accountName.ifBlank { defaultUserName } else anonymousName,
            avatarUrl = state.avatarUrl,
        )
    // В офлайне сетевой раздел недоступен → переключаемся на локальный (История).
    LaunchedEffect(offline) {
        if (offline && selected !in OFFLINE_KEYS) selected = KEY_HISTORY
    }

    // Результат SAF-пикера приходит в момент, когда HomeRoute ещё не RESUMED, и прямая
    // навигация отбрасывается guard'ом isResumed. Поэтому VM кладёт URI в state, а навигацию
    // запускаем из LaunchedEffect — он отрабатывает уже после возврата в RESUMED.
    LaunchedEffect(state.pendingFileUri) {
        state.pendingFileUri?.let { uri ->
            viewModel.consumePendingFile()
            onAction(HomeAction.OpenLocalFile(uri))
        }
    }
    val filePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.onFilePicked(uri)
        }
    val openFile: () -> Unit = { filePicker.launch(viewModel.fileMimeTypes) }

    val groups: List<Pair<String, List<DrawerEntry>>> =
        listOf(
            stringResource(R.string.home_group_feed) to
                listOf(
                    DrawerEntry(KEY_NEWEST, stringResource(R.string.home_newest), needsNet = true) {
                        Icon(
                            painterResource(R.drawable.ic_newest),
                            contentDescription = stringResource(R.string.home_newest),
                        )
                    },
                    DrawerEntry(KEY_SEARCH, stringResource(R.string.home_search), needsNet = true) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.home_search))
                    },
                ),
            stringResource(R.string.home_group_catalog) to
                RatingSection.entries.map { section ->
                    DrawerEntry(section.name, section.title, needsNet = true) { ratingIcon(section) }
                },
            stringResource(R.string.home_group_mine) to
                listOf(
                    DrawerEntry(KEY_HISTORY, stringResource(R.string.home_history)) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = stringResource(R.string.home_history),
                        )
                    },
                    DrawerEntry(KEY_FOLLOWED, stringResource(R.string.home_followed)) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = stringResource(R.string.home_followed),
                        )
                    },
                    DrawerEntry(KEY_DOWNLOADS, stringResource(R.string.home_downloads)) {
                        Icon(
                            painterResource(R.drawable.ic_file),
                            contentDescription = stringResource(R.string.home_downloads),
                        )
                    },
                ),
            stringResource(R.string.home_group_more) to
                listOf(
                    DrawerEntry(
                        "settings",
                        stringResource(R.string.home_settings),
                        action = { onAction(HomeAction.OpenSettings) },
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.home_settings),
                        )
                    },
                    DrawerEntry("openfile", stringResource(R.string.home_open_file), action = openFile) {
                        Icon(
                            painterResource(R.drawable.ic_file),
                            contentDescription = stringResource(R.string.home_open_file),
                        )
                    },
                    DrawerEntry(
                        "help",
                        stringResource(R.string.home_help),
                        action = { onAction(HomeAction.OpenHelp) },
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.home_help))
                    },
                ),
        )

    // В офлайне убираем сетевые пункты и пустые группы.
    val visibleGroups =
        groups
            .map { (header, entries) -> header to entries.filter { !offline || !it.needsNet } }
            .filter { it.second.isNotEmpty() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // Профиль/вход — только онлайн (сетевой раздел).
                    if (!offline) {
                        ProfileRow(
                            profile = profile,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onAction(HomeAction.OpenLogin)
                            },
                        )
                    }
                    OfflineStatusRow(offline = offline, onClick = { viewModel.refresh() })
                    visibleGroups.forEach { (header, entries) ->
                        Text(
                            header,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
                        )
                        entries.forEach { entry ->
                            NavigationDrawerItem(
                                icon = entry.icon,
                                label = { Text(entry.title) },
                                selected = entry.action == null && selected == entry.key,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    val act = entry.action
                                    if (act != null) act() else selected = entry.key
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                    }
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            when (selected) {
                KEY_NEWEST ->
                    NewestScreen(
                        onAuthor = { onAction(HomeAction.OpenAuthor(it)) },
                        onOpenWork = { onAction(HomeAction.OpenWork(it)) },
                        onMenu = openMenu,
                    )

                KEY_SEARCH ->
                    SearchScreen(
                        onOpenLink = { onAction(HomeAction.OpenLink(it)) },
                        onMenu = openMenu,
                    )

                KEY_HISTORY ->
                    HistoryScreen(
                        onOpenWork = { onAction(HomeAction.OpenWork(it)) },
                        onMenu = openMenu,
                    )

                KEY_FOLLOWED ->
                    FollowedScreen(
                        onAuthor = { onAction(HomeAction.OpenAuthor(it)) },
                        onOpenWork = { onAction(HomeAction.OpenWork(it)) },
                        onMenu = openMenu,
                    )

                KEY_DOWNLOADS ->
                    DownloadsScreen(
                        onOpenLocalFile = { onAction(HomeAction.OpenLocalFile(it)) },
                        onMenu = openMenu,
                    )

                else ->
                    RatingScreen(
                        section = RatingSection.valueOf(selected),
                        onMenu = openMenu,
                        onAuthor = { onAction(HomeAction.OpenAuthor(it)) },
                        onOpenWork = { onAction(HomeAction.OpenWork(it)) },
                        onOpenComments = { onAction(HomeAction.OpenComments(it)) },
                    )
            }
        }
    }
}
