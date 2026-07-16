package ru.samlib.client.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samlib.client.data.parser.DefaultWorkContentParser
import ru.samlib.client.data.parser.DefaultWorkDraftChunker
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

private class FakeAuthorEditRepository(
    private val newWork: SamlibResult<NewWorkForm>? = null,
    private val workEdit: SamlibResult<WorkEditForm>? = null,
) : AuthorEditRepository {
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

    override suspend fun loadNewWork(authorLink: String): SamlibResult<NewWorkForm> = newWork!!

    override suspend fun createWork(
        form: NewWorkForm,
        fileName: String,
        fields: WorkEditFields,
        file: UploadFile?,
    ): SamlibResult<Unit> = error("not used")

    override suspend fun loadWorkEdit(
        authorLink: String,
        file: String,
    ): SamlibResult<WorkEditForm> = workEdit!!

    override suspend fun saveWorkEdit(
        form: WorkEditForm,
        fields: WorkEditFields,
    ): SamlibResult<Unit> = error("not used")

    override suspend fun deleteWork(
        authorLink: String,
        file: String,
    ): SamlibResult<Unit> = error("not used")

    override suspend fun saveAbout(
        form: AboutForm,
        body: String,
    ): SamlibResult<Unit> = error("not used")
}

private fun newWorkForm() =
    NewWorkForm(
        dir = "k/x",
        fields = emptyMap(),
        author = "A",
        date = "01.01.2026",
        types = emptyList(),
        janrs = emptyList(),
        groups = emptyList(),
    )

private fun workEditForm(text: String) =
    WorkEditForm(dir = "k/x", file = "f.shtml", meta = newWorkForm(), text = text, textFields = emptyMap())

class LoadWorkEditUseCaseTest {
    private val chunker = DefaultWorkDraftChunker(DefaultWorkContentParser())

    @Test
    fun `workFile null грузит форму нового произведения`() =
        runTest {
            val useCase =
                LoadWorkEditUseCase(FakeAuthorEditRepository(newWork = SamlibResult.Success(newWorkForm())), chunker)
            val result = useCase("k/x", null)
            assertTrue(result is LoadWorkEditUseCase.Result.New)
        }

    @Test
    fun `workFile задан — грузит и чанкует форму редактирования`() =
        runTest {
            val useCase =
                LoadWorkEditUseCase(
                    FakeAuthorEditRepository(workEdit = SamlibResult.Success(workEditForm("Привет\n\nМир"))),
                    chunker,
                )
            val result = useCase("k/x", "f.shtml") as LoadWorkEditUseCase.Result.Existing
            assertEquals("Привет\n\nМир", result.form.text)
            assertTrue(result.draftBlocks.isNotEmpty())
        }

    @Test
    fun `ошибка сети — Failed с текстом ошибки`() =
        runTest {
            val useCase =
                LoadWorkEditUseCase(
                    FakeAuthorEditRepository(newWork = SamlibResult.Error(SamlibError.NETWORK)),
                    chunker,
                )
            val result = useCase("k/x", null)
            assertTrue(result is LoadWorkEditUseCase.Result.Failed)
        }
}
