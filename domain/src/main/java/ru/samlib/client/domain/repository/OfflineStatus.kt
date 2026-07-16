package ru.samlib.client.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Единый источник состояния сети приложения (развязка от конкретного HTTP-клиента). */
interface OfflineStatus {
    /** true, если приложение сейчас в офлайне (принудительно ИЛИ хост недоступен). */
    val offline: StateFlow<Boolean>

    /** Перепроверить доступность хоста (при старте и по нажатию на статус). */
    fun refresh()
}
