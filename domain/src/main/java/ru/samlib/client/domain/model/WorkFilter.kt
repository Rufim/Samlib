package ru.samlib.client.domain.model

/** Клиентский фильтр списка произведений (как в легаси: текст + жанры + пол + режим «исключать»). */
data class WorkFilter(
    val query: String = "",
    val genres: Set<Genre> = emptySet(),
    val genders: Set<Gender> = emptySet(), // пусто = любой пол
    val exclude: Boolean = false,
) {
    val active: Boolean get() = query.isNotBlank() || genres.isNotEmpty() || genders.isNotEmpty()
}

/** Пол автора по фамилии (первое слово имени). */
fun authorGender(name: String?): Gender = Gender.parseLastName(name?.trim()?.substringBefore(' ')?.ifBlank { null })

/** Применяет фильтр к списку. Пустой фильтр возвращает исходный список. */
fun List<Work>.applyFilter(f: WorkFilter): List<Work> {
    if (!f.active) return this
    val q = f.query.trim().lowercase()
    return filter { w ->
        val matchQuery =
            q.isEmpty() ||
                w.title?.lowercase()?.contains(q) == true ||
                w.authorShortName?.lowercase()?.contains(q) == true ||
                w.customGenres?.lowercase()?.contains(q) == true
        val matchGenre =
            f.genres.isEmpty() ||
                run {
                    val g = w.customGenres?.lowercase().orEmpty()
                    f.genres.any { it.title.lowercase() in g } || w.genres.any { it in f.genres }
                }
        val matchGender = f.genders.isEmpty() || authorGender(w.authorShortName) in f.genders
        val matched = matchQuery && matchGenre && matchGender
        if (f.exclude) !matched else matched
    }
}
