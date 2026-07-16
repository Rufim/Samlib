package ru.samlib.client.ui.navigation

import android.content.ContentResolver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import ru.samlib.client.ui.auth.LoginAction
import ru.samlib.client.ui.auth.LoginScreen
import ru.samlib.client.ui.author.AboutEditScreen
import ru.samlib.client.ui.author.AuthorAction
import ru.samlib.client.ui.author.AuthorScreen
import ru.samlib.client.ui.author.DirEditScreen
import ru.samlib.client.ui.author.GroupEditScreen
import ru.samlib.client.ui.author.WorkEditScreen
import ru.samlib.client.ui.comments.CommentsScreen
import ru.samlib.client.ui.common.SamlibTarget
import ru.samlib.client.ui.common.UnsupportedScreen
import ru.samlib.client.ui.common.classifySamlibLink
import ru.samlib.client.ui.help.HelpScreen
import ru.samlib.client.ui.home.HomeAction
import ru.samlib.client.ui.home.HomeScreen
import ru.samlib.client.ui.illustrations.IllustrationsScreen
import ru.samlib.client.ui.newest.NewestScreen
import ru.samlib.client.ui.work.LocalWorkScreen
import ru.samlib.client.ui.work.SettingsScreen
import ru.samlib.client.ui.work.WorkScreen
import ru.samlib.client.ui.work.WorkSearchScreen

// Экран считается «активным» только в состоянии RESUMED. Гейт от двойных переходов
// при быстрых повторных нажатиях (иначе back-стек мог «проскочить» в пустоту).
private fun NavBackStackEntry.isResumed() = lifecycle.currentState == Lifecycle.State.RESUMED

/** Открыть любую samlib-ссылку на нужном экране; неизвестную — на заглушке. */
private fun navigateSamlib(
    nav: NavHostController,
    uriHandler: UriHandler,
    href: String,
) {
    when (val t = classifySamlibLink(href)) {
        is SamlibTarget.Work -> nav.navigate(WorkRoute(t.link))
        is SamlibTarget.Author -> nav.navigate(AuthorRoute(t.link))
        is SamlibTarget.Comments -> nav.navigate(CommentsRoute(t.link))
        is SamlibTarget.Illustrations -> nav.navigate(IllustrationsRoute(t.link))
        is SamlibTarget.External -> runCatching { uriHandler.openUri(t.url) }
        is SamlibTarget.Unsupported -> nav.navigate(UnsupportedRoute(t.link))
    }
}

@Composable
fun SamlibNavHost(
    navController: NavHostController,
    startRoute: Any = HomeRoute,
    // авто-открытие последней книги при старте: кладётся поверх Home (back → на главную)
    autoOpenRoute: Any? = null,
) {
    // Ключ — autoOpenRoute, а не Unit: значение разрешается асинхронно уже после первой
    // композиции (см. MainActivity), эффект должен сработать именно при его появлении.
    LaunchedEffect(autoOpenRoute) {
        if (autoOpenRoute != null) navController.navigate(autoOpenRoute)
    }
    NavHost(navController = navController, startDestination = startRoute) {
        composable<HomeRoute> { entry ->
            val uriHandler = LocalUriHandler.current
            val nav = { route: Any -> if (entry.isResumed()) navController.navigate(route) }
            val openLink = { href: String ->
                if (entry.isResumed()) {
                    navigateSamlib(
                        navController,
                        uriHandler,
                        href,
                    )
                }
            }
            HomeScreen(onAction = { action ->
                when (action) {
                    is HomeAction.OpenAuthor -> nav(AuthorRoute(action.link))
                    // История содержит и локальные книги (ключ — URI файла): такие ссылки ведём
                    // в локальную читалку, ссылки samlib («/…») — в обычную.
                    is HomeAction.OpenWork ->
                        if (action.link.startsWith("${ContentResolver.SCHEME_FILE}:") ||
                            action.link.startsWith("${ContentResolver.SCHEME_CONTENT}:")
                        ) {
                            nav(LocalWorkRoute(action.link))
                        } else {
                            nav(WorkRoute(action.link))
                        }

                    is HomeAction.OpenComments -> nav(CommentsRoute(action.link))
                    is HomeAction.OpenLink -> openLink(action.href)
                    HomeAction.OpenSettings -> nav(SettingsRoute)
                    HomeAction.OpenHelp -> nav(HelpRoute)
                    HomeAction.OpenLogin -> nav(LoginRoute)
                    is HomeAction.OpenLocalFile -> nav(LocalWorkRoute(action.uri))
                }
            })
        }
        composable<NewestRoute> {
            NewestScreen(
                onAuthor = { link -> navController.navigate(AuthorRoute(link)) },
                onOpenWork = { link -> navController.navigate(WorkRoute(link)) },
            )
        }
        composable<AuthorRoute> { entry ->
            val route = entry.toRoute<AuthorRoute>()
            val uriHandler = LocalUriHandler.current
            val nav = { r: Any -> if (entry.isResumed()) navController.navigate(r) }
            AuthorScreen(
                link = route.link,
                onAction = { action ->
                    when (action) {
                        AuthorAction.Back -> if (entry.isResumed()) navController.popBackStack()
                        is AuthorAction.OpenWork -> nav(WorkRoute(action.link))
                        is AuthorAction.OpenAuthor -> nav(AuthorRoute(action.link))
                        is AuthorAction.OpenComments -> nav(CommentsRoute(action.link))
                        is AuthorAction.EditAbout -> nav(AboutEditRoute(action.link))
                        is AuthorAction.EditDir -> nav(DirEditRoute(action.link))
                        is AuthorAction.EditGroups -> nav(GroupEditRoute(action.link))
                        is AuthorAction.AddWork -> nav(WorkEditRoute(action.link))
                        is AuthorAction.EditWork -> nav(WorkEditRoute(action.link, action.file))
                        is AuthorAction.OpenLink ->
                            if (entry.isResumed()) {
                                navigateSamlib(
                                    navController,
                                    uriHandler,
                                    action.href,
                                )
                            }
                    }
                },
            )
        }
        composable<WorkRoute> { entry ->
            val route = entry.toRoute<WorkRoute>()
            val uriHandler = LocalUriHandler.current
            val nav = { r: Any -> if (entry.isResumed()) navController.navigate(r) }
            WorkScreen(
                link = route.link,
                scrollTo = route.scrollTo,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
                onOpenWork = { l -> nav(WorkRoute(l)) },
                onOpenAuthor = { l -> nav(AuthorRoute(l)) },
                onOpenLink = { href ->
                    if (entry.isResumed()) {
                        navigateSamlib(
                            navController,
                            uriHandler,
                            href,
                        )
                    }
                },
                onOpenSettings = { nav(SettingsRoute) },
                onOpenIllustrations = {
                    val indexLink =
                        route.link.replace(
                            SamlibLinkFormat.WORK_SUFFIX,
                            "/index${SamlibLinkFormat.WORK_SUFFIX}",
                        )
                    nav(IllustrationsRoute(SamlibLinkFormat.ILLUSTRATIONS_PREFIX + indexLink))
                },
                onOpenSearch = { fromIndex -> nav(WorkSearchRoute(link = route.link, fromIndex = fromIndex)) },
            )
        }
        composable<WorkSearchRoute> { entry ->
            val route = entry.toRoute<WorkSearchRoute>()
            WorkSearchScreen(
                link = route.link,
                uri = route.uri,
                fromIndex = route.fromIndex,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
                onOpenResult = { blockIndex ->
                    if (entry.isResumed()) {
                        // Поиск модальный: убираем экран поиска и пред-поисковую читалку из
                        // стека, чтобы «назад» из читалки не открывал поиск, а вёл на главную.
                        val link = route.link
                        val uri = route.uri
                        if (link != null) {
                            navController.navigate(WorkRoute(link, blockIndex)) {
                                popUpTo<WorkRoute> { inclusive = true }
                            }
                        } else if (uri != null) {
                            navController.navigate(LocalWorkRoute(uri, blockIndex)) {
                                popUpTo<LocalWorkRoute> { inclusive = true }
                            }
                        }
                    }
                },
            )
        }
        composable<SettingsRoute> { entry ->
            SettingsScreen(onBack = { if (entry.isResumed()) navController.popBackStack() })
        }
        composable<CommentsRoute> { entry ->
            val route = entry.toRoute<CommentsRoute>()
            val uriHandler = LocalUriHandler.current
            CommentsScreen(
                link = route.link,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
                onOpenLink = { href ->
                    if (entry.isResumed()) {
                        navigateSamlib(
                            navController,
                            uriHandler,
                            href,
                        )
                    }
                },
            )
        }
        composable<IllustrationsRoute> { entry ->
            val route = entry.toRoute<IllustrationsRoute>()
            IllustrationsScreen(
                link = route.link,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
            )
        }
        composable<HelpRoute> { entry ->
            HelpScreen(onBack = { if (entry.isResumed()) navController.popBackStack() })
        }
        composable<LoginRoute> { entry ->
            LoginScreen(
                onAction = { action ->
                    when (action) {
                        LoginAction.Back -> if (entry.isResumed()) navController.popBackStack()
                        is LoginAction.OpenAuthor ->
                            if (entry.isResumed()) {
                                navController.navigate(AuthorRoute(action.authorLink)) {
                                    popUpTo(LoginRoute) { inclusive = true }
                                }
                            }
                    }
                },
            )
        }
        composable<UnsupportedRoute> { entry ->
            val route = entry.toRoute<UnsupportedRoute>()
            UnsupportedScreen(
                url = route.url,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
            )
        }
        composable<AboutEditRoute> { entry ->
            val route = entry.toRoute<AboutEditRoute>()
            AboutEditScreen(
                authorLink = route.authorLink,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
            )
        }
        composable<DirEditRoute> { entry ->
            val route = entry.toRoute<DirEditRoute>()
            DirEditScreen(
                authorLink = route.authorLink,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
            )
        }
        composable<GroupEditRoute> { entry ->
            val route = entry.toRoute<GroupEditRoute>()
            GroupEditScreen(
                authorLink = route.authorLink,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
            )
        }
        composable<WorkEditRoute> { entry ->
            val route = entry.toRoute<WorkEditRoute>()
            WorkEditScreen(
                authorLink = route.authorLink,
                workFile = route.workFile,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
            )
        }
        composable<LocalWorkRoute> { entry ->
            val route = entry.toRoute<LocalWorkRoute>()
            LocalWorkScreen(
                uri = route.uri,
                scrollTo = route.scrollTo,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
                onOpenSettings = { if (entry.isResumed()) navController.navigate(SettingsRoute) },
                onOpenSearch = { fromIndex ->
                    if (entry.isResumed()) {
                        navController.navigate(WorkSearchRoute(uri = route.uri, fromIndex = fromIndex))
                    }
                },
            )
        }
    }
}
