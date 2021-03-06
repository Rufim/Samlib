package ru.samlib.client.util;

import android.util.Log;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.domain.entity.*;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Rufim on 04.07.2015.
 */
public class ParserUtils {

    protected static final String TAG = ParserUtils.class.getSimpleName();

    public static String cleanupHtml(Element el) {
        return cleanupHtml(el.children());
    }

    public static String cleanupHtml(Elements el) {
        //Cleanup
        for (Element elem : el.select("*")) {
            if (elem.select("img").size() < 1 && !elem.tagName().equals("input")) {
                if (!elem.hasText()) {
                    if (elem.parent() != null) elem.remove();
                }
            }
        }
        /*Elements table = el.select("table");
        if (table.select("input").size() > 0) {
            table.remove();
        } else {
            if (table.hasAttr("border") && table.select("tr").size() > 2) {
                table.wrap("<hr><hr>");
            }
            table.attr("border", "0");
        } */
        el.select("input").remove(); // inputs not supported

        return el.html().replaceAll("\\s<br>\\s\\n", "").replace("\n", "");
    }

    public static void parseType(Element el, Work work) {
        Elements info = el.select("b");
        if (info.size() > 0) {
            String text = TextUtils.trim(info.text());
            String size = null;
            String rate = null;
            if (text.matches("\\d+k \\d+\\.\\d+\\*\\d+")) {
                String[] texts = text.split(" ");
                size = texts[0];
                rate = texts[1];
            } else if (text.matches("\\d+k")) {
                size = text;
            } else if (text.matches("\\d+\\.\\d+\\*\\d+")) {
                rate = text;
            }

            if (size != null) {
                work.setSize(Integer.parseInt(size.replace("k", "")));
            }

            if (rate != null) {
                String[] rates = rate.split("\\*");
                work.setRate(new BigDecimal(rates[0]));
                work.setVotes(Integer.parseInt(rates[1]));
            }
        }
        String ownText = TextUtils.trim(el.ownText().replace("Оценка:", ""));
        String first = ownText.split(" ")[0];
        if (!first.contains(",") && Genre.parseGenre(first) == null) {
            String type = first.replace("\"", "");
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
            ownText = ownText.replace(first, "");
        }
        work.setHasComments(el.text().contains("Комментарии"));
        work.setGenresAsString(ownText);
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
                        work.setSmartLink(link);
                        work.setTitle(el.text());
                    } else if(work.getAuthor() == null) {
                        work.setAuthor(new Author(link));
                        work.getAuthor().setFullName(el.text());
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
                author.setAuthorSiteUrl(a.attr("href"));
                break;
            case "Aдpeс:":
                author.setEmail(element.select("u").text());
                break;
            case "Родился:":
                author.setDateBirth(parseData(content));
                break;
            case "Живет:":
                author.setAddress(content);
                break;
            case "Обновлялось:":
                Date date = parseData(content);
                if(author.getLastUpdateDate() == null || (date != null && author.getLastUpdateDate().before(date))) {
                    author.setLastUpdateDate(date);
                }
                break;
            case "Объем:":
                split = content.split("/");
                author.setSize(Integer.parseInt(split[0].replace("k", "")));
                author.setWorkCount(Integer.parseInt(split[1]));
                break;
            case "Рейтинг:":
                split = content.split("\\*");
                author.setRate(new BigDecimal(split[0]));
                author.setVotes(Integer.parseInt(split[1]));
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
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar.getTime();
            } else if (date.length == 2) {
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(date[1]) - 1);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar.getTime();
            }
        }
        return null;
    }


}
