package ru.samlib.client.parser;

import android.support.v4.util.LruCache;
import android.text.Html;
import android.util.Log;
import ru.kazantsev.template.domain.Valuable;
import ru.kazantsev.template.net.HTTPExecutor;
import ru.kazantsev.template.net.Response;
import ru.kazantsev.template.util.charset.CharsetDetector;
import ru.kazantsev.template.util.charset.CharsetMatch;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import ru.kazantsev.template.net.CachedResponse;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.util.SystemUtils;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.*;

import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.util.ParserUtils;
import ru.kazantsev.template.util.TextUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by Rufim on 04.07.2015.
 */
public class WorkParser extends Parser {

    private static LruCache<String, Work> workCache = new LruCache<>(3);


    private static final int MAX_INDENT_SIZE = 700;
    private Work work;

    public WorkParser(Work work) throws MalformedURLException {
        setPath(work.getLink());
        this.work = work;
    }

    public static Work getCachedWork(String link) {
        return workCache.get(link);
    }

    public WorkParser(String workLink) throws MalformedURLException {
        setPath(workLink);
        this.work = new Work(workLink);
    }

    public Work parse(boolean fullDownload, boolean processChapters) throws IOException {
        CachedResponse rawContent = null;
        if (work.getRawContent() == null && !fullDownload) {
            rawContent = HtmlClient.executeRequest(request, MIN_BODY_SIZE, cached);
        } else {
            rawContent = HtmlClient.executeRequest(request, cached);
        }
        if (rawContent == null || rawContent.length() == 0) {
            throw new IOException("Закешированный файл не найден и отцутствует соединение с интернетом");
        }
        if (work == null) {
            work = new Work(rawContent.getRequest().getBaseUrl().getPath().replaceAll("/+", "/"));
        }
        work.setCachedResponse(rawContent);
        Work parsedWork = parse(rawContent, rawContent.getEncoding(), work, processChapters);
        parsedWork.setCachedResponse(rawContent);
        return parsedWork;
    }

    public static String detectCharset(File file, String defaultCharset) {
        CharsetDetector detector = new CharsetDetector();
        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(file));
            detector.setText(stream);
            CharsetMatch charset = detector.detect();
            return charset == null ? defaultCharset : charset.getName();
        } catch (IOException e) {
            return defaultCharset;
        } finally {
            SystemUtils.close(stream);
        }
    }

    public static Work parse(File rawContent, String encoding, Work work, boolean processChapters) throws IOException {
        try {
            if (work.isNotSamlib()) {
                work.setRawContent(SystemUtils.readFile(rawContent, detectCharset(rawContent, encoding)));
            } else {
                work = parseWork(rawContent, encoding, work);
            }
            if (rawContent instanceof CachedResponse) {
                Log.i(TAG, "Work parsed using url " + work.getFullLink());
            } else {
                Log.i(TAG, "Work parsed using file " + rawContent.getAbsolutePath());
            }
            work.setChanged(false);
            if (processChapters) {
                processChapters(work, work.isNotSamlib() && !rawContent.getName().endsWith(".html"));
            }
            workCache.put(work.isNotSamlib() ? rawContent.getAbsolutePath() : work.getLink(), work);
        } catch (Exception ex) {
            work.setParsed(false);
            if (rawContent instanceof CachedResponse) {
                Log.e(TAG, "Error in work parsing using url " + work.getFullLink(), ex);
            } else {
                Log.e(TAG, "Error in work parsing using file " + rawContent.getAbsolutePath(), ex);
            }
        }
        return work;
    }

    public static Work parseWork(File file, String encoding, Work work) {
        String[] parts;
        if (!work.getLink().matches(work.getAuthor().getLink() + "rating\\d.shtml")) {
            parts = TextUtils.Splitter.extractLines(file, encoding, true,
                    new TextUtils.Splitter().addEnd("Первый блок ссылок"),
                    new TextUtils.Splitter("Блок описания произведения", "Кнопка вызова Лингвоанализатора"),
                    new TextUtils.Splitter().addStart("Блочек голосования").addStart("<!-------.*").addEnd("Собственно произведение"),
                    new TextUtils.Splitter().addEnd("<!-------.*"));
        } else {
            parts = TextUtils.Splitter.extractLines(file, encoding, true,
                    new TextUtils.Splitter().addEnd("Первый блок ссылок"),
                    new TextUtils.Splitter("<table width=90% border=0 cellpadding=0 cellspacing=0><tr>", "</tr></table>"),
                    new TextUtils.Splitter("<hr align=\"CENTER\" size=\"2\" noshade>", "<hr align=\"CENTER\" size=\"2\" noshade>"));
        }
        if (parts.length == 0) {
            return work;
        }
        Document head = Jsoup.parseBodyFragment(parts[0]);
        if (work.getAuthor().getFullName() == null) {
            work.getAuthor().setFullName(head.select("div > h3").first().ownText().split(":")[0]);
        }
        work.setTitle(head.select("center > h2").text());
        Elements lis = Jsoup.parseBodyFragment(parts[1]).select("li");
        int index = 2;
        if (lis.get(0).text().contains("Copyright")) {
            index--;
        } else {
            work.setHasComments(true);
        }
        String info = lis.get(index++).text();
        String[] data = new String[0];
        if (info.contains("Размещен")) {
            data = TextUtils.Splitter.extractStrings(info, true,
                    new TextUtils.Splitter("Размещен: ", ","),
                    new TextUtils.Splitter("изменен: ", "\\."),
                    new TextUtils.Splitter(" ", "k"));
        }
        if (info.contains("Обновлено")) {
            data = TextUtils.Splitter.extractStrings(info, true,
                    new TextUtils.Splitter("Обновлено: ", "\\."),
                    new TextUtils.Splitter(" ", "k"));
        }
        if (data.length > 0) {
            if (data.length == 2) {
                work.setUpdateDate(ParserUtils.parseData(data[0]));
                if (!data[1].contains("Статистика")) {
                    work.setSize(Integer.parseInt(data[1]));
                }
            }
            if (data.length == 3) {
                work.setCreateDate(ParserUtils.parseData(data[0]));
                work.setUpdateDate(ParserUtils.parseData(data[1]));
                work.setSize(Integer.parseInt(data[2]));
            }
        }
        if (lis.size() > index) {
            String typeGenre[] = lis.get(index++).text().split(":");
            work.setType(Type.parseType(typeGenre[0]));
            if (typeGenre.length > 1) {
                work.setGenresAsString(typeGenre[1]);
            }
            boolean hasNotDefaultCategory = false;
            for (int i = index; i < lis.size(); i++) {
                String text = lis.get(i).text();
                Element a = lis.get(i).select("a").first();
                if (text.contains("Иллюстрации")) {
                    work.setHasIllustration(true);
                } else if (text.contains("Скачать")) {
                    break;
                } else if (a != null) {
                    Category category = new Category();
                    category.setTitle(text);
                    category.setLink(a.attr("href"));
                    category.setAuthor(work.getAuthor());
                    if (work.getCategory() == null || !work.getCategory().equals(category)) {
                        work.setCategory(category);
                    }
                    hasNotDefaultCategory = true;
                }
            }
            if (!hasNotDefaultCategory) {
                Category category = new Category();
                category.setType(work.getType());
                if (work.getCategory() == null) {
                    work.setCategory(category);
                }
            }
        }
        if (parts.length == 3) {
            work.setRawContent(parts[2]);
        } else if (parts.length == 4) {
            if (parts[2].contains("Аннотация")) {
                work.setAnnotationBlocks(Arrays.asList(ParserUtils.cleanupHtml(Jsoup.parseBodyFragment(parts[2]).select("i"))));

            }
            if(parts[2].contains("Оценка:")) {
                work.setHasRate(true);
            }
            if (parts[3].contains("<!--Section Begins-->")) {
                work.setRawContent(TextUtils.Splitter.extractLines(file, encoding, true,
                        new TextUtils.Splitter("<!--Section Begins-->", "<!--Section Ends-->").setMatchCount(999))[0]);
            } else {
                work.setRawContent(parts[3]);
            }
        }
     /*   if(work.getRawContent() != null) {
            String oldMd5 = work.getMd5();
            work.setMd5(TextUtils.calculateMD5(work.getRawContent(), file.getRequest().getEncoding()));
            if(!work.getMd5().equals(oldMd5)) work.setChanged(true);
        } */
        return work;
    }

    public static void processChapters(Work work, boolean isTextFile) {
        if(isTextFile && !work.getRawContent().startsWith("<html>")) {
            work.setIndents(Arrays.asList(work.getRawContent().split("\n")));
        } else {
            List<String> indents = work.getIndents();
            try {
                indents.clear();
            } catch (RuntimeException ex) {
                // ignored
            }
            List<Bookmark> bookmarks = work.getAutoBookmarks();
            Document document = Jsoup.parseBodyFragment(work.getRawContent());
            document.setBaseUri(Constants.Net.BASE_DOMAIN);
            document.outputSettings().prettyPrint(false);
            List<Node> rootNodes = new ArrayList<>();
            //Element body = replaceTables(document.body());
            Elements rootElements = document.body().select("> *");
            HtmlToTextForSpanner forSpanner = new HtmlToTextForSpanner();
            work.setIndents(forSpanner.getIndents(rootElements));
            if (rootElements != null) {
                rootElements.clear();
            }
            Pattern pattern = Pattern.compile("((Пролог)|(Эпилог)|(Интерлюдия)|(Приложение)|(Глава \\d+)|(Часть \\d+)).*",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            bookmarks.clear();
            bookmarks.addAll(forSpanner.getBookmarks());
        /*if(bookmarks.size() == 0) {
            for (int i = 0; i < indents.size(); i++) {
                String text = indents.get(i);
                if (pattern.matcher(text).find() && pattern.matcher(text = TextUtils.trim(Html.fromHtml(text).toString())).matches()) {
                    Bookmark newBookmark = new Bookmark(text);
                    newBookmark.setPercent(((double) i) / indents.size());
                    newBookmark.setIndentIndex(i);
                    bookmarks.add(newBookmark);
                }
            }
        }*/
        }
        work.setParsed(true);
    }

    private static String[] firstWord = new String[]{"Пролог", "Эпилог", "Интерлюдия", "Приложение", "Глава", "Часть"};

    private static void addIndent(List<String> indents, String indent) {
        int maxOverflow = (int) (MAX_INDENT_SIZE + (MAX_INDENT_SIZE * 0.25));
        if (indent.length() > maxOverflow) {
            for (int index = 0; indent.length() > index + MAX_INDENT_SIZE; ) {
                if (indent.length() > index + maxOverflow) {
                    StringBuilder accum = new StringBuilder(indent.substring(index, index + MAX_INDENT_SIZE));
                    index = index + MAX_INDENT_SIZE;
                    int size = index;
                    while (index < indent.length() && indent.charAt(size) != ' ') {
                        size++;
                    }
                    if (size > 0) {
                        accum.append(indent.substring(index, size));
                        index += size;
                    }
                    indents.add(accum.toString());
                }
                if (indent.length() <= index + MAX_INDENT_SIZE) {
                    indents.add(indent.substring(index));
                    break;
                }
            }
        } else {
            indents.add(indent);
        }
    }

    private static boolean isEmpty(String sequence) {
        if (sequence.equals("\n")) return false;
        else return TextUtils.isEmpty(TextUtils.trim(sequence));
    }

    /**
     * Created by Admin on 05.05.2017.
     */
    public static class HtmlToTextForSpanner {

        private List<Bookmark> bookmarks = new ArrayList<>();
        private List<String> indents = new ArrayList<>();

        /**
         * Format an Element to plain-text
         *
         * @param elements the root element to format
         * @return formatted text
         */
        public List<String> getIndents(Elements elements) {
            for (Element element : elements) {
                FormattingVisitor formatter = new FormattingVisitor(indents);
                NodeTraversor traversor = new NodeTraversor(formatter);
                traversor.traverse(element);
            }
            return indents;
        }

        public List<Bookmark> getBookmarks() {
            List<Bookmark> resultBookmarks = new ArrayList<>();
            for (Bookmark bookmark : bookmarks) {
                if (bookmark.getIndentIndex() != 0 && !TextUtils.isEmpty(bookmark.getTitle())) {
                    resultBookmarks.add(bookmark);
                }

            }
            return resultBookmarks;
        }

        // the formatting rules, implemented in a breadth-first DOM traverse
        private class FormattingVisitor implements NodeVisitor {

            private List<String> indents;

            public FormattingVisitor(List<String> indents) {
                this.indents = indents;
            }

            // hit when the node is first seen
            public void head(Node node, int depth) {
                String nodeName = node.nodeName();
                if (node instanceof TextNode) {
                    Node parent;
                    if ((parent = getParentOrNull(node, "pre")) != null) {
                        append(((TextNode) node).getWholeText());
                    } else if ((parent = getParentOrNull(node, "a")) != null) {
                        if (parent.hasAttr("href")) {
                            String href = parent.attr("href");
                            if (href.startsWith("#")) {
                                Bookmark bookmark = new Bookmark(((TextNode) node).text());
                                bookmark.setIndent(href.substring(1));
                                bookmarks.add(bookmark);
                            } else {
                                if (href.startsWith("/")) {
                                    append("<a href=\"" + Constants.Net.BASE_DOMAIN + href + "\">" + getNodeHtml(node) + "");
                                } else {
                                    append("<a href=\"" + href + "\">" + getNodeHtml(node) + "");
                                }
                            }
                        } else if (parent.hasAttr("name")) {
                            initBookmark(parent.attr("name"));
                        } else {
                            append(getNodeHtml(node));
                        }
                    } else if ((parent = getParentOrNull(node, "i", "b")) != null) {
                        append("<" + parent.nodeName() + ">" + getNodeHtml(node) + "</" + parent.nodeName() + ">");
                    } else {
                        append(getNodeHtml(node)); // TextNodes carry all user-readable text in the DOM.
                    }
                } else if (nodeName.equals("dt")) {
                    append("  ");
                } else if (StringUtil.in(nodeName, "p", "h1", "h2", "h3", "h4", "h5", "tr")) {
                    append("\n");
                } else if (nodeName.equals("img")) {
                    append("\n" + getNodeHtml(node));
                } else if (nodeName.equals("a")) {
                    Element a = (Element) node;
                    if (a.hasAttr("name")) {
                        initBookmark(a.attr("name"));
                    }
                }
            }

            private void initBookmark(String name) {
                for (Bookmark bookmark : bookmarks) {
                    if (bookmark.getIndent().equals(name)) {
                        append("");
                        bookmark.setIndentIndex(indents.size());
                    }
                }
            }

            // hit when all of the node's children (if any) have been visited
            public void tail(Node node, int depth) {
                String name = node.nodeName();
                if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5", "div"))
                    append("\n");
            }

            private String getNodeHtml(Node node) {
                return node.outerHtml().replace("\n", "");
            }

            boolean newLine = true;

            // appends text to the string builder with a simple word wrap method
            private void append(String text) {
                if (text.length() == 0) return;
                if (indents.size() > 2) {
                    if (text.equals(" ") && lastString().endsWith(" ")) {
                        return; // don't accumulate long runs of empty spaces
                    }
                    if (text.startsWith("\n") && isLastTwoEmptyIndents()) {
                        int i = 0;
                        // don't accumulate new lines
                        while (text.charAt(i) == '\n') {
                            i++;
                            if (text.length() == i) {
                                return;
                            }
                        }
                        text = text.substring(i);
                    }
                }
                if (text.equals("\n")) {
                    newLine = true;
                } else if (!text.contains("\n")) {
                    if (newLine) {
                        indents.add(text);
                        newLine = false;
                    } else {
                        appendText(text);
                    }
                } else {
                    List<String> strings = Arrays.asList(text.split("\n"));
                    if (strings.size() > 0) {
                        if (text.startsWith("\n") || newLine) {
                            indents.addAll(strings);
                        } else {
                            appendText(strings.get(0));
                            if (strings.size() > 1) {
                                indents.addAll(strings.subList(1, strings.size()));
                            }
                        }
                    }
                    if (text.endsWith("\n")) {
                        newLine = true;
                    }
                }
            }

            private void appendText(String text) {
                if (indents.size() > 0) {
                    indents.set(indents.size() - 1, lastString() + text);
                } else {
                    indents.add(text);
                }
            }

            private boolean isLastTwoEmptyIndents() {
                if (indents.size() > 2) {
                    if (lastString().trim().isEmpty()) {
                        return indents.get(indents.size() - 2).trim().isEmpty();
                    }
                }
                return false;
            }

            private String lastString() {
                if (indents.size() == 0) return "";
                return indents.get(indents.size() - 1);
            }

            public List<String> toList() {
                return indents;
            }

            private Node getParentOrNull(Node node, String... tags) {
                Node parentNode;
                while ((parentNode = node.parent()) != null) {
                    if (StringUtil.in(parentNode.nodeName(), tags)) {
                        return parentNode;
                    } else {
                        node = parentNode;
                    }
                }
                return null;
            }
        }
    }

    private static final String WORK_SEND_RATE = "/cgi-bin/votecounter";

    public enum RateParams {
        FILE, DIR, BALL, OK;
    }

    public static Response sendRate(Work work, int rate) {
        try {
            String link = Constants.Net.BASE_DOMAIN + WORK_SEND_RATE;
            String wlink = work.getLinkWithoutSuffix();
            String alink = work.getAuthor().getLink();
            commentCookie = CommentsParser.requestCookie(work);
            Request request = new Request(link)
                    .setMethod(Request.Method.POST)
                    .addHeader("Accept", ACCEPT_VALUE)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Accept-Encoding", ACCEPT_ENCODING_VALUE)
                    .addHeader("Host", Constants.Net.BASE_HOST)
                    .addHeader("Referer", link)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Upgrade-Insecure-Requests", "1")
                    .setEncoding("CP1251")
                    .addParam(RateParams.FILE, wlink.substring(wlink.lastIndexOf('/') + 1))
                    .addParam(RateParams.DIR, alink.substring(1, alink.length() - 1))
                    .addParam(RateParams.BALL, String.valueOf(rate))
                    .addParam(RateParams.OK, "OK");
            HTTPExecutor executor = new HTTPExecutor(request);
            return executor.execute();
        } catch (Exception e) {
            return null;
        }
    }
}
