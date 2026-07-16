package ru.samlib.client.data.mapper

import ru.samlib.client.data.db.AuthorEntity
import ru.samlib.client.data.db.CachedWorkEntity
import ru.samlib.client.data.db.ObservedWorkEntity
import ru.samlib.client.data.db.WorkEntity
import ru.samlib.client.domain.model.Author
import ru.samlib.client.domain.model.ObservedWork
import ru.samlib.client.domain.model.Type
import ru.samlib.client.domain.model.Work
import java.util.Date

fun Author.toEntity(
    isObservable: Boolean,
    hasUpdates: Boolean = false,
    notNotified: Boolean = false,
): AuthorEntity =
    AuthorEntity(
        link = link,
        fullName = fullName,
        shortName = shortName,
        lastUpdateDate = lastUpdateDate?.time,
        lastCheckedDate = lastCheckedDate,
        workCount = workCount,
        isObservable = isObservable,
        hasUpdates = hasUpdates,
        notNotified = notNotified,
    )

fun AuthorEntity.toDomain(): Author =
    Author(
        link = link,
        fullName = fullName,
        shortName = shortName,
        lastUpdateDate = lastUpdateDate?.let { Date(it) },
        lastCheckedDate = lastCheckedDate,
        workCount = workCount,
        hasUpdates = hasUpdates,
    )

fun Work.toEntity(
    openedDate: Long = 0,
    pinned: Boolean = false,
): WorkEntity =
    WorkEntity(
        link = link,
        title = title,
        authorLink = authorLink,
        updateDate = updateDate?.time,
        size = size,
        openedDate = openedDate,
        authorShortName = authorShortName,
        genres = customGenres,
        pinned = pinned,
    )

fun ObservedWorkEntity.toDomain(): ObservedWork =
    ObservedWork(
        link = link,
        authorLink = authorLink,
        authorName = authorName,
        title = title,
        categoryTitle = categoryTitle,
        size = size,
        updateDate = updateDate,
        changed = changed,
        sizeDiff = sizeDiff,
        changedDate = changedDate,
        isStandalone = isStandalone,
        excluded = excluded,
    )

fun ObservedWork.toEntity(): ObservedWorkEntity =
    ObservedWorkEntity(
        link = link,
        authorLink = authorLink,
        authorName = authorName,
        title = title,
        categoryTitle = categoryTitle,
        size = size,
        updateDate = updateDate,
        changed = changed,
        sizeDiff = sizeDiff,
        changedDate = changedDate,
        isStandalone = isStandalone,
        excluded = excluded,
    )

/** Снимок произведения для файлового кеша офлайн-чтения. */
fun Work.toCachedEntity(rawFilePath: String): CachedWorkEntity =
    CachedWorkEntity(
        link = link,
        title = title,
        authorLink = authorLink,
        authorShortName = authorShortName,
        categoryTitle = categoryTitle,
        imageLink = imageLink,
        size = size,
        sizeDiff = sizeDiff,
        rate = rate,
        votes = votes,
        expertRate = expertRate,
        expertVotes = expertVotes,
        commentCount = commentCount,
        customGenres = customGenres,
        type = type.name,
        annotation = annotation,
        description = description,
        createDate = createDate,
        updateDate = updateDate,
        dateText = dateText,
        changedDate = changedDate,
        genres = genres, // список автоматически преобразуется TypeConverter
        annotationBlocks = annotationBlocks,
        filePath = rawFilePath,
        savedDate = System.currentTimeMillis(),
    )

/** Восстановление произведения из кеша; [content] — тело книги из файла. */
fun CachedWorkEntity.toDomain(content: String): Work =
    Work(
        link = link,
        title = title,
        authorLink = authorLink,
        authorShortName = authorShortName,
        categoryTitle = categoryTitle,
        imageLink = imageLink,
        size = size,
        sizeDiff = sizeDiff,
        rate = rate,
        votes = votes,
        expertRate = expertRate,
        expertVotes = expertVotes,
        commentCount = commentCount,
        genres = genres, // TypeConverter восстановит List<Genre>
        customGenres = customGenres,
        // безопасный разбор: неизвестное/битое значение не должно ронять чтение из кеша
        type = Type.entries.firstOrNull { it.name == type } ?: Type.OTHER,
        annotation = annotation,
        annotationBlocks = annotationBlocks,
        description = description,
        createDate = createDate,
        updateDate = updateDate,
        dateText = dateText,
        changedDate = changedDate,
        rawContent = content,
    )

fun WorkEntity.toDomain(): Work =
    Work(
        link = link,
        title = title,
        authorLink = authorLink,
        updateDate = updateDate?.let { Date(it) },
        size = size,
        authorShortName = authorShortName,
        customGenres = genres,
        pinned = pinned,
        openedDate = openedDate,
    )
