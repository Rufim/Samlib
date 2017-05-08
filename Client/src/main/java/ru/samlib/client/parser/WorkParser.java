package ru.samlib.client.parser;

import android.accounts.NetworkErrorException;
import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import ru.kazantsev.template.net.CachedResponse;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Bookmark;
import ru.samlib.client.domain.entity.Category;
import ru.samlib.client.domain.entity.Type;
import ru.samlib.client.domain.entity.Work;

import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.util.HtmlToPlainText;
import ru.samlib.client.util.JsoupUtils;
import ru.samlib.client.util.ParserUtils;
import ru.kazantsev.template.util.SystemUtils;
import ru.kazantsev.template.util.TextUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by Rufim on 04.07.2015.
 */
public class WorkParser extends Parser {


    private static final int MAX_INDENT_SIZE = 700;
    private Work work;

    public WorkParser(Work work) throws MalformedURLException {
        setPath(work.getLink());
        this.work = work;
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
        if (rawContent == null) {
            return work;
        }
        try {
            work = parseWork(rawContent, work);
            Log.e(TAG, "Work parsed using url " + request.getBaseUrl());
            work.setChanged(false);
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
            work = new Work(file.getRequest().getBaseUrl().getPath().replace("//","/"));
        }
        String[] parts;
        if(!work.getLink().matches(work.getAuthor().getLink() +"/rating\\d.shtml")) {
             parts = TextUtils.Splitter.extractLines(file, file.getEncoding(), true,
                    new TextUtils.Splitter().addEnd("Первый блок ссылок"),
                    new TextUtils.Splitter("Блок описания произведения", "Кнопка вызова Лингвоанализатора"),
                    new TextUtils.Splitter().addStart("Блочек голосования").addStart("<!-------.*").addEnd("Собственно произведение"),
                    new TextUtils.Splitter().addEnd("<!-------.*"));
        } else {
            parts = TextUtils.Splitter.extractLines(file, file.getEncoding(), true,
                    new TextUtils.Splitter().addEnd("Первый блок ссылок"),
                    new TextUtils.Splitter("<table width=90% border=0 cellpadding=0 cellspacing=0><tr>", "</tr></table>"),
                    new TextUtils.Splitter("<hr align=\"CENTER\" size=\"2\" noshade>", "<hr align=\"CENTER\" size=\"2\" noshade>"));
        }
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
                work.setUpdateDate(ParserUtils.parseData(data[0]));
                if(!data[1].contains("Статистика")) {
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
                    if (work.getCategory() == null || !work.getCategory().equals(category)) {
                        work.setCategory(category);
                    }
                }
            }


        }
        if(parts.length == 3) {
            work.setRawContent(parts[2]);
        } else if(parts.length == 4){
            if (parts[2].contains("Аннотация")) {
                work.setAnnotationBlocks(Arrays.asList(ParserUtils.cleanupHtml(Jsoup.parseBodyFragment(parts[2]).select("i"))));
            }
            if (parts[3].contains("<!--Section Begins-->")) {
                work.setRawContent(TextUtils.Splitter.extractLines(file, file.getEncoding(), true,
                        new TextUtils.Splitter("<!--Section Begins-->", "<!--Section Ends-->"))[0]);
            } else {
                work.setRawContent(parts[3]);
            }
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
        document.setBaseUri(Constants.Net.BASE_DOMAIN);
        document.outputSettings().prettyPrint(false);
        List<Node> rootNodes = new ArrayList<>();
        //Element body = replaceTables(document.body());
        Elements rootElements = document.body().select("> *");
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
                elements = document.body().children();
            }
        }
        indents.clear();
        HtmlToPlainText plainText = new HtmlToPlainText();
        for (Element element : elements) {
            String text = plainText.getPlainText(element);
            List<String> indentPart = Arrays.asList(TextUtils.splitByNewline(text));
            //TODO: optimise perfomance and use
            for (String part : indentPart) {
                //addIndent(indents, part);
                if (!part.contains("<img")) {
                    indents.add("&emsp;" + part);
                } else {
                    indents.add(part);
                }
            }
        }
        elements.clear();
        rootElements.clear();
        Pattern pattern = Pattern.compile("^((Пролог)|(Эпилог)|(Интерлюдия)|(Приложение)|(Глава)|(Часть)|(\\*{3,})|(\\d)).*$",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
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

    private static void addIndent(List<String> indents, String indent) {
        int maxOverflow = (int) (MAX_INDENT_SIZE + (MAX_INDENT_SIZE * 0.25));
        if(indent.length() > maxOverflow) {
            for (int index = 0; indent.length() > index + MAX_INDENT_SIZE;) {
                if(indent.length() > index + maxOverflow) {
                    StringBuilder accum = new StringBuilder(indent.substring(index, index + MAX_INDENT_SIZE));
                    index = index + MAX_INDENT_SIZE;
                    int size = index;
                    while (index < indent.length() && indent.charAt(size) != ' ') {
                        size++;
                    }
                    if(size > 0) {
                        accum.append(indent.substring(index, size));
                        index += size;
                    }
                    indents.add(accum.toString());
                }
                if(indent.length() <= index + MAX_INDENT_SIZE) {
                    indents.add(indent.substring(index));
                    break;
                }
            }
        } else {
            indents.add(indent);
        }
    }

    private static boolean isEmpty(String sequence) {
        if(sequence.equals("\n")) return false;
        else return TextUtils.isEmpty(TextUtils.trim(sequence));
    }

}
