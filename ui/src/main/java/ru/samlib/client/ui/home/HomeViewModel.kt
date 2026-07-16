package ru.samlib.client.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import ru.samlib.client.domain.repository.AuthRepository
import ru.samlib.client.domain.repository.AuthorRepository
import ru.samlib.client.domain.repository.OfflineStatus
import ru.samlib.client.domain.samlib.SamlibUrls
import ru.samlib.client.domain.usecase.PickLocalFileUseCase

/**
 * Состояние главного экрана: сеть (офлайн), сессия и профиль в шапке шторки.
 * Выбор вкладки и модель меню остаются в UI (иконки/строки — ресурсы Compose).
 */
class HomeViewModel(
    private val offlineManager: OfflineStatus,
    private val authRepo: AuthRepository,
    private val authorRepo: AuthorRepository,
    private val pickLocalFile: PickLocalFileUseCase,
) : ViewModel(),
    ContainerHost<HomeState, Nothing> {
    override val container = viewModelScope.container<HomeState, Nothing>(HomeState())

    /** MIME-типы для системного пикера книг (для запуска SAF-launcher в UI). */
    val fileMimeTypes: Array<String> get() = pickLocalFile.mimeTypes

    fun observeOffline() =
        intent {
            offlineManager.offline.collect { offline -> reduce { state.copy(offline = offline) } }
        }

    /**
     * Профиль шапки: имя и аватар аккаунта. Аватар грузим только онлайн и при наличии URL —
     * поэтому пересобираем при изменении и логина, и офлайна.
     */
    fun observeProfile() =
        intent {
            combine(authRepo.isLoggedIn, offlineManager.offline) { logged, offline -> logged to offline }
                .collect { (loggedIn, offline) ->
                    if (!loggedIn) {
                        reduce { state.copy(loggedIn = false, accountName = "", avatarUrl = null) }
                    } else {
                        val acc = authRepo.currentAccount()
                        val avatar =
                            if (!offline && acc.url.isNotBlank()) {
                                val path = acc.url.substringAfter(SamlibUrls.HOST).ifBlank { acc.url }
                                authorRepo.avatarOf(path)
                            } else {
                                null
                            }
                        reduce { state.copy(loggedIn = true, accountName = acc.name, avatarUrl = avatar) }
                    }
                }
        }

    /** Перепроверить доступность хоста (нажатие на статус сети). */
    fun refresh() = offlineManager.refresh()

    /**
     * Выбран локальный файл: закрепляем доступ на чтение и кладём URI в состояние.
     * Навигацию UI запускает из [HomeState.pendingFileUri] (результат приходит до RESUMED).
     */
    fun onFilePicked(uri: Uri) =
        intent {
            pickLocalFile.persistReadAccess(uri.toString())
            reduce { state.copy(pendingFileUri = uri.toString()) }
        }

    /** Отметить, что отложенная навигация к файлу выполнена. */
    fun consumePendingFile() = intent { reduce { state.copy(pendingFileUri = null) } }
}
