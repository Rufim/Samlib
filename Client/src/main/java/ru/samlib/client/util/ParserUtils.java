package ru.samlib.client.util;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.net.CachedResponse;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Rufim on 04.07.2015.
 */
public class ParserUtils {

    protected static final String TAG = ParserUtils.class.getSimpleName();

    public static String trim(String string) {
        return string.replaceAll("^\\s+|\\s+$", "");
    }

    public static Date parseData(String text) {
        Calendar calendar = Calendar.getInstance();
        if (text.contains(":")) {
            String[] time = text.split(":");
            int hours = Integer.parseInt(time[0]);
            calendar.set(Calendar.MINUTE, Integer.parseInt(time[1]));
            calendar.set(Calendar.HOUR_OF_DAY, hours);
            return calendar.getTime();
        } else if (text.contains("/")) {
            String[] date = text.split("/");
            if (date.length == 3) {
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(date[1]) - 1);
                calendar.set(Calendar.YEAR, Integer.parseInt(date[2]));
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                return calendar.getTime();
            } else if (date.length == 2) {
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(date[1]) - 1);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                return calendar.getTime();
            }
        }
        return null;
    }

    public static Work parseWork(CachedResponse file, Work work) {
        if(work == null || work.getAuthor() == null) {
            work = new Work(file.getRequest().getBaseUrl().getPath());
        }
        String[] parts = Splitter.extractLines(file, true,
                new Splitter().addEnd("Первый блок ссылок"),
                new Splitter("Блок описания произведения", "Кнопка вызова Лингвоанализатора"),
                new Splitter().addStart("Блочек голосования").addStart("<!-------.*").addEnd("Собственно произведение"),
                new Splitter().addEnd("<!-------.*"));
        Document head = Jsoup.parseBodyFragment(parts[0]);
        if(work.getAuthor().getFullName() == null) {
            work.getAuthor().setFullName(head.select("div > h3").first().ownText());
        }
        work.setTitle(head.select("center > h2").text());
        Elements lis = Jsoup.parseBodyFragment(parts[1]).select("ul > li");
        int index = 2;
        if(lis.get(0).text().contains("Copyright"))  {
            index--;
            work.setHasComments(false);
        }
        String[] data = Splitter.extractString(lis.get(index++).text(), true,
                new Splitter("Размещен: ", ","),
                new Splitter("изменен: ", "\\."),
                new Splitter(" ", "k"));
        work.setCreateDate(parseData(data[0]));
        work.setUpdateDate(parseData(data[1]));
        work.setSize(Integer.parseInt(data[2]));
        if(lis.size() > index) {
            String typeGenre[] = lis.get(index++).text().split(":");
            work.setType(Type.parseType(typeGenre[0]));
            if (typeGenre.length > 1) {
                work.setGenres(typeGenre[1]);
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
        if(parts[2].contains("Аннотация")) {
            work.getAnnotationBlocks().clear();
            work.addAnnotation(parts[2].substring(parts[2].indexOf("<i>"), parts[2].lastIndexOf("</i>") + 4));
        }
        work.setRawContent(parts[3]);
        work.setParsedContent(null);
        return work;
    }

    public static String cleanupHtml(Element el) {
        //TODO: FIX HtmlView to support inputs and tables
        //
        Elements table = el.select("table");  // tablets not supported
        if (table.select("input").size() > 0) {
            table.remove();
        } else {
            if (table.hasAttr("border") && table.select("tr").size() > 2) {
                table.wrap("<hr><hr>");
            }
            table.attr("border", "0");
        }
        //Cleanup
        for (Element elem : el.select("*")) {
            if (!elem.hasText() && elem.select("img").size() < 1) {
                if (elem.parent() != null) elem.remove();
            }
        }
        el.select("input").remove(); // inputs not supported

        return el.html().replaceAll("\\s<br>\\s\\n", "").replace("\n", "");
    }

    public static void parseType(Element el, Work work) {
        Elements info = el.select("b");
        if (info.size() > 0) {
            String[] rate = info.get(0).text().split("\\*");
            work.setRate(new BigDecimal(rate[0]));
            work.setKudoed(Integer.parseInt(rate[1]));
        }
        String ownText = el.ownText();
        if (ownText.contains("\"")) {
            String type = ownText.split("\"")[1];
            if (!type.isEmpty()) {
                Type tp = Type.parseType(type);
                if (tp == Type.OTHER) {
                    Category category = new Category();
                    category.setTitle(type);
                    work.setCategory(category);
                } else {
                    work.setType(tp);
                }

            }
        }
        work.setGenres(ownText.substring(ownText.lastIndexOf("\u00A0") + 1).trim());
    }

    public static Work parseWork(Element element) {
        Work work = new Work();
        Element li;
        if (element.tagName().equals("li")) {
            li = element;
        } else {
            li = element.select("li").first();
            if (li == null) {
                li = element;
                Log.w(TAG, "li not found: suspect malformed row - " + element.text());
            }
        }
        for (Element el : li.children()) {
            switch (el.nodeName()) {
                case "font":
                    work.setState(New.parseNew(el.attr("color")));
                    break;
                case "a":
                    String link = el.attr("href");
                    if (link.contains(".shtml")) {
                        work.setLink(link);
                        work.setTitle(el.text());
                    }
                    break;
                case "b":
                    String text = el.text();
                    if (text.matches("^\\d+k$")) {
                        work.setSize(Integer.parseInt(text.replace("k", "")));
                    }
                    break;
                case "small":
                    ParserUtils.parseType(el, work);
                    break;
                case "dd":
                    if (el.select("a[href^=/img]").size() > 0) {
                        work.setHasIllustration(true);
                        break;
                    } else {
                        if (el.hasText() || el.select("img").size() > 0) {
                            work.addAnnotation("<p>" + ParserUtils.cleanupHtml(el) + "</p>");
                        }
                    }
                    break;
            }
        }

        return work;
    }


    public static void parseInfoTableRow(Author author, Element element) {
        String content = element.ownText();
        String[] split = null;
        switch (element.select("b").text()) {
            case "WWW:":
                Element a = element.select("a").first();
                author.setSite(new Link(a.ownText(), a.attr("href")));
                break;
            case "Aдpeс:":
                author.setEmail(element.select("u").text());
                break;
            case "Родился:":
                author.setDateBirth(ParserUtils.parseData(content));
                break;
            case "Живет:":
                author.setAddress(content);
                break;
            case "Обновлялось:":
                author.setLastUpdateDate(ParserUtils.parseData(content));
                break;
            case "Объем:":
                split = content.split("/");
                author.setSize(Integer.parseInt(split[0].replace("k", "")));
                author.setWorkCount(Integer.parseInt(split[1]));
                break;
            case "Рейтинг:":
                split = content.split("\\*");
                author.setRate(new BigDecimal(split[0]));
                author.setKudoed(Integer.parseInt(split[1]));
                break;
            case "Посетителей за год:":
                author.setViews(Integer.parseInt(content));
                break;
            case "Friends/Friend Of:":
                split = content.split("/");
                author.setFriends(Integer.parseInt(split[0]));
                author.setFriendsOf(Integer.parseInt(split[1]));
                break;
            case "Friend Of:":
                author.setFriends(Integer.parseInt(content));
                break;
            case "Friends:":
                author.setFriendsOf(Integer.parseInt(content));
                break;
            default:
                Log.e(TAG, "Unknown element parsed: " + element.text());
        }
    }

}
