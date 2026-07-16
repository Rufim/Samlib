package ru.samlib.client.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private lateinit var db: SamlibDatabase

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    SamlibDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `наблюдаемые авторы сохраняются и читаются`() =
        runTest {
            db.authorDao().upsert(
                AuthorEntity("/a/x/", "Иван Иванов", "Иванов", null, null, 3, isObservable = true),
            )
            db.authorDao().upsert(
                AuthorEntity("/a/y/", "Пётр Петров", "Петров", null, null, 1, isObservable = false),
            )
            val observable = db.authorDao().observeObservable().first()
            assertEquals(1, observable.size)
            assertEquals("/a/x/", observable.first().link)
        }

    @Test
    fun `удаление автора работает`() =
        runTest {
            db.authorDao().upsert(
                AuthorEntity("/a/x/", "Иван", "Иванов", null, null, 0, isObservable = true),
            )
            db.authorDao().delete("/a/x/")
            assertEquals(
                0,
                db
                    .authorDao()
                    .observeObservable()
                    .first()
                    .size,
            )
        }

    @Test
    fun `история сортируется по дате открытия`() =
        runTest {
            db.historyDao().upsert(WorkEntity("/w/1", "A", "/a/x/", null, 5, openedDate = 100))
            db.historyDao().upsert(WorkEntity("/w/2", "B", "/a/x/", null, 5, openedDate = 200))
            val recent = db.historyDao().observeHistory().first()
            assertEquals(listOf("/w/2", "/w/1"), recent.map { it.link })
        }

    @Test
    fun `закладки читаются по работе`() =
        runTest {
            db.bookmarkDao().upsert(
                BookmarkEntity(
                    workLink = "/w/1",
                    indent = 10,
                    savedDate = 1,
                    userBookmark = true,
                ),
            )
            val list = db.bookmarkDao().observeForWork("/w/1").first()
            assertEquals(1, list.size)
            assertEquals(10, list.first().indent)
        }

    @Test
    fun `позиция чтения сохраняется как авто-закладка`() =
        runTest {
            db.bookmarkDao().clearAuto("/w/1")
            db.bookmarkDao().upsert(
                BookmarkEntity(workLink = "/w/1", indent = 42, savedDate = 1, userBookmark = false),
            )
            assertEquals(42, db.bookmarkDao().readingPosition("/w/1"))
        }
}
