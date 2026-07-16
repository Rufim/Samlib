package ru.samlib.client.ui.work

/** Общие пороги поиска по тексту книги/черновика (читалка и редактор). */
object SearchDefaults {
    /** Минимальная длина запроса — короче не ищем. */
    const val MIN_QUERY = 2

    /** Символов контекста до/после совпадения в списке результатов. */
    const val CONTEXT = 40

    /** Максимум находок — дальше сканирование обрывается (truncated). */
    const val MAX_HITS = 500
}
