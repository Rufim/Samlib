package ru.samlib.client.data.repository

import ru.samlib.client.data.parser.SearchParser
import ru.samlib.client.data.parser.WebSearchParser
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.data.remote.cp1251Encode
import ru.samlib.client.data.remote.samlibCatching
import ru.samlib.client.domain.model.Work
import ru.samlib.client.domain.repository.SamlibResult
import ru.samlib.client.domain.repository.SearchRepository
import ru.samlib.client.domain.repository.WebResults
import java.net.URLEncoder

class DefaultSearchRepository(
    private val http: SamlibHttpClient,
    private val parser: SearchParser,
    private val webParser: WebSearchParser,
) : SearchRepository {
    override suspend fun search(
        query: String,
        page: Int,
    ): SamlibResult<List<Work>> =
        samlibCatching {
            val q = "DIR=&PLACE=index&FIND=${cp1251Encode(query)}&JANR=0&TYPE=0&PAGE=$page"
            parser.parse(http.getEncoded("/cgi-bin/seek", q))
        }

    override fun parseWeb(html: String): WebResults {
        val works = webParser.parse(html)
        val captcha = works.isEmpty() && webParser.isCaptcha(html)
        val finished = works.isNotEmpty() || captcha || webParser.isNoResults(html)
        return WebResults(works, captcha, finished)
    }

    override fun webSearchUrl(query: String): String =
        "https://html.duckduckgo.com/html/?kl=ru-ru&q=" +
            URLEncoder.encode("site:samlib.ru $query", "UTF-8")
}
