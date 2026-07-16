package ru.samlib.client.data.parser

import ru.samlib.client.domain.model.RatingSection

/** Построение относительного URL страницы раздела для нужной стратегии пагинации. */
object RatingPager {
    /** Общий маркер количества страниц: `Страниц (N)`. */
    private val PAGE_COUNT = Regex("""Страниц\s*\((\d+)\)""")

    fun pageUrl(
        section: RatingSection,
        page: Int,
    ): String =
        when (section.paging) {
            // page 0 → каталог как есть; page N → index-(N+1).shtml
            RatingSection.Paging.INDEX ->
                if (page == 0) section.path else section.path.removeSuffix("/") + "/index-${page + 1}.shtml"
            // жанровые страницы: суффикс -(N+1).shtml на всех страницах
            RatingSection.Paging.GENRE -> section.path + "-${page + 1}.shtml"
        }

    /** Число страниц из маркера `Страниц (N)`; null если маркера нет (одностраничный список). */
    fun parsePageCount(html: String): Int? =
        PAGE_COUNT
            .find(html)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
}
