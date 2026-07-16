package ru.samlib.client.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class ParserUtilsTest {
    @Test
    fun `parseSizeKb извлекает число до k`() {
        assertEquals(12, ParserUtils.parseSizeKb("12k"))
        assertEquals(7, ParserUtils.parseSizeKb("7"))
        assertNull(ParserUtils.parseSizeKb("abc"))
        assertNull(ParserUtils.parseSizeKb(null))
    }

    @Test
    fun `parseDate разбирает dd_MM_yyyy`() {
        val d = ParserUtils.parseDate("05/11/2020", "dd/MM/yyyy")!!
        val c = Calendar.getInstance().apply { time = d }
        assertEquals(2020, c.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, c.get(Calendar.MONTH))
        assertEquals(5, c.get(Calendar.DAY_OF_MONTH))
        assertNull(ParserUtils.parseDate("не дата", "dd/MM/yyyy"))
    }
}
