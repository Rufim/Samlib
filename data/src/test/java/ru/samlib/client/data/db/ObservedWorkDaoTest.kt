package ru.samlib.client.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ObservedWorkDaoTest {
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
    fun `книги автора сохраняются и пометки сбрасываются`() =
        runTest {
            val dao = db.observedWorkDao()
            dao.upsertAll(
                listOf(
                    ObservedWorkEntity(
                        link = "/a/x/w1.shtml",
                        authorLink = "/a/x/",
                        changed = true,
                        sizeDiff = 3,
                    ),
                    ObservedWorkEntity(link = "/a/x/w2.shtml", authorLink = "/a/x/"),
                ),
            )
            assertEquals(1, dao.countChangedByAuthor("/a/x/"))
            dao.clearChangedByAuthor("/a/x/")
            assertEquals(0, dao.countChangedByAuthor("/a/x/"))
            assertTrue(dao.getByAuthor("/a/x/").none { it.changed || it.sizeDiff != 0 })
        }

    @Test
    fun `deleteByAuthor не трогает standalone-книги`() =
        runTest {
            val dao = db.observedWorkDao()
            dao.upsert(ObservedWorkEntity(link = "/a/x/w1.shtml", authorLink = "/a/x/"))
            dao.upsert(
                ObservedWorkEntity(
                    link = "/a/x/w2.shtml",
                    authorLink = "/a/x/",
                    isStandalone = true,
                ),
            )
            dao.deleteByAuthor("/a/x/")
            assertEquals(listOf("/a/x/w2.shtml"), dao.getStandalone().map { it.link })
            assertTrue(dao.observeStandalone().first().any { it.link == "/a/x/w2.shtml" })
        }

    @Test
    fun `флаги обновлений автора выставляются и сбрасываются`() =
        runTest {
            db.authorDao().upsert(
                AuthorEntity("/a/x/", "Иван", "Иванов", null, null, 1, isObservable = true),
            )
            db.authorDao().setUpdateFlags("/a/x/", hasUpdates = true, notNotified = true)
            assertTrue(db.authorDao().get("/a/x/")!!.hasUpdates)
            db.authorDao().clearNotNotified(listOf("/a/x/"))
            val e = db.authorDao().get("/a/x/")!!
            assertTrue(e.hasUpdates)
            assertFalse(e.notNotified)
        }
}
