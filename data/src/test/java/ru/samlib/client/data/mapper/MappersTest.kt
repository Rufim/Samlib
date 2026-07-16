package ru.samlib.client.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.model.Work

class MappersTest {
    @Test
    fun `author round-trip сохраняет ключевые поля`() {
        val author =
            Author(link = "/a/x/", fullName = "Иван Иванов", shortName = "Иванов", workCount = 5)
        val restored = author.toEntity(isObservable = true).toDomain()
        assertEquals(author.link, restored.link)
        assertEquals(author.fullName, restored.fullName)
        assertEquals(author.workCount, restored.workCount)
    }

    @Test
    fun `work round-trip сохраняет ключевые поля`() {
        val work = Work(link = "/w/1", title = "Заголовок", authorLink = "/a/x/", size = 12)
        val restored = work.toEntity().toDomain()
        assertEquals(work.link, restored.link)
        assertEquals(work.title, restored.title)
        assertEquals(work.authorLink, restored.authorLink)
        assertEquals(work.size, restored.size)
    }
}
