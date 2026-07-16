package ru.samlib.client.ui.search

import android.app.SearchManager
import android.content.Intent
import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.domain.model.SearchEngine
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import ru.samlib.client.domain.samlib.SamlibUrls
import ru.samlib.client.ui.R
import ru.samlib.client.ui.common.ErrorBox
import ru.samlib.client.ui.common.LoadingBox
import java.net.URLDecoder
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenLink: (String) -> Unit,
    onMenu: () -> Unit = {},
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    viewModel.collectSideEffect { effect ->
        when (effect) {
            is SearchSideEffect.OpenInBrowser -> {
                // Дефолтный поисковик устройства; фолбэк — Google в браузере.
                val webSearch =
                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(SearchManager.QUERY, effect.query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                runCatching { context.startActivity(webSearch) }.onFailure {
                    val q = URLEncoder.encode(effect.query, "UTF-8")
                    runCatching { uriHandler.openUri("https://www.google.com/search?q=$q") }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.common_menu),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.search_query_label)) },
                trailingIcon = {
                    // явная нажимаемая кнопка поиска
                    IconButton(onClick = { viewModel.search() }) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_action),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            )
            // Выбор движка поиска.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchEngine.entries.forEach { engine ->
                    FilterChip(
                        selected = state.engine == engine,
                        onClick = { viewModel.setEngine(engine) },
                        label = { Text(engine.title) },
                    )
                }
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    state.loading && state.items.isEmpty() && !state.captcha -> LoadingBox()
                    state.error != null && state.items.isEmpty() ->
                        ErrorBox(
                            state.error!!,
                            viewModel::search,
                        )

                    state.items.isNotEmpty() ->
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(state.items, key = { it.link }) { work ->
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenLink(work.link) }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                ) {
                                    Text(
                                        work.title.orEmpty(),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        listOfNotNull(work.authorShortName, work.size?.let { "${it}k" })
                                            .joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                HorizontalDivider()
                            }
                            if (state.canLoadMore) {
                                item {
                                    Button(
                                        onClick = { viewModel.loadMore() },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                    ) {
                                        Text(
                                            if (state.loading) {
                                                stringResource(R.string.search_loading)
                                            } else {
                                                stringResource(R.string.search_load_more)
                                            },
                                        )
                                    }
                                }
                            }
                        }

                    state.searched && !state.captcha && state.engine != SearchEngine.BROWSER ->
                        Text(
                            stringResource(R.string.search_no_results),
                            modifier = Modifier.padding(16.dp),
                        )

                    !state.searched && !state.captcha ->
                        Text(
                            if (state.engine == SearchEngine.BROWSER) {
                                stringResource(R.string.search_hint_browser)
                            } else {
                                stringResource(R.string.search_hint_default)
                            },
                            modifier = Modifier.padding(16.dp),
                        )
                }
                // WebView веб-поиска: смонтирован пока идёт фетч; ВИДЕН только при капче
                // (иначе 1dp — грузит и извлекает HTML в фоне, показываем нативный список).
                if (state.engine == SearchEngine.WEB && state.webActive) {
                    WebSearchInline(
                        url = state.webUrl,
                        onOpenResult = onOpenLink,
                        onHtml = viewModel::onWebHtml,
                        // при капче — на весь экран; при фоновом фетче — 1dp и прозрачно (без моргания)
                        modifier =
                            if (state.captcha) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier
                                    .size(1.dp)
                                    .alpha(0f)
                            },
                    )
                }
            }
        }
    }
}

// JS: прячем шапку/форму ввода DDG, оставляем только капчу/результаты.
private const val HIDE_DDG_INPUT_JS =
    "(function(){var s=document.createElement('style');" +
        "s.innerHTML='.header,.header--html,form,#header,.site-header,.search{display:none!important;}';" +
        "document.documentElement.appendChild(s);})()"

/** Раскодировать строку-результат evaluateJavascript (JSON-литерал) в обычный HTML. */
private fun decodeJsString(value: String): String =
    runCatching { JSONArray("[$value]").getString(0) }.getOrDefault(value)

/**
 * Веб-поиск через WebView (реальный браузер надёжно обходит бот-защиту DDG).
 * Грузит [url], извлекает HTML выдачи из DOM → [onHtml] (парсится в нативный список).
 * Виден только при капче; клик по произведению (.shtml) открывает читалку.
 */
@Composable
private fun WebSearchInline(
    url: String,
    onOpenResult: (String) -> Unit,
    onHtml: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val webView =
        remember {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(Color.TRANSPARENT)
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient =
                    object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            val raw = request?.url?.toString() ?: return false
                            val target =
                                if (raw.contains("uddg=")) {
                                    runCatching {
                                        URLDecoder.decode(
                                            raw.substringAfter("uddg=").substringBefore("&"),
                                            "UTF-8",
                                        )
                                    }.getOrDefault(raw)
                                } else {
                                    raw
                                }
                            // клик по результату samlib → открыть в приложении (тип определит openResult)
                            if (target.contains(SamlibUrls.HOST)) {
                                val path = SamlibLinkFormat.normalizeLink(target)
                                if (path != null &&
                                    (
                                        path.endsWith(SamlibLinkFormat.WORK_SUFFIX) ||
                                            Regex("^/[a-z]/[a-zA-Z0-9_]+/$").matches(
                                                path,
                                            )
                                    )
                                ) {
                                    onOpenResult(path)
                                    return true
                                }
                            }
                            return false
                        }

                        override fun onPageFinished(
                            view: WebView?,
                            pageUrl: String?,
                        ) {
                            view?.evaluateJavascript(HIDE_DDG_INPUT_JS, null)
                            // извлекаем HTML выдачи из DOM браузера и парсим в список
                            view?.evaluateJavascript(
                                "(function(){return document.documentElement.outerHTML})()",
                            ) { res ->
                                if (res != null && res != "null") onHtml(decodeJsString(res))
                            }
                        }
                    }
            }
        }
    LaunchedEffect(url) {
        if (url.isNotBlank()) webView.loadUrl(url)
    }
    AndroidView(factory = { webView }, modifier = modifier)
}
