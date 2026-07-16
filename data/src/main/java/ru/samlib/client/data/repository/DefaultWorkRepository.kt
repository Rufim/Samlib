package ru.samlib.client.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.samlib.client.data.db.BookmarkDao
import ru.samlib.client.data.db.BookmarkEntity
import ru.samlib.client.data.db.CachedWorkDao
import ru.samlib.client.data.db.HistoryDao
import ru.samlib.client.data.db.WorkEntity
import ru.samlib.client.data.mapper.toCachedEntity
import ru.samlib.client.data.mapper.toDomain
import ru.samlib.client.data.mapper.toEntity
import ru.samlib.client.data.parser.WorkParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.remote.samlibCatching
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.WorkRepository
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DefaultWorkRepository(
    private val context: Context,
    private val http: SamlibHttpClient,
    private val parser: WorkParser,
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val cachedWorkDao: CachedWorkDao,
) : WorkRepository {
    // Папка для хранения кешированных файлов произведений
    private val cacheDir by lazy {
        File(context.filesDir, "cached_works").also { it.mkdirs() }
    }

    // Позиция чтения живёт в памяти между записью и flush: пишется реактивно на каждый
    // скролл/блок озвучки (дёшево, не может «потеряться» из-за гонки с БД), а в БД
    // переносится только по явному flushReadingPosition — из контрольных точек жизненного
    // цикла (пауза/стоп озвучки, уход с экрана читалки), а не на каждое изменение.
    private val positionCache = ConcurrentHashMap<String, Int>()

    /**
     * Загружает произведение из сети и кеширует содержимое в файл;
     * при ошибке сети отдаёт кешированную версию для офлайн-чтения (если есть).
     */
    override suspend fun getWork(link: String): SamlibResult<Work> {
        val net =
            samlibCatching {
                val html = http.getPage(link)
                parser.parse(html, link)
            }
        return when (net) {
            is SamlibResult.Success -> {
                val work = net.data
                if (work.rawContent.isNotBlank()) {
                    saveContentToCache(work)
                }
                net
            }

            is SamlibResult.Error -> {
                val cached = runCatching { cachedWorkDao.get(link) }.getOrNull()
                val content = cached?.let { readCachedContent(it.filePath) }
                // Отдаём кеш только если файл на месте; иначе — исходная сетевая ошибка,
                // чтобы читалка показала экран ошибки, а не пустой текст.
                if (cached != null && content != null) {
                    SamlibResult.Success(cached.toDomain(content))
                } else {
                    net
                }
            }
        }
    }

    override suspend fun clearWorkCache() {
        // Удаляем все файлы и очищаем таблицу кеша
        cacheDir.listFiles()?.forEach { it.delete() }
        cachedWorkDao.clear()
    }

    /** Есть ли офлайн-кеш книги (файл существует). */
    override suspend fun hasCached(link: String): Boolean {
        val entity = cachedWorkDao.get(link) ?: return false
        return File(entity.filePath).exists()
    }

    /** Сырое содержимое книги из файлового кеша (для «Сохранить»/«Открыть в другой читалке»). */
    override suspend fun cachedRawContent(link: String): String? {
        val entity = cachedWorkDao.get(link) ?: return null
        return readCachedContent(entity.filePath)
    }

    override suspend fun addToHistory(work: Work) {
        val pinned = historyDao.isPinned(work.link) ?: false
        historyDao.upsert(work.toEntity(openedDate = System.currentTimeMillis(), pinned = pinned))
        // Кеш пишет getWork при успешной загрузке из сети — здесь не дублируем
        // (иначе многомегабайтный файл переписывался бы и при офлайн-открытии из кеша).
    }

    /**
     * Заносит локальную книгу (открытый файл) в ту же историю, что и книги samlib.
     * Ключ — URI файла (file://…/content://…), поэтому со ссылками samlib («/…») не конфликтует;
     * открытие такого элемента истории маршрутизируется в локальную читалку по схеме URI.
     */
    override suspend fun addLocalToHistory(
        uri: String,
        title: String?,
        author: String?,
    ) {
        val pinned = historyDao.isPinned(uri) ?: false
        historyDao.upsert(
            WorkEntity(
                link = uri,
                title = title,
                authorLink = null,
                updateDate = null,
                size = null,
                openedDate = System.currentTimeMillis(),
                authorShortName = author,
                genres = null,
                pinned = pinned,
            ),
        )
    }

    override fun observeHistory(): Flow<List<Work>> =
        historyDao.observeHistory().map { list -> list.map { it.toDomain() } }

    override suspend fun clearHistory() = historyDao.clearUnpinned()

    override suspend fun deleteHistory(link: String) = historyDao.delete(link)

    override suspend fun restoreHistory(work: Work) =
        historyDao.upsert(
            work.toEntity(
                openedDate = work.openedDate ?: System.currentTimeMillis(),
                pinned = work.pinned,
            ),
        )

    override suspend fun setHistoryPinned(
        link: String,
        pinned: Boolean,
    ) = historyDao.setPinned(link, pinned)

    override suspend fun isHistoryPinned(link: String): Boolean = historyDao.isPinned(link) ?: false

    override suspend fun saveReadingPosition(
        workLink: String,
        blockIndex: Int,
    ) {
        positionCache[workLink] = blockIndex
        // Timber.tag("Position").i("cache block=%d link=%s", blockIndex, workLink)
    }

    override suspend fun getReadingPosition(workLink: String): Int {
        positionCache[workLink]?.let {
            // Timber.tag("Position").i("read (cache) block=%d link=%s", it, workLink)
            return it
        }
        val pos = bookmarkDao.readingPosition(workLink) ?: 0
        // Timber.tag("Position").i("read (db) block=%d link=%s", pos, workLink)
        return pos
    }

    override suspend fun flushReadingPosition(workLink: String) {
        val blockIndex = positionCache[workLink] ?: return
        bookmarkDao.clearAuto(workLink)
        bookmarkDao.upsert(
            BookmarkEntity(
                workLink = workLink,
                indent = blockIndex,
                savedDate = System.currentTimeMillis(),
                userBookmark = false,
            ),
        )
        Timber.tag("Position").i("flush block=%d link=%s", blockIndex, workLink)
    }

    // --- Вспомогательные методы для файлового кеша ---

    /**
     * Сохраняет текст произведения в файл и записывает путь к файлу в БД.
     * Файловый I/O — на Dispatchers.IO: тело книги может быть в несколько МБ.
     */
    private suspend fun saveContentToCache(work: Work) {
        val file = getCacheFile(work.link)
        withContext(Dispatchers.IO) { file.writeText(work.rawContent) }
        cachedWorkDao.upsert(work.toCachedEntity(file.absolutePath))
    }

    /**
     * Возвращает файл для кеширования произведения по его ссылке.
     * Имя файла – хеш ссылки, чтобы избежать проблем с недопустимыми символами.
     */
    private fun getCacheFile(link: String): File {
        val fileName = link.hashCode().toString() + ".html"
        return File(cacheDir, fileName)
    }

    /**
     * Читает содержимое файла по указанному пути. Возвращает null, если файла нет
     * или чтение не удалось.
     */
    private suspend fun readCachedContent(filePath: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(filePath)
                if (file.exists()) file.readText() else null
            }.getOrNull()
        }
}
