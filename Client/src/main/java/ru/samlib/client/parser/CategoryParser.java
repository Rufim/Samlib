package ru.samlib.client.parser;

import android.accounts.NetworkErrorException;
import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Category;
import ru.samlib.client.domain.entity.Work;
import ru.kazantsev.template.net.CachedResponse;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.util.ParserUtils;
import ru.kazantsev.template.util.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Scanner;

/**
 * Created by 0shad on 21.07.2015.
 */
public class CategoryParser extends Parser{

    private static final String TAG = CategoryParser.class.getSimpleName();

    private Category category;

    public CategoryParser(Category category) throws MalformedURLException {
        setPath(category.getLink());
        this.category = category;
    }

    public CategoryParser(String categoryLink) throws MalformedURLException {
        setPath(categoryLink);
        category = new Category();
        category.setLink(categoryLink);
    }

    public Category parse() throws IOException {
        boolean hasNotAuthor = category.getAuthor() == null;
        Author author;
        if(hasNotAuthor) {
            category.setAuthor(new Author(request.getBaseUrl().toString()));
        }
        author = category.getAuthor();
        try {
            Document headDoc;
            Elements elements;
            CachedResponse rawFile = HtmlClient.executeRequest(request, cached);
            String[] parts = TextUtils.Splitter.extractLines(rawFile, rawFile.getEncoding(), false,
                    new TextUtils.Splitter().addEnd("Первый блок ссылок"),
                    new TextUtils.Splitter("Блок шапки", "Блок управления разделом"),
                    new TextUtils.Splitter("Блок ссылок на произведения", "Подножие"));
            // head - Categury Info
            if (parts.length > 0) {
                String title = Jsoup.parseBodyFragment(parts[0]).select("center > h3").text();
                int delim = title.indexOf(":");
                if(delim > 0) {
                    category.setTitle(title.substring(delim + 1));
                    if (hasNotAuthor) {
                        author.setFullName(title.substring(0, delim));
                    }
                }
            }
            // Category info
            if (parts.length > 1 && hasNotAuthor) {
                String head = parts[1];
                headDoc = Jsoup.parseBodyFragment(head);
                if (headDoc == null) return category;
                if (hasNotAuthor) {
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
                    author.setAnnotation(headDoc.select("a[href=./]").text());
                }
                if (head.contains("Аннотация")) {
                    String annotation = TextUtils.Splitter.extractLines(new ByteArrayInputStream(head.getBytes(request.getEncoding())),
                            request.getEncoding(),
                            true,
                            new TextUtils.Splitter("<a href=\\./>", "<ul>"))[0];
                    category.setAnnotation(ParserUtils.cleanupHtml(Jsoup.parseBodyFragment(annotation.substring(annotation.indexOf(":") + 1))));
                }
                if (hasNotAuthor) {
                    for (Element a : headDoc.select("ul").select("a")) {
                        Category category = new Category();
                        category.setTitle(a.text());
                        category.setLink(author.getLink() + "/" + a.attr("href"));
                        author.addCategory(category);
                    }
                }
            }
            // body - Works
            if (parts.length > 2 && !parts[2].isEmpty()) {
                Scanner scanner = new Scanner(parts[2]);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    Elements a;
                    if (!line.isEmpty()) {
                        try {
                            if (line.contains("<DL>")) {
                                Document lineDoc = Jsoup.parseBodyFragment(line);
                                Elements dl = lineDoc.select("DL");
                                Work work = ParserUtils.parseWork(dl.first());
                                work.setAuthor(author);
                                work.setCategory(category);
                                if (work.validate()) {
                                    category.addLink(work);
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
            Log.e(TAG, "Category " + category.getTitle() + " parsed");
        } catch (Exception | Error e) {
            Log.e(TAG, e.getMessage(), e);
            if (e instanceof IOException) {
                throw e;
            }
        }
        category.setParsed(true);
        return category;
    }


}
