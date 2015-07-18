package ru.samlib.client.parser;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.Splitter;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.net.CachedResponse;
import ru.samlib.client.util.ParserUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.Scanner;

/**
 * Created by Rufim on 01.07.2015.
 */
public class AuthorParser extends Parser {

    private Author author;

    public AuthorParser(Author author) throws MalformedURLException {
        setPath(author.getLink());
        this.author = author;
    }

    public AuthorParser(String authorLink) throws MalformedURLException {
        setPath(authorLink);
        author = new Author();
        author.setLink(authorLink);
    }

    public Author parse() {
        try {
            Document headDoc;
            Elements elements;
            CachedResponse rawFile = HtmlClient.executeRequest(request);
            String[] parts = ParserUtils.extractLines(rawFile, false,
                    new Splitter().addEnd("Первый блок ссылок"),
                    new Splitter("Блок шапки", "Блок управления разделом"),
                    new Splitter("Блок ссылок на произведения", "Подножие"));
            // head - Author Info
            if(parts.length > 0) {
                String title = parts[0];
                String[] titles = Jsoup.parseBodyFragment(parts[0]).select("center > h3").text().split(":");
                author.setFullName(titles[0]);
                author.setAnnotation(titles[1].trim());
            }
            // Author info
            if (parts.length > 1) {
                String head = parts[1];
                headDoc = Jsoup.parseBodyFragment(head);
                if(headDoc == null) return author;
                elements = headDoc.select("table[bgcolor=#e0e0e0] li");
                if (elements.size() > 0) {
                    for (Element element : elements) {
                        try {
                            parseInfoTableRow(element);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error in element parsing: " + element.text());
                        }
                    }
                }
                author.setHasAvatar(head.contains("src=.photo2.jpg"));
                author.setHasAbout(head.contains("href=about.shtml"));
                if (head.contains("Об авторе")) {
                    author.setAbout(
                            Jsoup.parseBodyFragment(ParserUtils
                                    .extractLines(new ByteArrayInputStream(head.getBytes(request.getEncoding())),
                                            request.getEncoding(),
                                            true,
                                            new Splitter("Об авторе", "(Аннотация к разделу)|(Начните знакомство с)"))[0]).text());
                }
                if (head.contains("Аннотация к разделу")) {
                    author.setSectionAnnotation(ParserUtils.extractString(headDoc.text(),
                            true,
                            new Splitter().addStart("Аннотация к разделу: "))[0]);
                }
                if (head.contains("Начните знакомство с")) {
                    String[] rec = ParserUtils.extractString(head, true, new Splitter("<b>Начните знакомство с</b>:", "\\n"));
                    Document recDoc = Jsoup.parseBodyFragment(rec[0]);
                    for (Element workEl : recDoc.select("li")) {
                        author.addRecommendation(parseWork(workEl));
                    }
                }
            }
            // body - Partitions with Works
            if (parts.length > 2 && !parts[2].isEmpty()) {
                Scanner scanner = new Scanner(parts[2]);
                Section newSection = null;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    Elements a;
                    if (!line.isEmpty()) {
                        try {
                            Document lineDoc = Jsoup.parseBodyFragment(line);
                            if (line.contains("<h3>")) {
                                newSection = new Section();
                                newSection.setTitle(lineDoc.select("h3").text());
                                author.addSection(newSection);
                            }
                            if (line.startsWith("</small>")) {
                                newSection = new Section();
                                a = lineDoc.select("a");
                                newSection.setTitle(a.text());
                                if (line.contains("href=index")) {
                                    newSection.setLink(a.attr("href"));
                                } else if (line.contains("href=/type")) {
                                    newSection.setType(Type.parseType(a.attr("href")));
                                }
                                scanner.nextLine();
                                newSection.setAnnotation(Jsoup.parseBodyFragment(scanner.nextLine()).text());
                                author.addSection(newSection);
                            }
                            if (line.contains("<DL>")) {
                                Elements dl = lineDoc.select("DL");
                                a = dl.select("a");
                                if (line.contains("TYPE=square")) {
                                    newSection.addLink(new Link(a.text(), a.attr("href")));
                                    continue;
                                }
                                Work work = parseWork(dl.first());
                                work.setAuthor(author);
                                work.setSectionTitle(newSection.getTitle());
                                if (work.validate()) {
                                    if (newSection == null) {
                                        author.addRootLink(work);
                                    } else {
                                        newSection.addLink(work);
                                    }
                                } else {
                                    Log.e(TAG, "Invalid work: " + line);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Exeption caused on line:" + line, e);
                        }
                    }
                }
                scanner.close();
            }
            Log.e(TAG, "Author parsed");
        } catch (Exception | Error e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return author;
    }

    private void parseInfoTableRow(Element element) {
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

    private Work parseWork(Element element) {
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
                    if(text.contains("k")) {
                        work.setSize(Integer.parseInt(text.replace("k", "")));
                    }
                    break;
                case "small":
                    parseType(el, work);
                    break;
                case "dd":
                    if (el.select("a[href^=/img]").size() > 0) {
                        work.setHasIllustration(true);
                        break;
                    } else {
                        if(el.hasText() || el.select("img").size() > 0) {
                            //TODO: FIX HtmlView to support inputs and tables
                            Elements table = el.select("table");
                            if(table.select("input").size() > 0) {
                                table.remove();
                            } else {
                                table.select("td").tagName("nobr");
                                table.select("tr").tagName("p");
                                table.select("tbody").unwrap();
                                table.unwrap();
                            }
                            //TODO:More complex fix
                            el.select("img[width~=\\d{3}]").wrap("<br></br>");
                            el.select("img").select(":not([width])").wrap("<br></br>");
                            el.select("input").remove();
                            //Cleanup
                            for (Element elem: el.select("*")) {
                                if (!elem.hasText() && elem.select("img").size() < 1) {
                              //      elem.remove();
                                }
                            }
                            work.addAnnotation("<p>" + el.html().replaceAll("\\s<br>\\s\\n", "").replace("\n","") + "</p>");
                        }
                    }
                    break;
            }
        }

        return work;
    }


    public void parseType(Element el, Work work) {
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
                    work.setSectionTitle(type);
                } else {
                    work.setType(tp);
                }

            }
        }
        String[] genres = ownText.substring(ownText.lastIndexOf("\u00A0") + 1).trim().split(",");
        for (String genre : genres) {
            work.addGenre(genre);
        }

    }

}
