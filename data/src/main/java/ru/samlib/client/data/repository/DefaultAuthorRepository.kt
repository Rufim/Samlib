package ru.samlib.client.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import ru.samlib.client.data.db.AuthorDao
import ru.samlib.client.data.db.AuthorEntity
import ru.samlib.client.data.db.ObservedWorkDao
import ru.samlib.client.data.db.ObservedWorkEntity
import ru.samlib.client.data.mapper.toDomain
import ru.samlib.client.data.mapper.toEntity
import ru.samlib.client.data.parser.AuthorParser
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.remote.samlibCatching
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.model.Category
import ru.samlib.client.domain.model.ObservedWork
import ru.samlib.client.domain.repository.AuthorRepository
import ru.samlib.client.domain.repository.SamlibResult

/** Не более скольких авторов/книг проверять на обновления одновременно. */
private const val MAX_PARALLEL_CHECKS = 4

class DefaultAuthorRepository(
    private val http: SamlibHttpClient,
    private val parser: AuthorParser,
    private val dao: AuthorDao,
    private val observedDao: ObservedWorkDao,
    private val workParser: WorkParser,
) : AuthorRepository {
    /** Только аватарка со страницы автора (без подгрузки разделов) — для шапки шторки. */
    override suspend fun avatarOf(link: String): String? =
        runCatching { parser.parse(http.getPage(link), link).avatarUrl }.getOrNull()

    override suspend fun getAuthor(link: String): SamlibResult<Author> =
        samlibCatching {
            val author = parser.parse(http.getPage(link), link)
            val enriched =
                coroutineScope {
                    author.categories
                        .map { cat ->
                            async {
                                val catLink = cat.link
                                if (catLink != null && cat.works.isEmpty()) {
                                    try {
                                        cat.copy(
                                            works =
                                                parser.parseWorksFromFragment(
                                                    http.getPage(catLink),
                                                    link,
                                                ),
                                        )
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        cat
                                    }
                                } else {
                                    cat
                                }
                            }
                        }.awaitAll()
                }
            author.copy(categories = enriched, works = enriched.flatMap { it.works })
        }

    override fun observeFollowed(): Flow<List<Author>> =
        dao.observeObservable().map { list -> list.map { it.toDomain() } }

    /** Подписка на автора + baseline его книг (без пометок — чтобы не было ложных «new»). */
    override suspend fun follow(author: Author) {
        dao.upsert(author.toEntity(isObservable = true))
        val baseline = baselineEntities(author)
        if (baseline.isNotEmpty()) observedDao.upsertAll(baseline)
    }

    override suspend fun unfollow(link: String) {
        dao.delete(link)
        observedDao.deleteByAuthor(link)
    }

    /** Снимок книг автора для observed_works (категория нужна для пометки разделов). */
    private fun baselineEntities(author: Author): List<ObservedWorkEntity> {
        val cats =
            author.categories.ifEmpty { listOf(Category(title = null, works = author.works)) }
        return cats.flatMap { cat ->
            cat.works.map { w ->
                ObservedWorkEntity(
                    link = w.link,
                    authorLink = author.link,
                    authorName = author.fullName,
                    title = w.title,
                    categoryTitle = cat.title,
                    size = w.size ?: 0,
                    updateDate = (w.updateDate ?: author.lastUpdateDate)?.time,
                )
            }
        }
    }

    /**
     * Проверяет отслеживаемых авторов и standalone-книги на изменения.
     * Детекция per-book по размеру (адаптация legacy AuthorParser.merge):
     * новая книга или изменившийся размер → changed/sizeDiff. Первая проверка
     * автора (нет сохранённых книг) — базовая, без пометок и уведомлений.
     * Возвращает впервые изменившиеся книги — для уведомлений.
     */
    override suspend fun checkFollowedUpdates(): List<ObservedWork> {
        val now = System.currentTimeMillis()
        // Параллельно, но не более MAX_PARALLEL_CHECKS одновременно — не даём
        // залпу запросов забить пул потоков и сам samlib.
        val sem = Semaphore(MAX_PARALLEL_CHECKS)
        val byAuthor =
            coroutineScope {
                dao
                    .observeObservable()
                    .first()
                    .map { e ->
                        async { sem.withPermit { checkAuthor(e, now) } }
                    }.awaitAll()
            }.flatten()
        val standalone =
            coroutineScope {
                observedDao
                    .getStandalone()
                    .map { s ->
                        async { sem.withPermit { checkStandalone(s, now) } }
                    }.awaitAll()
            }.filterNotNull()
        return (byAuthor + standalone).map { it.toDomain() }
    }

    /** Проверка одного отслеживаемого автора; возвращает впервые изменившиеся книги. */
    private suspend fun checkAuthor(
        e: AuthorEntity,
        now: Long,
    ): List<ObservedWorkEntity> {
        val res = getAuthor(e.link)
        if (res !is SamlibResult.Success) return emptyList()
        val a = res.data.copy(link = e.link, fullName = res.data.fullName ?: e.fullName)
        val saved = observedDao.getByAuthor(e.link).associateBy { it.link }
        val fresh = baselineEntities(a)
        val isBaseline = saved.isEmpty()
        val merged =
            fresh.map { f ->
                val old = saved[f.link]
                when {
                    isBaseline -> f
                    old == null -> f.copy(changed = true, sizeDiff = f.size, changedDate = now)
                    f.size > 0 && old.size > 0 && f.size != old.size ->
                        old.copy(
                            title = f.title,
                            categoryTitle = f.categoryTitle,
                            authorName = f.authorName,
                            size = f.size,
                            updateDate = f.updateDate ?: old.updateDate,
                            changed = true,
                            sizeDiff = f.size - old.size,
                            changedDate = now,
                        )

                    else ->
                        old.copy(
                            title = f.title,
                            categoryTitle = f.categoryTitle,
                            authorName = f.authorName,
                            updateDate = f.updateDate ?: old.updateDate,
                        )
                }
            }
        observedDao.upsertAll(merged)
        val newlyChanged =
            merged.filter { it.changed && !it.excluded && saved[it.link]?.changed != true }
        val hasNew = newlyChanged.isNotEmpty()
        dao.upsert(
            AuthorEntity(
                link = e.link,
                fullName = a.fullName,
                shortName = e.shortName,
                lastUpdateDate = a.lastUpdateDate?.time ?: e.lastUpdateDate,
                lastCheckedDate = now,
                workCount = a.workCount ?: e.workCount,
                isObservable = true,
                hasUpdates = e.hasUpdates || hasNew,
                notNotified = e.notNotified || hasNew,
            ),
        )
        return newlyChanged
    }

    /**
     * Проверка standalone-книги по дате «изменен» и размеру со страницы книги.
     * Возвращает пометку об изменении либо null (нет изменений/страница недоступна).
     */
    private suspend fun checkStandalone(
        s: ObservedWorkEntity,
        now: Long,
    ): ObservedWorkEntity? {
        val w =
            runCatching { workParser.parse(http.getPage(s.link), s.link) }.getOrNull()
                ?: return null
        val newDate = w.updateDate?.time
        val newSize = w.size ?: 0
        val oldDate = s.updateDate
        val dateChanged = newDate != null && oldDate != null && newDate > oldDate
        val sizeChanged = newSize > 0 && s.size > 0 && newSize != s.size
        val upd =
            s.copy(
                title = s.title ?: w.title,
                authorName = s.authorName ?: w.authorShortName,
                size = if (newSize > 0) newSize else s.size,
                updateDate = newDate ?: s.updateDate,
            )
        return if ((dateChanged || sizeChanged) && !s.changed) {
            val marked = upd.copy(changed = true, sizeDiff = newSize - s.size, changedDate = now)
            observedDao.upsert(marked)
            marked
        } else {
            observedDao.upsert(upd)
            null
        }
    }

    // --- API для UI ---

    override fun observeStandaloneWorks(): Flow<List<ObservedWork>> =
        observedDao.observeStandalone().map { list -> list.map { it.toDomain() } }

    override fun observeAuthorUpdates(authorLink: String): Flow<List<ObservedWork>> =
        observedDao.observeByAuthor(authorLink).map { list -> list.map { it.toDomain() } }

    /** «Просмотрено» по автору: снимает пометки со всех его книг и бейдж. */
    override suspend fun markSeen(authorLink: String) {
        observedDao.clearChangedByAuthor(authorLink)
        dao.setUpdateFlags(authorLink, hasUpdates = false, notNotified = false)
    }

    /** «Просмотрено» по одной книге (standalone в списке отслеживаемых). */
    override suspend fun markWorkSeen(link: String) = observedDao.clearChanged(link)

    /** Открытие произведения: снять пометку; если у автора не осталось — снять бейдж. */
    override suspend fun markWorkOpened(link: String) {
        val w = observedDao.get(link) ?: return
        observedDao.clearChanged(link)
        if (observedDao.countChangedByAuthor(w.authorLink) == 0) {
            dao.setUpdateFlags(w.authorLink, hasUpdates = false, notNotified = false)
        }
    }

    override suspend fun deleteObservedWork(link: String) = observedDao.delete(link)

    override suspend fun restoreObservedWork(w: ObservedWork) = observedDao.upsert(w.toEntity())

    override suspend fun followedAuthor(link: String): Author? = dao.get(link)?.toDomain()

    override suspend fun observedWorksOf(link: String): List<ObservedWork> =
        observedDao.getByAuthor(link).map { it.toDomain() }

    /** Восстановление после свайп-удаления (отмена в снек-баре). */
    override suspend fun restoreAuthor(
        author: Author,
        works: List<ObservedWork>,
    ) {
        dao.upsert(author.toEntity(isObservable = true, hasUpdates = author.hasUpdates))
        if (works.isNotEmpty()) observedDao.upsertAll(works.map { it.toEntity() })
    }

    override suspend fun clearNotNotified(links: List<String>) = dao.clearNotNotified(links)

    /** Отслеживается ли книга: есть запись и не исключена, либо отслеживается её автор. */
    override fun observeWorkObserved(
        link: String,
        authorLink: String,
    ): Flow<Boolean> =
        combine(observedDao.observe(link), dao.observeObservable()) { rec, followed ->
            when {
                rec != null -> !rec.excluded
                else -> followed.any { it.link == authorLink }
            }
        }

    /**
     * Чекбокс «Отслеживать книгу» в меню читалки.
     * Автор отслеживается → исключить/вернуть книгу (excluded).
     * Автор не отслеживается → добавить/убрать standalone (baseline со страницы книги).
     */
    override suspend fun toggleObserveWork(
        link: String,
        authorLink: String,
        title: String?,
    ) {
        val existing = observedDao.get(link)
        val authorFollowed = dao.get(authorLink)?.isObservable == true
        when {
            existing != null && authorFollowed ->
                observedDao.upsert(existing.copy(excluded = !existing.excluded))

            existing != null -> observedDao.delete(link)
            authorFollowed ->
                observedDao.upsert(
                    ObservedWorkEntity(
                        link = link,
                        authorLink = authorLink,
                        title = title,
                        excluded = true,
                    ),
                )

            else -> {
                val w = runCatching { workParser.parse(http.getPage(link), link) }.getOrNull()
                observedDao.upsert(
                    ObservedWorkEntity(
                        link = link,
                        authorLink = authorLink,
                        authorName = w?.authorShortName,
                        title = title ?: w?.title,
                        size = w?.size ?: 0,
                        updateDate = w?.updateDate?.time,
                        isStandalone = true,
                    ),
                )
            }
        }
    }
}
