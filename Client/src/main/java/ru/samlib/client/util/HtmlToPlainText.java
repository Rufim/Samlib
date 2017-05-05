package ru.samlib.client.util;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import ru.kazantsev.template.util.TextUtils;

/**
 * Created by Admin on 05.05.2017.
 */
public class HtmlToPlainText {

    /**
     * Format an Element to plain-text
     *
     * @param element the root element to format
     * @return formatted text
     */
    public String getPlainText(Element element) {
        FormattingVisitor formatter = new FormattingVisitor();
        NodeTraversor traversor = new NodeTraversor(formatter);
        traversor.traverse(element); // walk the DOM, and call .head() and .tail() for each node

        return formatter.toString();
    }

    // the formatting rules, implemented in a breadth-first DOM traverse
    private class FormattingVisitor implements NodeVisitor {
        private StringBuilder accum = new StringBuilder(); // holds the accumulated text

        // hit when the node is first seen
        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (node instanceof TextNode) {
                Node parent;
                if ((parent = getParentOrNull(node, "pre")) != null) {
                    append(((TextNode) node).getWholeText());
                } else if((parent = getParentOrNull(node, "a")) != null) {
                    append(parent.outerHtml());
                } else {
                    append(((TextNode) node).text()); // TextNodes carry all user-readable text in the DOM.
                }
            } else if (name.equals("li")) {
                append("\n * ");
            } else if (name.equals("dt")) {
                append("  ");
            } else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr")){
                append("\n");
            } else if (name.equals("img")) {
                append(node.outerHtml());
            }
        }

        // hit when all of the node's children (if any) have been visited
        public void tail(Node node, int depth) {
            String name = node.nodeName();
            if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5"))
                append("\n");
        }

        // appends text to the string builder with a simple word wrap method
        private void append(String text) {
            if(accum.length() > 3) {
                if (text.equals(" ") &&
                        (StringUtil.in(accum.substring(accum.length() - 1), " ", "\n")))
                    return; // don't accumulate long runs of empty spaces
                if (text.equals("\n") && accum.substring(accum.length() - 2).contains("\n\n")) {
                    return;  // don't accumulate new lines
                }
            }
            accum.append(text);
        }

        @Override
        public String toString() {
            return accum.toString();
        }

        private Node getParentOrNull(Node node, String tag) {
            Node parentNode;
            while ((parentNode = node.parent()) != null) {
                if(parentNode.nodeName().equals(tag)) {
                    return parentNode;
                } else {
                    node = parentNode;
                }
            }
            return null;
        }
    }
}

