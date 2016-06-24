package ru.samlib.client.parser;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.entity.Category;
import ru.samlib.client.domain.entity.Bookmark;
import ru.samlib.client.domain.entity.Type;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.net.CachedResponse;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.util.*;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Rufim on 04.07.2015.
 */
public class WorkParser extends Parser {


    public enum INDENT_TAGS {
        DD, DIV, P, BR, PRE;
    }

    private Work work;

    public WorkParser(Work work) throws MalformedURLException {
        setPath(work.getLink());
        this.work = work;
    }

    public WorkParser(String workLink) throws MalformedURLException {
        setPath(workLink);
        this.work = new Work(workLink);
    }

    public Work parse(boolean fullDownload, boolean processChapters) {
        CachedResponse rawContent = null;
        if (work.getRawContent() == null && !fullDownload) {
            rawContent = HtmlClient.executeRequest(request, MIN_BODY_SIZE);
        } else {
            rawContent = HtmlClient.executeRequest(request);
        }
        if (rawContent == null) {
            return work;
        }
        try {
            Date oldUpdateDate = work.getUpdateDate();
            work.setChanged(true);
            work = parseWork(rawContent, work);
            Log.e(TAG, "Work parsed using url " + request.getBaseUrl());
            if (oldUpdateDate != null
                    && oldUpdateDate.getTime() == work.getUpdateDate().getTime()
                    && work.getIndents().size() > 0) {
                work.setChanged(false);
                return work;
            }
            if(processChapters) {
                processChapters(work);
            }
        } catch (Exception ex) {
            work.setParsed(false);
            Log.e(TAG, "Work NOT parsed using url " + request.getBaseUrl(), ex);
        }
        return work;
    }

    public static Work parseWork(CachedResponse file, Work work) {
        if (work == null) {
            work = new Work(file.getRequest().getBaseUrl().getPath());
        }
        String[] parts = TextUtils.Splitter.extractLines(file, file.getEncoding(), true,
                new TextUtils.Splitter().addEnd("Первый блок ссылок"),
                new TextUtils.Splitter("Блок описания произведения", "Кнопка вызова Лингвоанализатора"),
                new TextUtils.Splitter().addStart("Блочек голосования").addStart("<!-------.*").addEnd("Собственно произведение"),
                new TextUtils.Splitter().addEnd("<!-------.*"));
        if (parts.length == 0) {
            return work;
        }
        Document head = Jsoup.parseBodyFragment(parts[0]);
        if (work.getAuthor().getFullName() == null) {
            work.getAuthor().setFullName(head.select("div > h3").first().ownText());
        }
        work.setTitle(head.select("center > h2").text());
        Elements lis = Jsoup.parseBodyFragment(parts[1]).select("li");
        int index = 2;
        if (lis.get(0).text().contains("Copyright")) {
            index--;
            work.setHasComments(false);
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
                work.setUpdateDate(TextUtils.parseData(data[0]));
                work.setSize(Integer.parseInt(data[1]));
            }
            if (data.length == 3) {
                work.setCreateDate(TextUtils.parseData(data[0]));
                work.setUpdateDate(TextUtils.parseData(data[1]));
                work.setSize(Integer.parseInt(data[2]));
            }
        }
        if (lis.size() > index) {
            String typeGenre[] = lis.get(index++).text().split(":");
            work.setType(Type.parseType(typeGenre[0]));
            if (typeGenre.length > 1) {
                work.setGenresAsString(typeGenre[1]);
            }
            for (int i = index; i < lis.size(); i++) {
                String text = lis.get(i).text();
                if (text.contains("Иллюстрации")) {
                    work.setHasIllustration(true);
                } else if (text.contains("Скачать")) {
                    break;
                } else {
                    Category category = new Category();
                    category.setTitle(text);
                    category.setLink(lis.attr("href"));
                    category.setAuthor(work.getAuthor());
                    work.setCategory(category);
                }
            }
        }
        if (parts[2].contains("Аннотация")) {
            work.getAnnotationBlocks().clear();
            work.addAnnotation(ParserUtils.cleanupHtml(Jsoup.parseBodyFragment(parts[2]).select("i").first()));
        }
        if (parts[3].contains("<!--Section Begins-->")) {
            work.setRawContent(TextUtils.Splitter.extractLines(file, file.getEncoding(), true,
                    new TextUtils.Splitter("<!--Section Begins-->", "<!--Section Ends-->"))[0]);
        } else {
            work.setRawContent(parts[3]);
        }
     /*   if(work.getRawContent() != null) {
            String oldMd5 = work.getMd5();
            work.setMd5(TextUtils.calculateMD5(work.getRawContent(), file.getRequest().getEncoding()));
            if(!work.getMd5().equals(oldMd5)) work.setChanged(true);
        } */
        work.setCachedResponse(file);
        return work;
    }

    public static void processChapters(Work work) {
        List<String> indents = work.getIndents();
        List<Bookmark> bookmarks = work.getAutoBookmarks();
        Document document = Jsoup.parseBodyFragment(work.getRawContent());
        document.outputSettings().prettyPrint(false);
        List<Node> rootNodes = new ArrayList<>();
        Element body = replaceTables(document.body());
        Elements rootElements = body.select("> *");
        Elements elements = rootElements.select("xxx7");
        if (elements.size() > 0) {
            for (Element element : elements) {
                rootNodes.addAll(element.childNodes());
            }
        } else {
            elements = rootElements.select("div[align=justify]");
            if (elements.size() > 0) {
                for (Element element : elements) {
                    rootNodes.addAll(element.childNodes());
                }
            } else {
                rootNodes = body.childNodes();
            }
        }
        if (rootNodes.size() > 0
                && rootNodes.get(0) instanceof Element
                && "font".equals(((Element) rootNodes.get(0)).tagName())
                && rootNodes.size() == 1) {
            rootNodes = rootNodes.get(0).childNodes();
        }
        elements.clear();
        rootElements.clear();
        indents.clear();
        Pattern pattern = Pattern.compile("^((Пролог)|(Эпилог)|(Интерлюдия)|(Приложение)|(Глава)|(Часть)|(\\*{3,})|(\\d)).*$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        indents.addAll(Arrays.asList(JsoupUtils.ownText(document.body()).split("\\n")));
        for (int i = 0; i < rootNodes.size(); i++) {
            Node node = rootNodes.get(i);
            if (node instanceof Element) {
                Element el = (Element) node;
                if (SystemUtils.parseEnum(el.tagName().toUpperCase(), INDENT_TAGS.class) != null) {
                    if (!el.text().isEmpty()) {
                        indents.add(TextUtils.linkifyHtml(el.outerHtml()));
                    }
                } else {
                    if (indents.isEmpty()) {
                        indents.add(el.outerHtml());
                    } else {
                        indents.set(indents.size() - 1, indents.get(indents.size() - 1) + el.outerHtml());
                    }
                }
            } else if (node instanceof TextNode) {
                indents.add(TextUtils.linkifyHtml(((TextNode) node).text()));
            }
        }
        bookmarks.clear();
        bookmarks.add(new Bookmark("Начало"));
        for (int i = 0; i < indents.size(); i++) {
            if(indents.get(i).length() > 3 && indents.get(i).length() < 10) {
                String text = JsoupUtils.cleanHtml(indents.get(i));
                if (pattern.matcher(TextUtils.trim(text)).find()) {
                    Bookmark newBookmark = new Bookmark(text);
                    newBookmark.setPercent(((double) i) / indents.size());
                    newBookmark.setIndentIndex(i);
                    bookmarks.add(newBookmark);
                }
            }
        }
        work.setParsed(true);
    }


    public static Element replaceTables(Element el) {
        Elements tables = el.select("table");
        Elements tds = el.select("td");
        Elements trs = el.select("tr");
        tables.unwrap();
        tds.tagName("dd");
        trs.unwrap();
        return el;
    }

}
