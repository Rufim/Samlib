package ru.samlib.client.ui.work

/**
 * Запуск media-сессии TTS (`TtsService`, физически в `:app`) — развязка `:ui` от конкретного класса.
 * Без параметра контекста: реализация в `:app` захватывает его сама при создании (см. DI) — так
 * вызывать можно и из ViewModel, а не только из composable с LocalContext.
 */
fun interface TtsServiceLauncher {
    fun start()
}
