package ru.samlib.client.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.samlib.client.data.db.SamlibDatabase
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LegacyImporterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: SamlibDatabase
    private lateinit var importer: DefaultLegacyImporter
    private lateinit var legacyFile: File
    private lateinit var bookFile: File

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(context, SamlibDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val savedBooks = DefaultSavedBooksRepository(context, db.savedBookDao())
        importer =
            DefaultLegacyImporter(context, db.authorDao(), db.historyDao(), db.bookmarkDao(), savedBooks)

        bookFile = File(context.cacheDir, "legacy_book.txt").apply { writeText("hello") }
        legacyFile = File(context.cacheDir, "Samlib_test.db")
        if (legacyFile.exists()) legacyFile.delete()
        seedLegacyDb(legacyFile)
    }

    @After
    fun tearDown() {
        db.close()
        legacyFile.delete()
        bookFile.delete()
    }

    private fun seedLegacyDb(file: File) {
        val ldb = SQLiteDatabase.openOrCreateDatabase(file, null)
        ldb.execSQL(
            "CREATE TABLE Author(link TEXT PRIMARY KEY, fullName TEXT, shortName TEXT, " +
                "observable INTEGER, deleted INTEGER, lastUpdateDate INTEGER, lastCheckedDate INTEGER, workCount INTEGER)",
        )
        ldb.execSQL(
            "CREATE TABLE Bookmark(workUrl TEXT PRIMARY KEY, authorUrl TEXT, workTitle TEXT, " +
                "authorShortName TEXT, genres TEXT, savedDate INTEGER, indentIndex INTEGER, indent TEXT, userBookmark INTEGER)",
        )
        ldb.execSQL(
            "CREATE TABLE ExternalWork(filePath TEXT PRIMARY KEY, workUrl TEXT, authorUrl TEXT, " +
                "workTitle TEXT, authorShortName TEXT, genres TEXT, mimeType TEXT, savedDate INTEGER)",
        )
        // подписки: одна отслеживаемая, одна нет (должна пропуститься)
        ldb.execSQL(
            "INSERT INTO Author VALUES('/k/kazancew_d/','Казанцев Дмитрий','Казанцев Д.',1,0,1700000000000,1700000000,4)",
        )
        ldb.execSQL("INSERT INTO Author VALUES('/x/xxx/','Некто','Н.',0,0,1699000000000,1699000000,2)")
        // история/позиции: полные URL с доменом
        ldb.execSQL(
            "INSERT INTO Bookmark VALUES('http://samlib.ru/k/kazancew_d/test.shtml','http://samlib.ru/k/kazancew_d/'," +
                "'Тест','Казанцев Д.','История',1700000001000,5,'',0)",
        )
        ldb.execSQL(
            "INSERT INTO Bookmark VALUES('http://samlib.ru/b/bulgakow/master.shtml','http://samlib.ru/b/bulgakow/'," +
                "'Мастер','Булгаков','Проза',1700000002000,12,'',1)",
        )
        // сохранённая книга — реальный читаемый файл
        ldb.execSQL(
            "INSERT INTO ExternalWork VALUES('${bookFile.absolutePath}','http://samlib.ru/o/old/book.shtml'," +
                "'http://samlib.ru/o/old/','Старая книга','Автор Старый','Фантастика','text/plain',1700000003000)",
        )
        ldb.close()
    }

    @Test
    fun `импортирует подписки, историю с позицией и сохранённые книги`() =
        runTest {
            val report = importer.import(legacyFile)

            assertEquals(1, report.subscriptions) // не-observable автор пропущен
            assertEquals(2, report.history)
            assertEquals(1, report.savedBooks)

            // подписки
            val authors = db.authorDao().observeObservable().first()
            assertEquals(listOf("/k/kazancew_d/"), authors.map { it.link })
            assertEquals("Казанцев Дмитрий", authors.first().fullName)
            // lastCheckedDate из секунд переведён в миллисекунды
            assertEquals(1700000000000L, authors.first().lastCheckedDate)

            // история: полные URL нормализованы до относительных ссылок
            val history =
                db
                    .historyDao()
                    .observeHistory()
                    .first()
                    .map { it.link }
                    .toSet()
            assertEquals(setOf("/k/kazancew_d/test.shtml", "/b/bulgakow/master.shtml"), history)

            // позиция чтения перенесена как авто-закладка
            assertEquals(5, db.workBookmarkPosition("/k/kazancew_d/test.shtml"))

            // сохранённые
            val saved = db.savedBookDao().getAll()
            assertEquals(1, saved.size)
            assertEquals("Старая книга", saved.first().title)
            assertEquals("txt", saved.first().format)
        }

    @Test
    fun `defaultLegacyDb возвращает null, если файла нет`() {
        context.getDatabasePath("Samlib.db").delete()
        assertTrue(importer.defaultLegacyDb() == null)
    }

    /**
     * До Scoped Storage (Android ≤9) «Сохранённые» книги могли лежать на публичном внешнем
     * хранилище, а не в приватной директории приложения (см. READ_EXTERNAL_STORAGE
     * maxSdkVersion=32 в манифесте). Эмулируем такой путь — вне files/library и вне cacheDir —
     * и проверяем, что LegacyImporter в любом случае копирует файл в приватную библиотеку.
     * Реальную проверку прав доступа на физическом устройстве с API ≤29 это не заменяет —
     * Robolectric не эмулирует enforcement READ_EXTERNAL_STORAGE, только сам код копирования.
     */
    @Test
    fun `сохранённая книга с публичного внешнего пути копируется в приватную library`() =
        runTest {
            val publicDir = File(context.cacheDir.parentFile, "public_storage_emulated").apply { mkdirs() }
            val publicFile = File(publicDir, "public_book.fb2").apply { writeText("legacy public content") }
            val publicDb = File(context.cacheDir, "Samlib_public_test.db")
            if (publicDb.exists()) publicDb.delete()
            try {
                seedExternalWorkOnly(publicDb, publicFile)

                val report = importer.import(publicDb)
                assertEquals(1, report.savedBooks)

                val saved = db.savedBookDao().getAll().single { it.title == "Публичная книга" }
                val storedPath = Uri.parse(saved.uri).path
                assertTrue(
                    "файл должен быть скопирован в приватную library, а не остаться по исходному пути",
                    storedPath != null && storedPath.startsWith(File(context.filesDir, "library").absolutePath),
                )
                assertEquals("legacy public content", File(requireNotNull(storedPath)).readText())
            } finally {
                publicDb.delete()
                publicFile.delete()
                publicDir.delete()
            }
        }

    private fun seedExternalWorkOnly(
        file: File,
        bookFile: File,
    ) {
        val ldb = SQLiteDatabase.openOrCreateDatabase(file, null)
        ldb.execSQL(
            "CREATE TABLE ExternalWork(filePath TEXT PRIMARY KEY, workUrl TEXT, authorUrl TEXT, " +
                "workTitle TEXT, authorShortName TEXT, genres TEXT, mimeType TEXT, savedDate INTEGER)",
        )
        ldb.execSQL(
            "INSERT INTO ExternalWork VALUES('${bookFile.absolutePath}','http://samlib.ru/p/public/book.shtml'," +
                "'http://samlib.ru/p/public/','Публичная книга','Автор Публичный'," +
                "'Фантастика','text/plain',1700000004000)",
        )
        ldb.close()
    }

    private suspend fun SamlibDatabase.workBookmarkPosition(link: String): Int? = bookmarkDao().readingPosition(link)
}
