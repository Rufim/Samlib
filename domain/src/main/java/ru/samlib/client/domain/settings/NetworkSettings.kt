package ru.samlib.client.domain.settings

/** Сетевые настройки: домен samlib и HTTP-прокси «host:port» (пусто = без прокси). */
data class NetworkSettings(
    val domain: String = "",
    val proxy: String = "",
)
