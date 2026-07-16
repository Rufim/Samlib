package ru.samlib.client.domain.model

/**
 * Движок поиска:
 * - [SAMLIB] — собственный поиск samlib (`/cgi-bin/seek`);
 * - [WEB] — веб-поиск по сайту (DuckDuckGo), парсинг выдачи в списке (WebView только при капче);
 * - [BROWSER] — сразу открыть запрос во внешнем браузере.
 */
enum class SearchEngine(
    val title: String,
) {
    SAMLIB("Самлиб"),
    WEB("Веб парсер"),
    BROWSER("В браузере"),
}
