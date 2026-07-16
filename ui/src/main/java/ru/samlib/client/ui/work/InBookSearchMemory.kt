package ru.samlib.client.ui.work

/**
 * Память поиска по книге, живущая на уровне приложения (Koin single). Экран поиска —
 * модальный: при выборе результата он убирается из бэкстека, поэтому его ViewModel
 * пересоздаётся при следующем открытии. Здесь сохраняются запрос, результаты и позиция
 * списка для конкретной книги, чтобы повторное открытие поиска (иконка 🔍) не гоняло
 * заново сеть/парсинг/поиск, а мгновенно восстановило то же самое состояние — до тех пор,
 * пока не закрыта сама книга (см. [clear], вызывается из onLeave читалки).
 */
class InBookSearchMemory {
    var key: String? = null
        private set
    var query: String = ""
    var firstVisibleIndex: Int = 0
    var firstVisibleOffset: Int = 0

    // Кеш текста книги и последних результатов поиска — реюзается, пока книга не закрыта.
    var texts: List<String> = emptyList()
    var textsLower: List<String> = emptyList()
    var textLoaded: Boolean = false
    var hits: List<SearchHit> = emptyList()
    var truncated: Boolean = false

    /** Переключиться на книгу [k]; при смене книги прежнее состояние сбрасывается. */
    fun forBook(k: String) {
        if (key != k) {
            key = k
            reset()
        }
    }

    /** Полный сброс при выходе из книги — следующее открытие поиска не должно унаследовать чужой контекст. */
    fun clear() {
        key = null
        reset()
    }

    private fun reset() {
        query = ""
        firstVisibleIndex = 0
        firstVisibleOffset = 0
        texts = emptyList()
        textsLower = emptyList()
        textLoaded = false
        hits = emptyList()
        truncated = false
    }
}
