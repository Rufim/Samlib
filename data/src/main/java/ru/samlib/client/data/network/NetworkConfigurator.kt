package ru.samlib.client.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.domain.repository.ReaderSettingsRepository

/**
 * Применяет сетевые настройки (домен samlib, HTTP-прокси) к [SamlibHttpClient]
 * при старте и при их изменении. Создаётся eagerly (Koin createdAtStart).
 */
class NetworkConfigurator(
    private val http: SamlibHttpClient,
    settings: ReaderSettingsRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    init {
        scope.launch {
            settings.network.collect { http.reconfigure(it.domain, it.proxy) }
        }
    }
}
