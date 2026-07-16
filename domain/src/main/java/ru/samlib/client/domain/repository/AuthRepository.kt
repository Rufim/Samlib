package ru.samlib.client.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Хранит cookie сессии samlib и выполняет вход/выход (как в легаси SignInParser).
 */
interface AuthRepository {
    val loginCookie: Flow<String?>
    val isLoggedIn: Flow<Boolean>

    suspend fun currentCookie(): String

    /** Данные аккаунта (имя/почта/адрес) из cookie — для префилла формы комментария. */
    suspend fun currentAccount(): AccountInfo

    suspend fun login(
        login: String,
        password: String,
    ): SamlibResult<Unit>

    suspend fun logout()
}
