package ru.samlib.client.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.samlib.client.domain.repository.AccountInfo
import ru.samlib.client.domain.repository.CommentDraft
import ru.samlib.client.domain.repository.ImageUpload
import ru.samlib.client.domain.repository.UploadFile
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import ru.samlib.client.domain.samlib.SamlibUrls
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class SamlibHttpClient private constructor(
    private val engineFor: (proxyUrl: String?) -> HttpClientEngine,
    initialBaseUrl: String,
) {
    /** Продакшн: OkHttp; домен и прокси меняются в рантайме через [reconfigure]. */
    constructor() : this(
        engineFor = { proxy ->
            OkHttp.create {
                // Таймауты обязательны: «подвешенная» сеть (Wi-Fi без интернета) без них
                // держит запрос почти бесконечно — читалка не падает в офлайн-кеш.
                config {
                    connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
                    readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
                    writeTimeout(WRITE_TIMEOUT_S, TimeUnit.SECONDS)
                }
                if (!proxy.isNullOrBlank()) {
                    val u =
                        if (proxy.startsWith("http", ignoreCase = true)) proxy else "http://$proxy"
                    this.proxy = ProxyBuilder.http(Url(u))
                }
            }
        },
        initialBaseUrl = SamlibUrls.BASE,
    )

    /** Тесты: фиксированный engine (прокси/домен не меняются). */
    constructor(engine: HttpClientEngine, baseUrl: String = SamlibUrls.BASE) :
        this(engineFor = { engine }, initialBaseUrl = baseUrl)

    @Volatile
    private var baseUrl: String = initialBaseUrl

    @Volatile
    private var proxyUrl: String? = null

    // Клиент без авто-редиректа: нужен, чтобы поймать Set-Cookie у ответов 302
    // (токен комментария и логин отдают cookie именно в 302, до COOK_CHECK).
    @Volatile
    private var client = HttpClient(engineFor(null))

    @Volatile
    private var noRedirectClient = HttpClient(engineFor(null)) { followRedirects = false }

    private val cp1251 = Charset.forName("windows-1251")

    /** Смена домена samlib и HTTP-прокси в рантайме (из настроек). */
    fun reconfigure(
        domain: String?,
        proxy: String?,
    ) {
        domain?.trim()?.ifBlank { null }?.let { baseUrl = normalizeDomain(it) }
        val newProxy = proxy?.trim()?.ifBlank { null }
        if (newProxy != proxyUrl) {
            proxyUrl = newProxy
            client = HttpClient(engineFor(newProxy))
            noRedirectClient = HttpClient(engineFor(newProxy)) { followRedirects = false }
        }
    }

    private fun normalizeDomain(d: String): String {
        val s = d.trim().removeSuffix("/")
        return if (s.startsWith("http", ignoreCase = true)) s else "http://$s"
    }

    /** Быстрая проверка доступности хоста samlib (для офлайн-режима). */
    suspend fun isHostReachable(): Boolean =
        withTimeoutOrNull(REACHABLE_TIMEOUT_MS) {
            runCatching {
                val resp = client.get(baseUrl) { header("User-Agent", USER_AGENT) }
                resp.status.value < 500
            }.getOrDefault(false)
        } ?: false

    suspend fun getPage(
        path: String,
        cookie: String = "",
    ): String {
        val response =
            client.get(baseUrl + path) {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                if (cookie.isNotBlank()) header("Cookie", cookie)
            }
        return response.bodyCp1251()
    }

    suspend fun getEncoded(
        path: String,
        encodedQuery: String,
    ): String {
        val response =
            client.get("$baseUrl$path?$encodedQuery") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
            }
        return response.bodyCp1251()
    }

    /**
     * Вход на samlib: POST cp1251-формы на /cgi-bin/login. Возвращает объединённый
     * cookie сессии из Set-Cookie или null при неудаче. (Как в легаси SignInParser.)
     */
    suspend fun login(
        login: String,
        password: String,
    ): String? {
        // Параметр кнопки «Вход!=Войти!» обязателен — без него CGI отвечает 404.
        val body =
            formBody(
                linkedMapOf(
                    "OPERATION" to "login",
                    "BACK" to "",
                    "DATA0" to login,
                    "DATA1" to password,
                    "Вход!" to "Войти!",
                ),
            )
        val resp =
            noRedirectClient.post("$baseUrl/cgi-bin/login") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                header("Referer", "$baseUrl/cgi-bin/login")
                header("Content-Type", "application/x-www-form-urlencoded")
                setBody(body)
            }
        val cookie = collectCookies(resp)
        // Успех: сервер выставил сессионные cookie (NAME/PASSWORD). При неверных данных их нет.
        return if (cookie.contains("PASSWORD=") || cookie.contains("NAME=")) cookie else null
    }

    /**
     * Отправка комментария (как в легаси CommentsParser): сперва GET за cookie COMMENT,
     * затем POST cp1251-формы на /cgi-bin/comment. Возвращает true при 2xx/3xx.
     */
    suspend fun postComment(
        workLinkNoSuffix: String,
        draft: CommentDraft,
        operation: String,
        loginCookie: String,
    ): String? {
        val token =
            noRedirectClient.get("$baseUrl/cgi-bin/comment?COMMENT=$workLinkNoSuffix") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                if (loginCookie.isNotBlank()) header("Cookie", loginCookie)
            }
        val commentCookie = collectCookies(token)
        // Свежий COMMENT-cookie должен победить пустой из логина → дедуп по имени, последнее значение.
        val cookie = mergeCookies(loginCookie, commentCookie)
        // Имя: отображаемое имя автора из cookie ZUI (до первого «&»), иначе логин из NAME.
        val displayName = cookieValue(loginCookie, "ZUI").substringBefore("&").trim()
        val effectiveName =
            draft.name.ifBlank { displayName.ifBlank { cookieValue(loginCookie, "NAME") } }
        val body =
            formBody(
                linkedMapOf(
                    "FILE" to workLinkNoSuffix,
                    "TEXT" to draft.text,
                    "NAME" to effectiveName,
                    "EMAIL" to draft.email,
                    "URL" to draft.url,
                    "MSGID" to draft.msgid.orEmpty(),
                    "OPERATION" to operation,
                    "add" to "Добавить!",
                ),
            )
        val resp =
            client.post("$baseUrl/cgi-bin/comment") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                header("Referer", "$baseUrl/cgi-bin/comment") // без Referer CGI отвечает 404
                header("Content-Type", "application/x-www-form-urlencoded")
                if (cookie.isNotBlank()) header("Cookie", cookie)
                setBody(body)
            }
        // Успех — редирект (302) на страницу комментариев. 200 = служебная страница
        // с сообщением сервера (например «слишком много сообщений подряд»).
        if (resp.status.value in 300..399) return null
        val plain = runCatching { resp.bodyCp1251() }.getOrDefault("").htmlToPlain()
        if (!plain.contains(
                "ДОБАВЛЕНИЕ КОММЕНТАРИЯ",
                ignoreCase = true,
            )
        ) {
            return null // редирект уже отработал
        }
        val message =
            plain
                .replace(Regex("(?i)\\*+\\s*ДОБАВЛЕНИЕ КОММЕНТАРИЯ\\s*\\*+"), "")
                .replace("ДОБАВЛЕНИЕ КОММЕНТАРИЯ", "")
                .replace("Написать программисту!", "")
                .trim()
        return message.ifBlank { "Не удалось отправить комментарий" }
    }

    /** GET страницы редактора раздела (/cgi-bin/zhurnal) под сессией. [query] уже собран. */
    suspend fun getEditPage(
        query: String,
        cookie: String,
    ): String {
        val response =
            client.get("$baseUrl/cgi-bin/zhurnal?$query") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                if (cookie.isNotBlank()) header("Cookie", cookie)
            }
        return response.bodyCp1251()
    }

    /**
     * POST формы редактора раздела (/cgi-bin/zhurnal), поля cp1251. Успех — редирект (302);
     * иначе возвращает текст сообщения/ошибки со страницы, либо null.
     */
    suspend fun postZhurnal(
        params: Map<String, String>,
        cookie: String,
    ): String? {
        val resp =
            client.post("$baseUrl/cgi-bin/zhurnal") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                header("Referer", "$baseUrl/cgi-bin/zhurnal")
                header("Content-Type", "application/x-www-form-urlencoded")
                if (cookie.isNotBlank()) header("Cookie", cookie)
                setBody(formBody(params))
            }
        return errorFromResponse(resp)
    }

    /** POST cp1251-формы на /cgi-bin/zhurnal с возвратом HTML ответа (шаг подтверждения удаления). */
    suspend fun postZhurnalHtml(
        params: Map<String, String>,
        cookie: String,
    ): String {
        val resp =
            client.post("$baseUrl/cgi-bin/zhurnal") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                header("Referer", "$baseUrl/cgi-bin/zhurnal")
                header("Content-Type", "application/x-www-form-urlencoded")
                if (cookie.isNotBlank()) header("Cookie", cookie)
                setBody(formBody(params))
            }
        return resp.bodyCp1251()
    }

    /**
     * Отправка формы произведения (new_book) на /cgi-bin/zhurnal. Форма объявлена как
     * multipart/form-data — url-encoded сервер для неё не принимает, поэтому собираем
     * multipart вручную «как браузер»: части в порядке полей формы, значения в cp1251,
     * пустая файловая часть FILE перед TEXT, нажатая кнопка OPERATION=Store последней.
     * Требование сервера: CODESET=auto (явные кодировки роняют отправку).
     * Успех — редирект (302 на /editors); иначе сообщение сервера.
     */
    suspend fun postWorkForm(
        fields: List<Pair<String, String>>,
        text: String,
        cookie: String,
        file: UploadFile? = null,
    ): String? {
        val boundary = "---------------------------SamlibClientFormBoundary"
        val out = ByteArrayOutputStream()

        fun part(
            name: String,
            value: String,
        ) {
            out.write(
                "--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray(
                    Charsets.ISO_8859_1,
                ),
            )
            out.write(value.toByteArray(cp1251))
            out.write("\r\n".toByteArray(Charsets.ISO_8859_1))
        }
        fields.forEach { (name, value) -> part(name, value) }
        // часть FILE присутствует всегда, как в браузере: пустая либо с выбранным файлом
        out.write(
            (
                "--$boundary\r\nContent-Disposition: form-data; " +
                    "name=\"FILE\"; filename=\"${file?.name.orEmpty()}\"\r\n" +
                    "Content-Type: ${file?.mime ?: "application/octet-stream"}\r\n\r\n"
            ).toByteArray(
                Charsets.ISO_8859_1,
            ),
        )
        if (file != null) out.write(file.bytes)
        out.write("\r\n".toByteArray(Charsets.ISO_8859_1))
        part("TEXT", text)
        part("OPERATION", "Store")
        out.write("--$boundary--\r\n".toByteArray(Charsets.ISO_8859_1))

        val resp =
            client.post("$baseUrl/cgi-bin/zhurnal") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                header("Referer", "$baseUrl/cgi-bin/zhurnal")
                header("Content-Type", "multipart/form-data; boundary=$boundary")
                if (cookie.isNotBlank()) header("Cookie", cookie)
                setBody(out.toByteArray())
            }
        if (resp.status.value in 300..399) return null
        val plain = runCatching { resp.bodyCp1251() }.getOrDefault("").htmlToPlain()
        // 200 для этой формы — всегда отказ: пробуем вытащить осмысленное сообщение.
        val known =
            Regex(
                "(?i)(ошибка[^.!]{0,150}|не передал[аи][^.!]{0,150}|" +
                    "не указан[аоы]?[^.!]{0,150}|уже существует[^.!]{0,100})",
            ).find(plain)
                ?.value
                ?.trim()
        return known ?: "Сервер не принял форму: ${plain.take(160)}"
    }

    /** Страница редактора групп (разделов) /cgi-bin/groupedit?DIR=... */
    suspend fun getGroupEdit(
        dir: String,
        cookie: String,
    ): String {
        val response =
            client.get("$baseUrl/cgi-bin/groupedit?DIR=$dir") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                if (cookie.isNotBlank()) header("Cookie", cookie)
            }
        return response.bodyCp1251()
    }

    /**
     * POST формы групп (/cgi-bin/groupedit), поля cp1251. Успех — редирект (302);
     * иначе возвращает текст сообщения/ошибки, либо null.
     */
    suspend fun postGroupEdit(
        params: Map<String, String>,
        cookie: String,
    ): String? {
        val resp =
            client.post("$baseUrl/cgi-bin/groupedit") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                header("Referer", "$baseUrl/cgi-bin/groupedit")
                header("Content-Type", "application/x-www-form-urlencoded")
                if (cookie.isNotBlank()) header("Cookie", cookie)
                setBody(formBody(params))
            }
        return errorFromResponse(resp)
    }

    /**
     * Загрузка фотографии раздела (аватар) через /cgi-bin/file_manager (multipart).
     * [fileName] — слот (.photo1 в оглавлении / .photo2 в информации об авторе).
     * Успех — редирект на /editors; иначе текст ошибки, либо null.
     */
    suspend fun uploadPhoto(
        dir: String,
        fileName: String,
        image: ImageUpload,
        cookie: String,
    ): String? {
        val content =
            MultiPartFormDataContent(
                formData {
                    append("DIR", dir)
                    append("FILE_NAME", fileName)
                    append(
                        "FILE",
                        image.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, image.mime)
                            append(HttpHeaders.ContentDisposition, "filename=\"photo\"")
                        },
                    )
                    // латинское «(Store)» — машинный маркер операции для CGI
                    append("OPERATION", "Сохранить! (Store)")
                },
            )
        val resp =
            client.post("$baseUrl/cgi-bin/file_manager") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                header("Referer", "$baseUrl/cgi-bin/file_manager?DIR=$dir")
                if (cookie.isNotBlank()) header("Cookie", cookie)
                setBody(content)
            }
        return errorFromResponse(resp)
    }

    /** Склеивает cookie-строки, дедуплицируя по имени (последнее значение побеждает). */
    private fun mergeCookies(vararg cookies: String): String {
        val map = LinkedHashMap<String, String>()
        cookies.filter { it.isNotBlank() }.forEach { c ->
            c.split(";").map { it.trim() }.filter { it.contains("=") }.forEach { pair ->
                map[pair.substringBefore("=")] = pair.substringAfter("=")
            }
        }
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    /** Данные аккаунта из cookie ZUI (имя&почта&адрес…) для префилла формы комментария. */
    fun accountInfo(cookie: String): AccountInfo {
        val zui = cookieValue(cookie, "ZUI")
        if (zui.isBlank()) return AccountInfo(cookieValue(cookie, "NAME"), "", "")
        val parts = zui.split("&")
        val name = parts.getOrNull(0)?.trim().orEmpty()
        val rest = parts.drop(1).map { it.trim() }
        val email = rest.firstOrNull { it.contains("@") }.orEmpty()
        val url = rest.firstOrNull { it.startsWith("http", ignoreCase = true) }.orEmpty()
        return AccountInfo(name, email, url)
    }

    /** Значение cookie по имени из строки «a=1; b=2» (cp1251 url-decoded). */
    private fun cookieValue(
        cookie: String,
        name: String,
    ): String =
        cookie
            .split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("$name=") }
            ?.substringAfter("=")
            ?.let { runCatching { URLDecoder.decode(it, "windows-1251") }.getOrDefault(it) }
            .orEmpty()

    /** Действие над комментарием по MSGID (напр. delete) — GET с параметрами и сессией. */
    suspend fun commentAction(
        commentsLink: String,
        operation: String,
        msgid: String,
        loginCookie: String,
    ): Boolean {
        val workNoSuffix =
            commentsLink
                .removePrefix(SamlibLinkFormat.COMMENT_PREFIX)
                .removeSuffix(SamlibLinkFormat.WORK_SUFFIX)
        val token =
            noRedirectClient.get("$baseUrl/cgi-bin/comment?COMMENT=$workNoSuffix") {
                header("User-Agent", USER_AGENT)
                if (loginCookie.isNotBlank()) header("Cookie", loginCookie)
            }
        val cookie = mergeCookies(loginCookie, collectCookies(token))
        val sep = if (commentsLink.contains("?")) "&" else "?"
        val resp =
            client.get("$baseUrl$commentsLink${sep}OPERATION=$operation&MSGID=$msgid") {
                header("User-Agent", USER_AGENT)
                header("Accept", ACCEPT)
                header("Referer", "$baseUrl$commentsLink")
                if (cookie.isNotBlank()) header("Cookie", cookie)
            }
        return resp.status.value in 200..399
    }

    /**
     * Полное чтение тела ответа как cp1251-строки. toInputStream().readBytes() —
     * блокирующий вызов, поэтому уводим его на IO-пул: иначе он занимает поток
     * CPU-ограниченного Default-пула (на нём Orbit выполняет intent-ы).
     */
    private suspend fun HttpResponse.bodyCp1251(): String =
        withContext(Dispatchers.IO) {
            String(bodyAsChannel().toInputStream().readBytes(), cp1251)
        }

    /** HTML → плоский текст: без <script>, тегов и лишних пробелов. */
    private fun String.htmlToPlain(): String = replace(SCRIPT_RE, " ").replace(TAG_RE, " ").replace(WS_RE, " ").trim()

    /**
     * Общий разбор ответа CGI-форм: успех — редирект (3xx) → null;
     * 200 со словами об ошибке → её текст; иначе null.
     */
    private suspend fun errorFromResponse(resp: HttpResponse): String? {
        if (resp.status.value in 300..399) return null
        val plain = runCatching { resp.bodyCp1251() }.getOrDefault("").htmlToPlain()
        return if (plain.contains("ошибк", ignoreCase = true) ||
            plain.contains(
                "Error",
                ignoreCase = true,
            )
        ) {
            plain.take(ERROR_SNIPPET_LEN)
        } else {
            null
        }
    }

    private fun collectCookies(resp: HttpResponse): String =
        resp.headers
            .getAll(HttpHeaders.SetCookie)
            ?.mapNotNull { it.substringBefore(";").trim().ifBlank { null } }
            ?.joinToString("; ")
            .orEmpty()

    private fun formBody(params: Map<String, String>): ByteArray =
        params.entries
            .joinToString("&") { (k, v) ->
                URLEncoder.encode(k, "windows-1251") + "=" + URLEncoder.encode(v, "windows-1251")
            }.toByteArray(cp1251)

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0"
        const val ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"

        /** Предкомпилированные regex — используются на каждом ответе сервера. */
        val SCRIPT_RE = Regex("(?is)<script.*?</script>")
        val TAG_RE = Regex("(?is)<[^>]+>")
        val WS_RE = Regex("\\s+")

        /** Максимальная длина показываемого пользователю фрагмента ошибки сервера. */
        const val ERROR_SNIPPET_LEN = 200

        /** Таймауты сети: «подвешенный» Wi-Fi без них держит запрос почти бесконечно. */
        const val CONNECT_TIMEOUT_S = 10L
        const val READ_TIMEOUT_S = 30L
        const val WRITE_TIMEOUT_S = 30L

        /** Быстрая проверка доступности хоста (isHostReachable). */
        const val REACHABLE_TIMEOUT_MS = 5_000L
    }
}
