package ru.samlib.client.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.samlib.client.domain.model.Work

class FilterHistoryUseCaseTest {
    private val useCase = FilterHistoryUseCase()

    private fun work(
        link: String,
        title: String,
        author: String,
        pinned: Boolean = false,
    ) = Work(link = link, title = title, authorShortName = author, pinned = pinned)

    @Test
    fun `пустой запрос и onlyPinned=false возвращает всё`() {
        val list = listOf(work("/a", "Мышь", "Иванов"), work("/b", "Кот", "Петров"))
        assertEquals(list, useCase(list, query = "", onlyPinned = false))
    }

    @Test
    fun `фильтрует по названию и автору без учёта регистра`() {
        val list = listOf(work("/a", "Мышь", "Иванов"), work("/b", "Кот", "Петров"))
        assertEquals(listOf("/b"), useCase(list, query = "кот", onlyPinned = false).map { it.link })
        assertEquals(listOf("/a"), useCase(list, query = "иванов", onlyPinned = false).map { it.link })
    }

    @Test
    fun `onlyPinned оставляет только закреплённые`() {
        val list =
            listOf(
                work("/a", "Мышь", "Иванов", pinned = false),
                work("/b", "Кот", "Петров", pinned = true),
            )
        assertEquals(listOf("/b"), useCase(list, query = "", onlyPinned = true).map { it.link })
    }
}
