package ru.samlib.client.domain.samlib

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SamlibLinkFormatTest {
    @Test
    fun `normalizeLink обрезает домен и схлопывает слэши`() {
        assertEquals(
            "/m/melxnik_a_a/",
            SamlibLinkFormat.normalizeLink("http://samlib.ru/m/melxnik_a_a/"),
        )
        assertEquals("/a/b.shtml", SamlibLinkFormat.normalizeLink("/a//b.shtml"))
        assertNull(SamlibLinkFormat.normalizeLink(""))
    }
}
