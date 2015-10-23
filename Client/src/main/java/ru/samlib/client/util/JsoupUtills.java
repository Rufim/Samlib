package ru.samlib.client.util;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.*;
import org.jsoup.safety.Whitelist;

/**
 * Created by Dmitry on 20.10.2015.
 */
public class JsoupUtills {

    public static String cleanHtml(String str) {
        Document.OutputSettings settings = new Document.OutputSettings();
        settings.escapeMode(Entities.EscapeMode.xhtml);
        return Jsoup.clean(str, "", Whitelist.none(), settings);
    }

    public static String ownText(Element element) {
        StringBuilder builder = new StringBuilder();
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;
                appendNormalisedText(builder, textNode);
            } else if (child instanceof Element) {
                appendNewlineIfBr((Element) child, builder);
            }
        }
        return builder.toString().trim();
    }

    private static void appendNormalisedText(StringBuilder accum, TextNode textNode) {
        String text = textNode.getWholeText();

        if (preserveWhitespace(textNode.parentNode()))
            accum.append(text);
        else
            StringUtil.appendNormalisedWhitespace(accum, text, lastCharIsWhitespace(accum));
    }

    private static void appendNewlineIfBr(Element element, StringBuilder accum) {
        if (element.tag().getName().equals("br")) accum.append("\n");
    }

    private static boolean preserveWhitespace(Node node) {
        // looks only at this element and one level up, to prevent recursion & needless stack searches
        if (node != null && node instanceof Element) {
            Element element = (Element) node;
            return element.tag().preserveWhitespace() ||
                    element.parent() != null && element.parent().tag().preserveWhitespace();
        }
        return false;
    }

    private static boolean lastCharIsWhitespace(StringBuilder sb) {
        return sb.length() != 0 && sb.charAt(sb.length() - 1) == ' ';
    }

}
