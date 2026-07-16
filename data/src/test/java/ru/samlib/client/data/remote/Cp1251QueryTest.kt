package ru.samlib.client.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class Cp1251QueryTest {
    @Test
    fun `кириллица кодируется в cp1251 percent-encoding`() {
        assertEquals("%CF%F3%F8%EA%E8%ED", cp1251Encode("Пушкин"))
    }
}
