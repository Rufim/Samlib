package ru.samlib.client.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.samlib.client.domain.model.RatingSection

class RatingPagerTest {
    @Test
    fun `INDEX стратегия строит URL страниц`() {
        val s = RatingSection.TOP_AUTHORS
        assertEquals("/rating/hits/", RatingPager.pageUrl(s, 0))
        assertEquals("/rating/hits/index-2.shtml", RatingPager.pageUrl(s, 1))
        assertEquals("/rating/hits/index-3.shtml", RatingPager.pageUrl(s, 2))
    }

    @Test
    fun `GENRE стратегия строит URL страниц`() {
        val s = RatingSection.REVIEWS
        assertEquals("/janr/index_janr_23-1.shtml", RatingPager.pageUrl(s, 0))
        assertEquals("/janr/index_janr_23-2.shtml", RatingPager.pageUrl(s, 1))
    }

    @Test
    fun `parsePageCount читает маркер Страниц`() {
        assertEquals(1061, RatingPager.parsePageCount("... <b>Страниц (1061):</b> ..."))
        assertEquals(64, RatingPager.parsePageCount("Страниц(64)"))
    }

    @Test
    fun `parsePageCount возвращает null без маркера`() {
        assertNull(RatingPager.parsePageCount("<html>нет маркера</html>"))
    }
}
