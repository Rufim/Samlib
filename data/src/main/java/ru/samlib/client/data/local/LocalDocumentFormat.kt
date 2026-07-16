package ru.samlib.client.data.local

/**
 * Расширения локальных файлов книг — независимо распознаются в `parseLocalDocument`
 * (LocalFileRepository.kt), [LegacyImporter] и `ReaderFiles.mimeOf`; единая точка
 * вместо трижды продублированных строк-суффиксов. Тег [LocalDocument.format] —
 * это соответствующая константа без ведущей точки (`EXT_FB2.removePrefix(".")`).
 */
object LocalDocumentFormat {
    const val EXT_FB2_ZIP = ".fb2.zip"
    const val EXT_FB2 = ".fb2"
    const val EXT_EPUB = ".epub"
    const val EXT_DOCX = ".docx"
    const val EXT_DOC = ".doc"
    const val EXT_HTML = ".html"
    const val EXT_HTM = ".htm"
    const val EXT_TXT = ".txt"
}
