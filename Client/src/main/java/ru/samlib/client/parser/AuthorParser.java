package ru.samlib.client.parser;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.Splitter;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.net.CachedResponse;
import ru.samlib.client.util.ParserUtils;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.util.Scanner;

/**
 * Created by Rufim on 01.07.2015.
 */
public class AuthorParser extends Parser {

    private static final String TAG = AuthorParser.class.getSimpleName();

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
                            ParserUtils.parseInfoTableRow(author, element);
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
                        author.addRecommendation(ParserUtils.parseWork(workEl));
                    }
                }
            }
            // body - Partitions with Works
            if (parts.length > 2 && !parts[2].isEmpty()) {
                Scanner scanner = new Scanner(parts[2]);
                Category newCategory = null;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    Elements a;
                    if (!line.isEmpty()) {
                        try {
                            Document lineDoc = Jsoup.parseBodyFragment(line);
                            if (line.contains("<h3>")) {
                                newCategory = new Category();
                                newCategory.setTitle(lineDoc.select("h3").text());
                                author.addCategory(newCategory);
                            }
                            if (line.startsWith("</small>")) {
                                newCategory = new Category();
                                a = lineDoc.select("a");
                                newCategory.setTitle(a.text());
                                if (line.contains("href=index")) {
                                    newCategory.setLink(author.getLink() + "/" + a.attr("href"));
                                    newCategory.setAuthor(author);
                                } else if (line.contains("href=/type")) {
                                    newCategory.setType(Type.parseType(a.attr("href")));
                                }
                                scanner.nextLine();
                                line = scanner.nextLine();
                                if(line.startsWith("<font")) {
                                    String annotation = line;
                                    line = scanner.nextLine();
                                    if(line.startsWith("<dd>")) {
                                        newCategory.setAnnotation(ParserUtils.cleanupHtml(Jsoup.parseBodyFragment(annotation + line.substring(line.indexOf("<dd>")))));
                                        line = scanner.nextLine();
                                    } else {
                                        newCategory.setAnnotation(ParserUtils.cleanupHtml(Jsoup.parseBodyFragment(annotation)));
                                    }
                                }
                                lineDoc = Jsoup.parseBodyFragment(line);
                                author.addCategory(newCategory);
                            }
                            if (line.contains("<DL>")) {
                                Elements dl = lineDoc.select("DL");
                                if (line.contains("TYPE=square")) {
                                    a = dl.select("a");
                                    newCategory.addLink(new Link(a.text(), a.attr("href")));
                                    continue;
                                }
                                Work work = ParserUtils.parseWork(dl.first());
                                work.setAuthor(author);
                                work.setCategoryTitle(newCategory.getTitle());
                                if (work.validate()) {
                                    if (newCategory == null) {
                                        author.addRootLink(work);
                                    } else {
                                        newCategory.addLink(work);
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
            Log.e(TAG, "Author " + author.getTitle() + " parsed");
        } catch (Exception | Error e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return author;
    }

}
