package ru.samlib.client.parser;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.entity.Chapter;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.net.CachedResponse;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.util.JsoupUtils;
import ru.samlib.client.util.ParserUtils;
import ru.samlib.client.util.SystemUtils;
import ru.samlib.client.util.TextUtils;

import java.net.MalformedURLException;
import java.util.Arrays;
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

    public Work parse(boolean fullDownload) {
        CachedResponse rawContent = null;
        if (work.getRawContent() == null && !fullDownload) {
            rawContent = HtmlClient.executeRequest(request, MIN_BODY_SIZE);
        } else {
            rawContent = HtmlClient.executeRequest(request);
        }
        if(rawContent == null) {
            return work;
        }
        try {
            if (rawContent.isDownloadOver) {
                work.setParsed(true);
            }
            work = ParserUtils.parseWork(rawContent, work);
            Log.e(TAG, "Work parsed using url " + request.getBaseUrl());
            processChapters(work);
        } catch (Exception ex)  {
            work.setParsed(false);
            Log.e(TAG, "Work NOT parsed using url " + request.getBaseUrl(), ex);
        }
        return work;
    }

    public void processChapters(Work work) {
        List<String> indents = work.getIndents();
        List<Chapter> chapters = work.getChapters();
        Document document = Jsoup.parseBodyFragment(work.getRawContent());
        document.outputSettings().prettyPrint(false);
        Elements rootElements = replaceTables(document.body()).select("> *");
        Elements elements = rootElements.select("xxx7");
        if (elements.size() > 0) {
            rootElements = elements.first().select("> *");
        } else {
            elements = rootElements.select("div[align=justify]");
            if (elements.size() > 0) {
                rootElements = elements.first().select("> *");
            }
        }
        while (rootElements.size() > 0
                && rootElements.first().tagName() == "font"
                && rootElements.size() == 1) {
            rootElements = rootElements.first().select("> *");
        }
        work.setRootElements(rootElements);
        chapters.clear();
        indents.clear();
        Chapter currentChapter = new Chapter("Начало");
        Pattern pattern = Pattern.compile("^((Пролог)|(Эпилог)|(Интерлюдия)|(Приложение)|(Глава)|(Часть)|(\\*{3,})|(\\d)).*$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        indents.addAll(Arrays.asList(JsoupUtils.ownText(document.body()).split("\\n")));
        for (int i = 0; i < rootElements.size(); i++) {
            Element el = rootElements.get(i);
            if (SystemUtils.parseEnum(el.tagName().toUpperCase(), INDENT_TAGS.class) != null) {
                indents.add(TextUtils.linkifyHtml(el.outerHtml()));
            } else {
                if (indents.isEmpty()) {
                    indents.add(el.outerHtml());
                } else {
                    indents.set(indents.size() - 1, indents.get(indents.size() - 1) + el.outerHtml());
                }
            }
        }
        for (int i = 0; i < indents.size(); i++) {
            String text = JsoupUtils.cleanHtml(indents.get(i));
            if (rootElements.size() > i) {
                if (pattern.matcher(TextUtils.trim(text)).find()) {
                    Chapter newChapter = new Chapter(text);
                    chapters.add(currentChapter);
                    newChapter.setPercent(((float) i) / rootElements.size());
                    newChapter.setIndex(i);
                    currentChapter = newChapter;
                }
            }
        }
        chapters.add(currentChapter);
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
