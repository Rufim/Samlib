package ru.samlib.client.ui.home

import androidx.compose.runtime.Immutable

@Immutable
data class HomeState(
    /** Офлайн: принудительно в настройках или хост недоступен. */
    val offline: Boolean = false,
    /** Есть активная сессия samlib. */
    val loggedIn: Boolean = false,
    /** Имя аккаунта как есть (может быть пустым); подпись «Аноним/Пользователь» решает UI. */
    val accountName: String = "",
    /** Аватар со страницы автора (null — если офлайн/не залогинен/нет). */
    val avatarUrl: String? = null,
    /** Выбранный через SAF файл, ожидающий отложенной навигации (см. HomeScreen). */
    val pendingFileUri: String? = null,
)
