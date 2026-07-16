package ru.samlib.client.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

private val linkStyles =
    TextLinkStyles(
        style = SpanStyle(color = Color(0xFF1565C0), textDecoration = TextDecoration.Underline),
    )

private val BARE_URL = Regex("""https?://[^\s<>"')]+|/[\w./%-]+\.shtml""")

fun htmlToAnnotatedString(
    html: String,
    linkListener: LinkInteractionListener? = null,
): AnnotatedString =
    buildAnnotatedString {
        val root = Jsoup.parseBodyFragment(html).body()

        fun appendWithLinks(text: String) {
            if (linkListener == null) {
                append(text)
                return
            }
            var last = 0
            for (m in BARE_URL.findAll(text)) {
                if (m.range.first > last) append(text.substring(last, m.range.first))
                withLink(LinkAnnotation.Url(m.value, linkStyles, linkListener)) { append(m.value) }
                last = m.range.last + 1
            }
            if (last < text.length) append(text.substring(last))
        }

        fun render(node: Node) {
            for (child in node.childNodes()) {
                when (child) {
                    is TextNode -> appendWithLinks(child.text())
                    is Element ->
                        when (child.tagName().lowercase()) {
                            "br" -> append("\n")
                            "b", "strong" ->
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    render(
                                        child,
                                    )
                                }

                            "i", "em" -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { render(child) }
                            "u", "ins" ->
                                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                    render(
                                        child,
                                    )
                                }

                            "s", "strike", "del" ->
                                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                    render(
                                        child,
                                    )
                                }

                            "a" -> {
                                val href = child.attr("href").trim()
                                if (href.isEmpty() || linkListener == null) {
                                    render(child)
                                } else {
                                    withLink(LinkAnnotation.Url(href, linkStyles, linkListener)) {
                                        render(
                                            child,
                                        )
                                    }
                                }
                            }

                            else -> render(child)
                        }
                }
            }
        }

        render(root)
    }
