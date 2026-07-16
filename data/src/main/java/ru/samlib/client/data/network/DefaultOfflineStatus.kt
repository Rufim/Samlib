package ru.samlib.client.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.domain.repository.OfflineStatus
import ru.samlib.client.domain.repository.ReaderSettingsRepository

/**
 * Единый источник состояния сети приложения. Приложение считается офлайн, если
 * пользователь принудительно включил офлайн в настройках ИЛИ хост samlib недоступен.
 * В офлайне сетевые разделы прячутся, статус показывается в шторке.
 */
class DefaultOfflineStatus(
    private val http: SamlibHttpClient,
    settings: ReaderSettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : OfflineStatus {
    private val reachable = MutableStateFlow(true)

    /** true, если хост самлиба сейчас недоступен (авто-проверка). */
    val hostReachable: StateFlow<Boolean> = reachable

    override val offline: StateFlow<Boolean> =
        combine(settings.offlineForced, reachable) { forced, reach -> forced || !reach }
            .stateIn(scope, SharingStarted.Eagerly, false)

    override fun refresh() {
        scope.launch { reachable.value = http.isHostReachable() }
    }
}
