package ru.samlib.client.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.remote.samlibCatching
import ru.samlib.client.domain.repository.AccountInfo
import ru.samlib.client.domain.repository.AuthRepository
import ru.samlib.client.domain.repository.SamlibResult

class DefaultAuthRepository(
    private val dataStore: DataStore<Preferences>,
    private val http: SamlibHttpClient,
) : AuthRepository {
    private val cookieKey = stringPreferencesKey("login_cookie")

    override val loginCookie: Flow<String?> = dataStore.data.map { it[cookieKey]?.ifBlank { null } }
    override val isLoggedIn: Flow<Boolean> = loginCookie.map { !it.isNullOrBlank() }

    override suspend fun currentCookie(): String = loginCookie.first().orEmpty()

    override suspend fun currentAccount(): AccountInfo = http.accountInfo(currentCookie())

    override suspend fun login(
        login: String,
        password: String,
    ): SamlibResult<Unit> =
        samlibCatching {
            val cookie = http.login(login.trim(), password) ?: error("Неверный логин или пароль")
            dataStore.edit { it[cookieKey] = cookie }
        }

    override suspend fun logout() {
        dataStore.edit { it.remove(cookieKey) }
    }
}
