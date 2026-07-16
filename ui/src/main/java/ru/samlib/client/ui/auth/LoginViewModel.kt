package ru.samlib.client.ui.auth

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.repository.AuthRepository
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.samlib.SamlibUrls

@Immutable
data class LoginState(
    val loggedIn: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
)

sealed interface LoginSideEffect {
    /** [authorLink] — путь личной страницы автора (может быть пустым). */
    data class Success(
        val authorLink: String,
    ) : LoginSideEffect
}

sealed interface LoginAction {
    data object Back : LoginAction

    data class OpenAuthor(
        val authorLink: String,
    ) : LoginAction
}

class LoginViewModel(
    private val auth: AuthRepository,
) : ViewModel(),
    ContainerHost<LoginState, LoginSideEffect> {
    override val container = viewModelScope.container<LoginState, LoginSideEffect>(LoginState())

    fun observe() =
        intent {
            auth.isLoggedIn.collect { logged -> reduce { state.copy(loggedIn = logged) } }
        }

    fun login(
        login: String,
        password: String,
    ) = intent {
        if (login.isBlank() || password.isBlank()) {
            reduce { state.copy(error = "Введите логин и пароль") }
            return@intent
        }
        reduce { state.copy(loading = true, error = null) }
        when (val result = auth.login(login, password)) {
            is SamlibResult.Success -> {
                reduce { state.copy(loading = false, error = null) }
                postSideEffect(LoginSideEffect.Success(authorLink()))
            }

            is SamlibResult.Error ->
                reduce {
                    state.copy(loading = false, error = result.cause?.message ?: "Не удалось войти")
                }
        }
    }

    /** Открыть свою страницу автора (для кнопки «Моя страница» уже вошедшему). */
    fun openMyPage() = intent { postSideEffect(LoginSideEffect.Success(authorLink())) }

    private suspend fun authorLink(): String {
        val url = auth.currentAccount().url
        return url.substringAfter(SamlibUrls.HOST, "").ifBlank { url }
    }

    fun logout() = intent { auth.logout() }
}
