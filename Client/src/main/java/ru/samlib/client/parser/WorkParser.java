package ru.samlib.client.parser;

import android.support.v4.util.LruCache;
import android.util.Log;

import net.vrallev.android.cat.Cat;

import ru.kazantsev.template.net.*;
import ru.kazantsev.template.util.charset.CharsetDetector;
import ru.kazantsev.template.util.charset.CharsetMatch;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import ru.kazantsev.template.util.SystemUtils;
import ru.samlib.client.App;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.*;

import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.util.ParserUtils;
import ru.kazantsev.template.util.TextUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        try {
            if (work.getRawContent() == null && !fullDownload) {
                rawContent = HtmlClient.executeRequest(request, MIN_BODY_SIZE, cached || lazyLoad);
            } else {
                rawContent = HtmlClient.executeRequest(request, cached || lazyLoad);
            }
        } catch (Throwable tr) {
            if (lazyLoad) {
                lazyLoad = false;
                Cat.w(tr);
                return parse(fullDownload, processChapters);
            } else {
                throw tr;
            }
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
        return parse(rawContent, "text/html", encoding, work, processChapters);
    }

    public static Work parse(File rawContent, String mimeType, String encoding, Work work, boolean processChapters) throws IOException {
        try {
            if (work.isNotSamlib()) {
                File unzipped = null;
                if (mimeType == null) mimeType = "";
                if (rawContent.getName().endsWith(".zip") || mimeType.contains("zip")) {
                    unzipped = HtmlClient.getCachedFile(App.getInstance(), "unzip.tmp", true);
                    boolean fileFound = false;
                    unzipped.delete();
                    if (unzipped.createNewFile()) {
                        try (FileInputStream fin = new FileInputStream(rawContent);
                             ZipInputStream zin = new ZipInputStream(fin)) {
                            ZipEntry ze = null;
                            while ((ze = zin.getNextEntry()) != null) {
                                if (!ze.isDirectory() && HtmlClient.isSupportedFormat(ze.getName())) {
                                    fileFound = true;
                                    FileOutputStream out = new FileOutputStream(unzipped);
                                    SystemUtils.copy(zin, out);
                                    zin.closeEntry();
                                    out.close();
                                    break;
                                }
                            }
                        }
                    }
                    if (!fileFound) {
                        return work;
                    } else {
                        File tmp = rawContent;
                        rawContent = unzipped;
                        unzipped = tmp;
                    }
                }
                String chrset = detectCharset(rawContent, encoding);
                work.setRawContent(SystemUtils.readFile(rawContent, chrset.contains("UTF") ? chrset : encoding));
                if (unzipped != null) {
                    rawContent.delete();
                    rawContent = unzipped;
                }
            } else {
                work = parseWork(rawContent, encoding, work);
            }
            work.setChanged(false);
            if (processChapters) {
                processChapters(work, work.isNotSamlib() && !rawContent.getName().endsWith(".html"));
            }
            if (rawContent instanceof CachedResponse) {
                Log.i(TAG, "Work parsed using url " + work.getFullLink());
            } else {
                Log.i(TAG, "Work parsed using file " + rawContent.getAbsolutePath());
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
        if (!work.getLink().matches(work.getAuthor().getLink() + "rating\\d.shtml") && !work.getLink().endsWith("publish.shtml")) {
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
                    work.setHasFB2(true);
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
            if (parts[2].contains("Оценка:")) {
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

    public static void parseFB2(Work work) {
        Document doc = Jsoup.parse(work.getRawContent(), "", org.jsoup.parser.Parser.xmlParser());
        Elements description = doc.select("> FictionBook description");
        Elements bodys = doc.select("> FictionBook body");
        Elements binary = doc.select("> FictionBook binary");
        if (description.size() == 0) {
            work.setAnnotation("");
        } else {
            Elements genres = description.select("genre");
            if (genres.size() != 0) {
                ArrayList<String> fb2Genres = new ArrayList<>();
                for (Element genre : genres) {
                    FB2Genre fb2Genre = FB2Genre.parseGenre(genre.text());
                    if (fb2Genre != null) {
                        fb2Genres.add(fb2Genre.getName());
                    } else if (!genre.text().isEmpty()) {
                        fb2Genres.add(genre.text());
                    }
                }
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < fb2Genres.size(); i++) {
                    builder.append(fb2Genres.get(i));
                    if (i + 1 != genres.size()) {
                        builder.append(", ");
                    }
                }
                work.setCustomGenres(builder.toString());
            }
            Elements authors = description.select("author");
            if (authors.size() > 0) {
                Element author = authors.first();
                StringBuilder builder = new StringBuilder();
                if (author.select("last-name").size() > 0) {
                    builder.append(author.select("last-name").text());
                }
                if (author.select("middle-name").size() > 0) {
                    if (builder.length() > 0) builder.append(" ");
                    builder.append(author.select("middle-name").text());
                }
                if (author.select("first-name").size() > 0) {
                    if (builder.length() > 0) builder.append(" ");
                    builder.append(author.select("first-name").text());
                }
                work.getAuthor().setFullName(builder.toString());
                work.getAuthor().setShortName(null);
                work.getAuthor().getShortName();
            }
            Elements title = description.select("book-title");
            if (title.size() > 0) {
                work.setTitle(title.text());
            }
            Elements annotation = description.select("annotation");
            work.setAnnotationBlocks(new ArrayList<>());
            if (annotation.size() > 0) {
                work.setAnnotation(annotation.html());
            }
        }
        try {
            work.getIndents().clear();
        } catch (RuntimeException ex) {// ignored
        }
        work.setIndents(new ArrayList<>());
        for (Element body : bodys) {
            parseSection(body, work, binary);
        }
        work.setParsed(true);
    }

    private static void parseSection(Element section, Work work, Elements binary) {
        List<String> indents = work.getIndents();
        String id = section.attr("id");
        if (TextUtils.notEmpty(id)) {
            for (Bookmark bookmark : work.getAutoBookmarks()) {
                if (bookmark.getIndent().equals(id) && bookmark.getIndentIndex() <= 0) {
                    bookmark.setIndentIndex(indents.size());
                }
            }
        }
        for (Element titleP : section.select("> title")) {
            parseFB2Content(titleP, work, binary);
            indents.add("");
        }
        for (Element epigraphP : section.select("> epigraph")) {
            parseFB2Content(epigraphP, work, binary);
            indents.add("");
        }
        for (Element annotation : section.select("> annotation")) {
            parseFB2Content(annotation, work, binary);
            indents.add("");
        }
        for (Element subSection : section.select("> section")) {
            parseSection(subSection, work, binary);
            indents.add("");
        }
        parseFB2Content(section, work, binary);
    }

    private static void addImage(Element element, Work work, Elements binary) {
        String id = element.attr("l:href");
        if (id != null && id.startsWith("#")) {
            Elements image = binary.select("binary[id=" + id.substring(1) + "]");
            if (image.size() > 0) {
                work.getIndents().add("<img src=\"data:" + image.first().attr("content-type") + ";base64," + image.first().text() + "\">");
            }
        }
    }

    private static void parseFB2Content(Elements content, Work work, Elements binary) {
        for (Element element : content) {
            parseFB2Content(element, work, binary);
        }
    }

    private static void parseFB2Content(Element content, Work work, Elements binary) {
        List<String> indents = work.getIndents();
        for (Element children : content.children()) {
            String tag = children.tag().getName();
            if (tag.equalsIgnoreCase("p") || tag.equalsIgnoreCase("text-author") || tag.equalsIgnoreCase("v")) {
                children.select("emphasis").tagName("i");
                children.select("strikethrough").tagName("strike");
                indents.add(children.html());
                Elements refs = children.select("a");
                for (Element ref : refs) {
                    Bookmark bookmark = new Bookmark(ref.text());
                    bookmark.setIndent(ref.attr("l:href").substring(1));
                    work.getAutoBookmarks().add(bookmark);
                }

            }
            if (tag.equalsIgnoreCase("empty-line")) {
                indents.add("");
            }
            if (tag.equalsIgnoreCase("image")) {
                addImage(children, work, binary);
            }
            if (tag.equalsIgnoreCase("poem")) {
                indents.add("");
                for (Element stanza : children.select("stanza")) {
                    for (Element v : stanza.select("v")) {
                        indents.add("<i>" + v.text() + "</i>");
                    }
                    indents.add("");
                }
                Elements author = children.select("text-author");
                if (author.size() > 0) {
                    indents.add(author.text());
                    indents.add("");
                }
            }
            if (tag.equalsIgnoreCase("subtitle")) {
                parseSection(children, work, binary);
            }
            if (tag.equalsIgnoreCase("cite")) {
                parseSection(children, work, binary);
            }
        }
    }

    public static void processChapters(Work work, boolean isNotHtml) {
        String startWith = work.getRawContent().length() > 200 ? work.getRawContent().substring(0, 199) : work.getRawContent();
        if (isNotHtml && !startWith.contains("<html")) {
            if (startWith.contains("<FictionBook")) {
                parseFB2(work);
                return;
            } else {
                work.setIndents(Arrays.asList(work.getRawContent().split("\n")));
            }
        } else {
            List<String> indents = work.getIndents();
            try {
                indents.clear();
            } catch (RuntimeException ex) {
                // ignored
            }
            String baseDomain = work.isNotSamlib() ? "" : Constants.Net.BASE_DOMAIN;
            List<Bookmark> bookmarks = work.getAutoBookmarks();
            Document document = Jsoup.parse(work.getRawContent(), "", org.jsoup.parser.Parser.xmlParser());
            document.setBaseUri(baseDomain);
            document.outputSettings().prettyPrint(false);
            List<Node> rootNodes = new ArrayList<>();
            //Element body = replaceTables(document.body());
            Elements rootElements = null;
            if (document.body() != null) {
                rootElements = document.body().select("> *");
            } else {
                rootElements = document.select("> *");
            }
            HtmlToTextForSpanner forSpanner = new HtmlToTextForSpanner();
            forSpanner.setBaseDomain(baseDomain);
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
        private String baseDomain = "";

        public void setBaseDomain(String baseDomain) {
            this.baseDomain = baseDomain;
        }

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
                    } else if(getParentOrNull(node, "img") == null) {
                        append(getNodeHtml(node)); // TextNodes carry all user-readable text in the DOM.
                    }
                } else if (nodeName.equalsIgnoreCase("a")) {
                    if (node.hasAttr("href")) {
                        String href = node.attr("href");
                        if (href.startsWith("#")) {
                            Bookmark bookmark = new Bookmark(((Element) node).text());
                            bookmark.setIndent(href.substring(1));
                            bookmarks.add(bookmark);
                            append("<a href=\"" + href + "\">");
                        } else {
                            if (href.startsWith("/")) {
                                append("<a href=\"" + baseDomain + href + "\">");
                            } else {
                                append("<a href=\"" + href + "\">");
                            }
                        }
                    }
                    if (node.hasAttr("name")) {
                        initBookmark(node.attr("name"));
                    }
                } else if (nodeName.equals("dt")) {
                    append("  ");
                } else if (StringUtil.in(nodeName, "span", "p", "div", "i", "b", "h1", "h2", "h3", "h4", "h5", "h6", "strong", "em", "small", "del", "ins", "sup")) {
                    StringBuilder attrs = new StringBuilder();
                    for (Attribute attribute : node.attributes()) {
                        attrs.append(" ");
                        attrs.append(attribute.getKey());
                        attrs.append("=");
                        attrs.append("\"");
                        attrs.append(attribute.getValue());
                        attrs.append("\"");
                    }
                    attrs.append(" ");
                    if (StringUtil.in(nodeName, "p", "div")) {
                        append("<" + "span" + attrs + ">");
                    } else {
                        append("<" + nodeName + attrs + ">");
                    }
                } else if (nodeName.equalsIgnoreCase("br")) {
                    Node parent = node.parent();
                    if (parent == null || getParentOrNull(node, "span", "p", "div") == null || parent.nodeName().equalsIgnoreCase("body")) {
                        append("\n");
                    } else if (!StringUtil.in(parent.nodeName(), "h1", "h2", "h3", "h4", "h5", "h6")) {
                        append("<" + nodeName + ">");
                    }
                } else if (StringUtil.in(nodeName, "tr", "ul", "dd")) {
                    append("\n");
                } else if (nodeName.equals("img")) {
                    append(getNodeHtml(node));
                } else if (nodeName.equals("a")) {
                    Element a = (Element) node;
                    if (a.hasAttr("name")) {
                        initBookmark(a.attr("name"));
                    }
                } else if (nodeName.equals("li")) {
                    Node parent = node.parent();
                    for (int i = 0; i < getParentCount(node, "ul", "ol") - 1; i++) {
                        append("\t");
                    }
                    if (parent.nodeName().equalsIgnoreCase("ul")) {
                        append("\u25CF");
                    }
                    if (parent.nodeName().equalsIgnoreCase("ol")) {
                        for (int i = 0; i < parent.childNodeSize(); i++) {
                            if (parent.childNode(i) == node) {
                                append(i + ".");
                            }
                        }
                    }
                }
            }

            private void initBookmark(String name) {
                for (Bookmark bookmark : bookmarks) {
                    if (bookmark.getIndent().equals(name) && bookmark.getIndentIndex() <= 0) {
                        append("");
                        bookmark.setIndentIndex(indents.size());
                    }
                }
            }


            // hit when all of the node's children (if any) have been visited
            public void tail(Node node, int depth) {
                String nodeName = node.nodeName();
                if (StringUtil.in(nodeName, "i", "a", "b", "h1", "h2", "h3", "h4", "h5", "h6", "p", "div", "span", "strong", "em", "small", "del", "ins", "sup")) {
                    if (StringUtil.in(nodeName, "p", "div")) {
                        append("</span>");
                    } else {
                        append("</" + nodeName + ">");
                    }
                }
                if (StringUtil.in(nodeName, "dt", "p", "div", "li", "ul"))
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

            private int getParentCount(Node node, String... tags) {
                int count = 0;
                Node parentNode = node;
                while ((parentNode = getParentOrNull(parentNode, tags)) != null) {
                    count++;
                }
                return count;
            }
        }
    }

    private static final String WORK_SEND_RATE = "/cgi-bin/votecounter";

    public enum RateParams {
        FILE, DIR, BALL, COOK, VOTE, COOK_CHECK, OK;
    }

    public static boolean sendRate(Work work, int rate) {
        try {
            String link = Constants.Net.BASE_DOMAIN + WORK_SEND_RATE;
            String wlink = work.getLinkWithoutSuffix();
            String alink = work.getAuthor().getLink();
            String voteCookie = getVoteCookie();
            Request request = new Request(link)
                    .setMethod(Request.Method.POST)
                    .addHeader(Header.ACCEPT, ACCEPT_VALUE)
                    .addHeader(Header.USER_AGENT, USER_AGENT)
                    .addHeader(Header.HOST, Constants.Net.BASE_HOST)
                    .addHeader(Header.REFERER, work.getFullLink())
                    .addHeader(Header.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .addHeader(Header.UPGRADE_INSECURE_REQUESTS, "1")
                    .setEncoding("CP1251")
                    .addParam(RateParams.FILE, wlink.substring(wlink.lastIndexOf('/') + 1))
                    .addParam(RateParams.DIR, alink.substring(1, alink.length() - 1))
                    .addParam(RateParams.BALL, String.valueOf(rate))
                    .addParam(RateParams.OK, "OK");
            if (voteCookie != null) {
                request.addCookie(RateParams.VOTE, voteCookie);
            }
            Response response = request.execute();
            if (voteCookie == null) {
                if (response.getHeaders().containsKey(Header.SET_COOKIE)) {
                    voteCookie = HTTPExecutor.parseParamFromHeader(response.getHeaders().get(Header.SET_COOKIE).get(0), RateParams.VOTE);
                    setVoteCookie(voteCookie);
                }
                Element element = Jsoup.parse(response.getRawContent()).head().select("meta[http-equiv=refresh]").first();
                if (element != null) {
                    String content = element.attr("content");
                    Request redirectedRequest = new Request(Constants.Net.BASE_DOMAIN + content.substring(content.indexOf("=") + 1), true);
                    redirectedRequest.getHeaders().putAll(request.getHeaders());
                    redirectedRequest.setFollowRedirect(true);
                    if (voteCookie != null) {
                        redirectedRequest.addCookie(RateParams.VOTE, voteCookie);
                    } else {
                        setVoteCookie(request.getParam(RateParams.COOK));
                        redirectedRequest.addCookie(RateParams.VOTE, getVoteCookie());
                    }
                    redirectedRequest.execute();
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
