package ru.samlib.client.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object NewestRoute

@Serializable
data class AuthorRoute(
    val link: String,
)

@Serializable
data class WorkRoute(
    val link: String,
    val scrollTo: Int = -1,
)

@Serializable
data class LocalWorkRoute(
    val uri: String,
    val scrollTo: Int = -1,
)

/** Поиск по тексту книги: сетевое произведение ([link]) или локальный файл ([uri]). */
@Serializable
data class WorkSearchRoute(
    val link: String? = null,
    val uri: String? = null,
    // текущая позиция чтения на момент открытия — чтобы показать находки дальше по тексту
    val fromIndex: Int = -1,
)

@Serializable
object SettingsRoute

@Serializable
data class CommentsRoute(
    val link: String,
)

@Serializable
data class IllustrationsRoute(
    val link: String,
)

@Serializable
object HelpRoute

@Serializable
object LoginRoute

@Serializable
data class UnsupportedRoute(
    val url: String,
)

@Serializable
data class AboutEditRoute(
    val authorLink: String,
)

@Serializable
data class DirEditRoute(
    val authorLink: String,
)

@Serializable
data class GroupEditRoute(
    val authorLink: String,
)

@Serializable
data class WorkEditRoute(
    val authorLink: String,
    val workFile: String? = null,
)
