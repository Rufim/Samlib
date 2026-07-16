package ru.samlib.client.data.testfixtures

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import ru.samlib.client.data.db.AuthorDao
import ru.samlib.client.data.db.AuthorEntity

class FakeAuthorDao : AuthorDao {
    private val map = linkedMapOf<String, AuthorEntity>()
    private val state = MutableStateFlow<List<AuthorEntity>>(emptyList())

    private fun publish() {
        state.value = map.values.toList()
    }

    override suspend fun upsert(author: AuthorEntity) {
        map[author.link] = author
        publish()
    }

    override suspend fun delete(link: String) {
        map.remove(link)
        publish()
    }

    override suspend fun get(link: String): AuthorEntity? = map[link]

    override fun observeObservable(): Flow<List<AuthorEntity>> = state.map { list -> list.filter { it.isObservable } }

    override suspend fun setUpdateFlags(
        link: String,
        hasUpdates: Boolean,
        notNotified: Boolean,
    ) {
        map[link]?.let { map[link] = it.copy(hasUpdates = hasUpdates, notNotified = notNotified) }
        publish()
    }

    override suspend fun clearNotNotified(links: List<String>) {
        links.forEach { link -> map[link]?.let { map[link] = it.copy(notNotified = false) } }
        publish()
    }
}
