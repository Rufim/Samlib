package ru.samlib.client.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkFormValidatorTest {
    @Test
    fun `пустой заголовок — ошибка`() {
        assertEquals(WorkFormError.EmptyTitle, WorkFormValidator.validate("file", "  ", 100))
    }

    @Test
    fun `имя файла — только латиница, цифры, дефис и подчёркивание`() {
        assertEquals(WorkFormError.BadFileName, WorkFormValidator.validate("Плохое имя", "Т", 100))
        assertEquals(WorkFormError.BadFileName, WorkFormValidator.validate("has space", "Т", 100))
        assertNull(WorkFormValidator.validate("my_book-2", "Т", 100))
    }

    @Test
    fun `null-имя пропускает проверку имени (режим редактирования)`() {
        assertNull(WorkFormValidator.validate(null, "Т", 100))
    }

    @Test
    fun `тело короче 60 байт cp1251 отклоняется`() {
        assertEquals(WorkFormError.TextTooShort, WorkFormValidator.validate("f", "Т", 59))
        assertNull(WorkFormValidator.validate("f", "Т", 60))
    }
}
