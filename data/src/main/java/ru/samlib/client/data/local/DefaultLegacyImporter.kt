package ru.samlib.client.data.local

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.samlib.client.data.db.AuthorDao
import ru.samlib.client.data.db.AuthorEntity
import ru.samlib.client.data.db.BookmarkDao
import ru.samlib.client.data.db.BookmarkEntity
import ru.samlib.client.data.db.HistoryDao
import ru.samlib.client.data.db.WorkEntity
import ru.samlib.client.domain.repository.LegacyImportReport
import ru.samlib.client.domain.repository.LegacyImporter
import ru.samlib.client.domain.repository.SavedBookInfo
import ru.samlib.client.domain.repository.SavedBooksRepository
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import java.io.File

/**
 * Перенос данных из старой (legacy, DBFlow) БД «Samlib.db» в новую Room-БД:
 *  - подписки: таблица `Author`, `observable = 1` → `authors` (isObservable=1);
 *  - история чтения и позиция «продолжить»: таблица `Bookmark` → `works` (openedDate) + `bookmarks`;
 *  - сохранённые книги: таблица `ExternalWork` → «Сохранённые» (файл копируется в библиотеку).
 *
 * Best-effort: отсутствующие таблицы/столбцы и битые строки молча пропускаются; полные URL
 * приводятся к относительным ссылкам (как в новой схеме) через [SamlibLinkFormat.normalizeLink].
 */
class DefaultLegacyImporter(
    private val context: Context,
    private val authorDao: AuthorDao,
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val savedBooks: SavedBooksRepository,
) : LegacyImporter {
    /** Файл legacy-БД при обновлении поверх (applicationId совпадает) — или null, если его нет. */
    override fun defaultLegacyDb(): File? =
        context.getDatabasePath(LEGACY_DB_NAME).takeIf { it.exists() && it.length() > 0 }

    override suspend fun import(dbFile: File): LegacyImportReport =
        withContext(Dispatchers.IO) {
            val db =
                runCatching {
                    SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                }.getOrNull() ?: return@withContext LegacyImportReport()
            try {
                LegacyImportReport(
                    subscriptions = importSubscriptions(db),
                    history = importHistory(db),
                    savedBooks = importSavedBooks(db),
                )
            } finally {
                runCatching { db.close() }
            }
        }

    /**
     * Отслеживаемые авторы (Author.observable = 1). Переносит только метаданные подписки —
     * `rate`/`votes`/`views`/`avatarUrl`/`about` и т.п. из legacy `Author` сознательно не
     * читаются: [AuthorEntity] хранит исключительно эти метаданные, а "богатый" профиль
     * никогда не кешируется в Room даже для обычного (не legacy) потока — он всегда парсится
     * заново из HTML в `AuthorRepository.getAuthor()`. Завести под них колонки ради
     * одноразового импорта означало бы данные, которые никогда не обновятся повторно.
     */
    private suspend fun importSubscriptions(db: SQLiteDatabase): Int {
        var n = 0
        query(
            db,
            "SELECT link, fullName, shortName, lastUpdateDate, lastCheckedDate, workCount " +
                "FROM Author WHERE observable = 1",
        )?.use { c ->
            while (c.moveToNext()) {
                val link = SamlibLinkFormat.normalizeLink(c.str("link")) ?: continue
                authorDao.upsert(
                    AuthorEntity(
                        link = link,
                        fullName = c.str("fullName"),
                        shortName = c.str("shortName"),
                        lastUpdateDate = c.longOrNull("lastUpdateDate"),
                        lastCheckedDate = c.longOrNull("lastCheckedDate")?.let(::toMillis),
                        workCount = c.intOrNull("workCount"),
                        isObservable = true,
                    ),
                )
                n++
            }
        }
        return n
    }

    /** История + позиция чтения (Bookmark). Каждая запись → произведение в истории и авто-закладка. */
    private suspend fun importHistory(db: SQLiteDatabase): Int {
        var n = 0
        query(
            db,
            "SELECT workUrl, authorUrl, workTitle, authorShortName, genres, savedDate, indentIndex " +
                "FROM Bookmark",
        )?.use { c ->
            while (c.moveToNext()) {
                val workLink = SamlibLinkFormat.normalizeLink(c.str("workUrl")) ?: continue
                val savedDate = c.longOrNull("savedDate") ?: System.currentTimeMillis()
                historyDao.upsert(
                    WorkEntity(
                        link = workLink,
                        title = c.str("workTitle"),
                        authorLink = SamlibLinkFormat.normalizeLink(c.str("authorUrl")),
                        updateDate = null,
                        size = null,
                        openedDate = savedDate,
                        authorShortName = c.str("authorShortName"),
                        genres = c.str("genres"),
                        pinned = false,
                    ),
                )
                // авто-закладка (userBookmark=false) — «продолжить чтение» с последней позиции
                bookmarkDao.upsert(
                    BookmarkEntity(
                        workLink = workLink,
                        indent = c.intOrNull("indentIndex") ?: 0,
                        savedDate = savedDate,
                        userBookmark = false,
                    ),
                )
                n++
            }
        }
        return n
    }

    /** Сохранённые/скачанные книги (ExternalWork). Файл копируется в приватную библиотеку. */
    private suspend fun importSavedBooks(db: SQLiteDatabase): Int {
        var n = 0
        query(
            db,
            "SELECT filePath, workTitle, authorShortName, mimeType, savedDate FROM ExternalWork",
        )?.use { c ->
            while (c.moveToNext()) {
                val path = c.str("filePath") ?: continue
                val file = File(path)
                if (!file.exists() || !file.canRead()) continue
                val title = c.str("workTitle")?.ifBlank { null } ?: file.nameWithoutExtension
                val date = c.longOrNull("savedDate") ?: System.currentTimeMillis()
                val ok =
                    runCatching {
                        savedBooks.record(
                            SavedBookInfo(
                                "${ContentResolver.SCHEME_FILE}://$path",
                                title,
                                c.str("authorShortName"),
                                formatOf(path),
                                date,
                            ),
                        )
                    }.isSuccess
                if (ok) n++
            }
        }
        return n
    }

    private fun query(
        db: SQLiteDatabase,
        sql: String,
    ): Cursor? = runCatching { db.rawQuery(sql, null) }.getOrNull()

    private fun Cursor.str(col: String): String? {
        val i = getColumnIndex(col)
        return if (i < 0 || isNull(i)) null else getString(i)
    }

    private fun Cursor.longOrNull(col: String): Long? {
        val i = getColumnIndex(col)
        return if (i < 0 || isNull(i)) null else getLong(i)
    }

    private fun Cursor.intOrNull(col: String): Int? {
        val i = getColumnIndex(col)
        return if (i < 0 || isNull(i)) null else getInt(i)
    }

    /** legacy хранит lastCheckedDate в секундах, остальные даты — в миллисекундах. */
    private fun toMillis(value: Long): Long = if (value < 100_000_000_000L) value * 1000 else value

    private fun formatOf(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.endsWith(LocalDocumentFormat.EXT_FB2_ZIP) -> LocalDocumentFormat.EXT_FB2_ZIP.removePrefix(".")
            lower.endsWith(LocalDocumentFormat.EXT_FB2) -> LocalDocumentFormat.EXT_FB2.removePrefix(".")
            lower.endsWith(LocalDocumentFormat.EXT_EPUB) -> LocalDocumentFormat.EXT_EPUB.removePrefix(".")
            lower.endsWith(LocalDocumentFormat.EXT_DOCX) -> LocalDocumentFormat.EXT_DOCX.removePrefix(".")
            lower.endsWith(LocalDocumentFormat.EXT_DOC) -> LocalDocumentFormat.EXT_DOC.removePrefix(".")
            lower.endsWith(LocalDocumentFormat.EXT_HTM) || lower.endsWith(LocalDocumentFormat.EXT_HTML) ->
                LocalDocumentFormat.EXT_HTML.removePrefix(".")
            lower.endsWith(LocalDocumentFormat.EXT_TXT) -> LocalDocumentFormat.EXT_TXT.removePrefix(".")
            else -> lower.substringAfterLast('.', "").ifBlank { LocalDocumentFormat.EXT_TXT.removePrefix(".") }
        }
    }

    private companion object {
        // DBFlow добавляет расширение .db к имени БД ("Samlib" → файл "Samlib.db").
        const val LEGACY_DB_NAME = "Samlib.db"
    }
}
