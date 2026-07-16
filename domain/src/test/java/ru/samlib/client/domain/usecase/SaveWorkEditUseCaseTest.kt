package ru.samlib.client.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.domain.repository.AboutForm
import ru.samlib.client.domain.repository.AuthorEditRepository
import ru.samlib.client.domain.repository.DirFields
import ru.samlib.client.domain.repository.DirForm
import ru.samlib.client.domain.repository.GroupEntry
import ru.samlib.client.domain.repository.GroupForm
import ru.samlib.client.domain.repository.ImageUpload
import ru.samlib.client.domain.repository.NewWorkForm
import ru.samlib.client.domain.repository.SamlibError
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.UploadFile
import ru.samlib.client.domain.repository.WorkEditFields
import ru.samlib.client.domain.repository.WorkEditForm

private class FakeSaveAuthorEditRepository(
    private val createResult: SamlibResult<Unit> = SamlibResult.Success(Unit),
    private val saveResult: SamlibResult<Unit> = SamlibResult.Success(Unit),
) : AuthorEditRepository {
    var lastFileName: String? = null
    var lastFile: UploadFile? = null

    override suspend fun loadAbout(authorLink: String): SamlibResult<AboutForm> = error("not used")

    override suspend fun loadGroups(authorLink: String): SamlibResult<GroupForm> = error("not used")

    override suspend fun saveGroups(
        form: GroupForm,
        groups: List<GroupEntry>,
    ): SamlibResult<Unit> = error("not used")

    override suspend fun uploadAvatar(
        authorLink: String,
        image: ImageUpload,
        slot: String,
    ): SamlibResult<Unit> = error("not used")

    override suspend fun loadDir(authorLink: String): SamlibResult<DirForm> = error("not used")

    override suspend fun saveDir(
        form: DirForm,
        fields: DirFields,
    ): SamlibResult<Unit> = error("not used")

    override suspend fun loadNewWork(authorLink: String): SamlibResult<NewWorkForm> = error("not used")

    override suspend fun createWork(
        form: NewWorkForm,
        fileName: String,
        fields: WorkEditFields,
        file: UploadFile?,
    ): SamlibResult<Unit> {
        lastFileName = fileName
        lastFile = file
        return createResult
    }

    override suspend fun loadWorkEdit(
        authorLink: String,
        file: String,
    ): SamlibResult<WorkEditForm> = error("not used")

    override suspend fun saveWorkEdit(
        form: WorkEditForm,
        fields: WorkEditFields,
    ): SamlibResult<Unit> = saveResult

    override suspend fun deleteWork(
        authorLink: String,
        file: String,
    ): SamlibResult<Unit> = error("not used")

    override suspend fun saveAbout(
        form: AboutForm,
        body: String,
    ): SamlibResult<Unit> = error("not used")
}

private fun saveNewWorkForm() =
    NewWorkForm(
        dir = "k/x",
        fields = emptyMap(),
        author = "A",
        date = "01.01.2026",
        types = emptyList(),
        janrs = emptyList(),
        groups = emptyList(),
    )

private fun saveWorkEditForm(text: String) =
    WorkEditForm(dir = "k/x", file = "f.shtml", meta = saveNewWorkForm(), text = text, textFields = emptyMap())

private fun saveFields(
    title: String = "Заголовок",
    text: String = "Т".repeat(60),
) = WorkEditFields(title = title, type = "", janr = "", group = "", annotation = "", text = text)

class SaveWorkEditUseCaseTest {
    @Test
    fun `новое произведение — пустой заголовок отклоняется валидацией`() {
        val useCase = SaveWorkEditUseCase(FakeSaveAuthorEditRepository())
        val params =
            SaveWorkEditUseCase.Params(
                target = SaveWorkEditUseCase.Target.New(saveNewWorkForm(), "file"),
                fields = saveFields(title = "  "),
                attachedBytes = null,
                attachedFileName = null,
                attachedMime = "text/plain",
            )
        assertEquals(WorkFormError.EmptyTitle, useCase.validate(params))
    }

    @Test
    fun `новое произведение — короткое тело отклоняется, длинное проходит`() {
        val useCase = SaveWorkEditUseCase(FakeSaveAuthorEditRepository())
        val short =
            SaveWorkEditUseCase.Params(
                target = SaveWorkEditUseCase.Target.New(saveNewWorkForm(), "file"),
                fields = saveFields(text = "коротко"),
                attachedBytes = null,
                attachedFileName = null,
                attachedMime = "text/plain",
            )
        assertEquals(WorkFormError.TextTooShort, useCase.validate(short))
        val long = short.copy(fields = saveFields(text = "Т".repeat(60)))
        assertNull(useCase.validate(long))
    }

    @Test
    fun `редактирование — неизменённый текст не проверяется на длину`() {
        val useCase = SaveWorkEditUseCase(FakeSaveAuthorEditRepository())
        val sameText = saveWorkEditForm("старый текст")
        val params =
            SaveWorkEditUseCase.Params(
                target = SaveWorkEditUseCase.Target.Existing(sameText),
                fields = saveFields(text = "старый текст"),
                attachedBytes = null,
                attachedFileName = null,
                attachedMime = "text/plain",
            )
        assertNull(useCase.validate(params))
    }

    @Test
    fun `новое произведение — успешное сохранение вызывает createWork с приложенным файлом`() =
        runTest {
            val repo = FakeSaveAuthorEditRepository()
            val useCase = SaveWorkEditUseCase(repo)
            val params =
                SaveWorkEditUseCase.Params(
                    target = SaveWorkEditUseCase.Target.New(saveNewWorkForm(), "myfile"),
                    fields = saveFields(),
                    attachedBytes = ByteArray(100),
                    attachedFileName = "upload.txt",
                    attachedMime = "text/plain",
                )
            val result = useCase(params)
            assertTrue(result is SaveWorkEditUseCase.Result.Saved)
            assertEquals("myfile", repo.lastFileName)
            assertEquals("upload.txt", repo.lastFile?.name)
        }

    @Test
    fun `редактирование — ошибка сети даёт Failed с сообщением`() =
        runTest {
            val repo = FakeSaveAuthorEditRepository(saveResult = SamlibResult.Error(SamlibError.NETWORK))
            val useCase = SaveWorkEditUseCase(repo)
            val params =
                SaveWorkEditUseCase.Params(
                    target = SaveWorkEditUseCase.Target.Existing(saveWorkEditForm("old")),
                    fields = saveFields(text = "new text"),
                    attachedBytes = null,
                    attachedFileName = null,
                    attachedMime = "text/plain",
                )
            val result = useCase(params) as SaveWorkEditUseCase.Result.Failed
            assertTrue(result.message.isNotBlank())
        }
}
