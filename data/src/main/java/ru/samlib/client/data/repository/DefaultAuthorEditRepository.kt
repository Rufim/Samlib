package ru.samlib.client.data.repository

import ru.samlib.client.data.parser.AuthorEditParser
import ru.samlib.client.data.parser.ParseException
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.remote.cp1251Encode
import ru.samlib.client.data.remote.samlibCatching
import ru.samlib.client.domain.repository.AboutForm
import ru.samlib.client.domain.repository.AuthRepository
import ru.samlib.client.domain.repository.AuthorEditRepository
import ru.samlib.client.domain.repository.DirFields
import ru.samlib.client.domain.repository.DirForm
import ru.samlib.client.domain.repository.GroupEntry
import ru.samlib.client.domain.repository.GroupForm
import ru.samlib.client.domain.repository.ImageUpload
import ru.samlib.client.domain.repository.NewWorkForm
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.UploadFile
import ru.samlib.client.domain.repository.WorkEditFields
import ru.samlib.client.domain.repository.WorkEditForm

class DefaultAuthorEditRepository(
    private val http: SamlibHttpClient,
    private val parser: AuthorEditParser,
    private val auth: AuthRepository,
) : AuthorEditRepository {
    /** Каталог автора из ссылки страницы: «/k/kazancew_d/» → «k/kazancew_d». */
    private fun dirOf(authorLink: String) = authorLink.trim().trim('/')

    /** Загрузить форму «о себе» (текущий текст + скрытые поля для повторной отправки). */
    override suspend fun loadAbout(authorLink: String): SamlibResult<AboutForm> =
        samlibCatching {
            val dir = dirOf(authorLink)
            val query = "OPERATION=${cp1251Encode("(Text edit)")}&DIR=$dir&STORE_FILE=about"
            val fields = parser.parseForm(http.getEditPage(query, auth.currentCookie()))
            AboutForm(dir = dir, fields = fields, body = fields["BODY"].orEmpty())
        }

    /** Загрузить форму групп (разделов): непустые слоты TITLn/ANNOTn. */
    override suspend fun loadGroups(authorLink: String): SamlibResult<GroupForm> =
        samlibCatching {
            val dir = dirOf(authorLink)
            val fields = parser.parseForm(http.getGroupEdit(dir, auth.currentCookie()))
            val max = fields["NUM"]?.toIntOrNull() ?: 0
            val groups =
                (1..max)
                    .map { i -> GroupEntry(fields["TITL$i"].orEmpty(), fields["ANNOT$i"].orEmpty()) }
                    .filter { it.title.isNotBlank() || it.annotation.isNotBlank() }
            GroupForm(fields = fields, max = max, groups = groups)
        }

    /** Сохранить группы: раскладываем список по слотам TITL1..NUM, остальные — пустые. */
    override suspend fun saveGroups(
        form: GroupForm,
        groups: List<GroupEntry>,
    ): SamlibResult<Unit> =
        samlibCatching {
            val params = LinkedHashMap(form.fields)
            for (i in 1..form.max) {
                val g = groups.getOrNull(i - 1)
                params["TITL$i"] = g?.title?.trim().orEmpty()
                params["ANNOT$i"] = g?.annotation?.trim().orEmpty()
            }
            params["OPERATION"] = "replace"
            params["Store"] = "Store (Сохранить)"
            val error = http.postGroupEdit(params, auth.currentCookie())
            if (error != null) throw ParseException(error)
        }

    /** Загрузить аватар (фото автора). По умолчанию слот .photo2 — «Информация об авторе». */
    override suspend fun uploadAvatar(
        authorLink: String,
        image: ImageUpload,
        slot: String,
    ): SamlibResult<Unit> =
        samlibCatching {
            val error = http.uploadPhoto(dirOf(authorLink), slot, image, auth.currentCookie())
            if (error != null) throw ParseException(error)
        }

    /** Загрузить форму «Сведения о разделе» (edit_dir). */
    override suspend fun loadDir(authorLink: String): SamlibResult<DirForm> =
        samlibCatching {
            val dir = dirOf(authorLink)
            val query = "OPERATION=edit_dir&DIR=$dir"
            val fields = parser.parseForm(http.getEditPage(query, auth.currentCookie()))
            DirForm(
                dir = dir,
                fields = fields,
                author = fields["AUTHOR"].orEmpty(),
                title = fields["TITLE"].orEmpty(),
                email = fields["EMAIL"].orEmpty(),
                homeLink = fields["HOME_LINK"].orEmpty(),
                annotation = fields["ANNOT"].orEmpty(),
            )
        }

    /** Сохранить «Сведения о разделе», сохранив прочие поля формы без изменений. */
    override suspend fun saveDir(
        form: DirForm,
        fields: DirFields,
    ): SamlibResult<Unit> =
        samlibCatching {
            val params = LinkedHashMap(form.fields)
            params["DIR"] = form.dir
            params["AUTHOR"] = fields.author
            params["TITLE"] = fields.title
            params["EMAIL"] = fields.email
            params["HOME_LINK"] = fields.homeLink
            params["ANNOT"] = fields.annotation
            params["OPERATION"] = "Сохранить! (Store dir)"
            val error = http.postZhurnal(params, auth.currentCookie())
            if (error != null) throw ParseException(error)
        }

    /** Загрузить форму нового произведения (new_book): дефолты и списки выбора. */
    override suspend fun loadNewWork(authorLink: String): SamlibResult<NewWorkForm> =
        samlibCatching {
            val dir = dirOf(authorLink)
            val html = http.getEditPage("OPERATION=new_book&DIR=$dir", auth.currentCookie())
            val fields = parser.parseForm(html)
            if (fields["DIR"].isNullOrBlank()) throw ParseException("Форма произведения не открылась (нет сессии?)")
            val selects = parser.parseSelectOptions(html)
            NewWorkForm(
                dir = dir,
                fields = fields,
                author = fields["AUTHOR"].orEmpty(),
                date = fields["DATE"].orEmpty(),
                types = selects["TYPE"].orEmpty(),
                janrs = selects["JANR"].orEmpty(),
                groups = selects["ADD_TYPE"].orEmpty(),
            )
        }

    /**
     * Создать произведение: multipart-отправка формы new_book в порядке полей формы.
     * [fileName] — латинское имя файла (без .shtml), [type]/[janr]/[group] — значения селектов.
     */
    override suspend fun createWork(
        form: NewWorkForm,
        fileName: String,
        fields: WorkEditFields,
        file: UploadFile?,
    ): SamlibResult<Unit> =
        samlibCatching {
            val f = form.fields
            val params =
                listOf(
                    "DIR" to form.dir,
                    "REPLACE" to f["REPLACE"].orEmpty(),
                    "OWNER" to f["OWNER"].orEmpty(),
                    "TMP_FILE" to f["TMP_FILE"].orEmpty(),
                    "REQUIRED" to f["REQUIRED"].orEmpty(),
                    "STORE_FILE" to fileName,
                    "TITLE" to fields.title,
                    "AUTHOR" to form.author,
                    "TYPE" to fields.type,
                    "TRANSLATOR" to f["TRANSLATOR"].orEmpty(),
                    "JANR" to fields.janr,
                    "READ_LEVEL" to (f["READ_LEVEL"] ?: "2"),
                    "WRITE_LEVEL" to (f["WRITE_LEVEL"] ?: "4"),
                    "ADD_TYPE" to fields.group,
                    "DATE" to form.date,
                    "NO_COMMENT" to (f["NO_COMMENT"] ?: "0"),
                    "ANNOT" to fields.annotation,
                    // единственная кодировка, которую CGI принимает без порчи текста
                    "CODESET" to "auto",
                )
            val error = http.postWorkForm(params, fields.text, auth.currentCookie(), file)
            if (error != null) throw ParseException(error)
        }

    /** Загрузить форму редактирования произведения: метаданные (edit_book) и текст «(Text edit)». */
    override suspend fun loadWorkEdit(
        authorLink: String,
        file: String,
    ): SamlibResult<WorkEditForm> =
        samlibCatching {
            val dir = dirOf(authorLink)
            val cookie = auth.currentCookie()
            val metaHtml = http.getEditPage("OPERATION=edit_book&FILE=$file&DIR=$dir", cookie)
            val fields = parser.parseForm(metaHtml)
            if (fields["STORE_FILE"].isNullOrBlank()) {
                throw ParseException(
                    "Форма произведения не открылась (нет сессии?)",
                )
            }
            val selects = parser.parseSelectOptions(metaHtml)
            val textQuery = "OPERATION=${cp1251Encode("(Text edit)")}&DIR=$dir&STORE_FILE=$file"
            val textFields = parser.parseForm(http.getEditPage(textQuery, cookie))
            WorkEditForm(
                dir = dir,
                file = file,
                meta =
                    NewWorkForm(
                        dir = dir,
                        fields = fields,
                        author = fields["AUTHOR"].orEmpty(),
                        date = fields["DATE"].orEmpty(),
                        types = selects["TYPE"].orEmpty(),
                        janrs = selects["JANR"].orEmpty(),
                        groups = selects["ADD_TYPE"].orEmpty(),
                    ),
                text = textFields["BODY"].orEmpty(),
                textFields = textFields,
            )
        }

    /**
     * Сохранить произведение: метаданные — multipart Store (REPLACE=Yes, TEXT сервер
     * игнорирует), затем текст — «Store body», только если он изменился.
     */
    override suspend fun saveWorkEdit(
        form: WorkEditForm,
        fields: WorkEditFields,
    ): SamlibResult<Unit> =
        samlibCatching {
            // порядок полей — как в форме (LinkedHashMap парсера); TEXT уходит отдельной частью
            val params = LinkedHashMap(form.meta.fields)
            params.remove("TEXT")
            params["TITLE"] = fields.title
            params["TYPE"] = fields.type
            params["JANR"] = fields.janr
            params["ADD_TYPE"] = fields.group
            params["ANNOT"] = fields.annotation
            params["CODESET"] = "auto"
            val cookie = auth.currentCookie()
            val error = http.postWorkForm(params.toList(), "", cookie)
            if (error != null) throw ParseException(error)
            if (fields.text != form.text) {
                val body = LinkedHashMap(form.textFields)
                body["DIR"] = form.dir
                body["STORE_FILE"] = form.file
                body["BODY"] = fields.text
                body["OPERATION"] = "Сохранить! (Store body)"
                val textError = http.postZhurnal(body, cookie)
                if (textError != null) throw ParseException(textError)
            }
        }

    /** Удалить произведение: запрос удаления → подтверждение с delhash → delete_object. */
    override suspend fun deleteWork(
        authorLink: String,
        file: String,
    ): SamlibResult<Unit> =
        samlibCatching {
            val dir = dirOf(authorLink)
            val cookie = auth.currentCookie()
            val confirmHtml =
                http.postZhurnalHtml(
                    mapOf("DIR" to dir, "STORE_FILE" to file, "OPERATION" to "Delete"),
                    cookie,
                )
            val confirm = parser.parseForm(confirmHtml)
            val delhash =
                confirm["delhash"]
                    ?: throw ParseException("Сервер не подтвердил удаление")
            val error =
                http.postZhurnal(
                    mapOf(
                        "OPERATION" to "delete_object",
                        "DIR" to dir,
                        "FILE" to file,
                        "delhash" to delhash,
                        // параметр кнопки обязателен — без него CGI молча не удаляет
                        "del" to "Удалить! (Delete it!)",
                    ),
                    cookie,
                )
            if (error != null) throw ParseException(error)
        }

    /** Сохранить текст «о себе», вернув остальные поля формы без изменений. */
    override suspend fun saveAbout(
        form: AboutForm,
        body: String,
    ): SamlibResult<Unit> =
        samlibCatching {
            val params = LinkedHashMap(form.fields)
            params["DIR"] = form.dir
            params["STORE_FILE"] = "about"
            params["BODY"] = body
            params["OPERATION"] = "Сохранить! (Store body)"
            val error = http.postZhurnal(params, auth.currentCookie())
            if (error != null) throw ParseException(error)
        }
}
