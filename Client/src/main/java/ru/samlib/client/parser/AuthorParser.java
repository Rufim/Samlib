package ru.samlib.client.parser;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.kazantsev.template.net.HTTPExecutor;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.samlib.client.App;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.*;
import ru.kazantsev.template.net.CachedResponse;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.util.ParserUtils;
import ru.kazantsev.template.util.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by Rufim on 01.07.2015.
 */
public class AuthorParser extends Parser {


    public static class AuthorNotExistException extends Exception {

    }

    private static final String TAG = AuthorParser.class.getSimpleName();

    private Author author;

    public AuthorParser(Author author) throws MalformedURLException {
        setPath(author.getLink());
        this.author = author;
    }

    public AuthorParser(String authorLink) throws MalformedURLException {
        this(new Author(authorLink));
    }

    public Author parse() throws IOException, AuthorNotExistException {
        try {
            Document headDoc;
            Elements elements;
            CachedResponse rawFile;
            if(!cached && AndroidSystemUtils.isNetworkAvailable(App.getInstance())) {
                if(HTTPExecutor.pingHost(Constants.Net.BASE_HOST, 80, 1000)) {
                   if(HTTPExecutor.pingURL(request.getUrl().toString(), 1000) == 404) {
                        throw new AuthorNotExistException();
                   }
                }
            }
            if (author.getCategories().isEmpty()) {
                rawFile = HtmlClient.executeRequest(request, MIN_BODY_SIZE, cached);
            } else {
                rawFile = HtmlClient.executeRequest(request, cached);
                author.setWorks(new ArrayList<>());
            }
            if (rawFile.isDownloadOver()) {
                author.setParsed(true);
            }
            List<Category> categories = new ArrayList<>();
            List<Work> rootWorks = new ArrayList<>();
            List<Link> rootLinks = new ArrayList<>();
            List<Work> recommendations = new ArrayList<>();
            String[] parts = TextUtils.Splitter.extractLines(rawFile, rawFile.getEncoding(), false,
                    new TextUtils.Splitter().addEnd("Первый блок ссылок"),
                    new TextUtils.Splitter("Блок шапки", "Блок управления разделом"),
                    new TextUtils.Splitter("Блок ссылок на произведения", "Подножие"));
            // head - Author Info
            if (parts.length > 0 && parts[0] != null) {
                String[] titles = Jsoup.parseBodyFragment(parts[0]).select("center > h3").text().split(":");
                author.setFullName(titles[0]);
                author.setAnnotation(titles[1].trim());
            }
            // Author info
            if (parts.length > 1) {
                String head = parts[1];
                headDoc = Jsoup.parseBodyFragment(head);
                if (headDoc == null) return author;
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
                            Jsoup.parseBodyFragment(TextUtils.Splitter
                                    .extractLines(new ByteArrayInputStream(head.getBytes(request.getEncoding())),
                                            request.getEncoding(),
                                            true,
                                            new TextUtils.Splitter("Об авторе", "(Аннотация к разделу)|(Начните знакомство с)"))[0]).text());
                }
                if (head.contains("Аннотация к разделу")) {
                    author.setSectionAnnotation(TextUtils.Splitter.extractStrings(headDoc.text(),
                            true,
                            new TextUtils.Splitter().addStart("Аннотация к разделу: "))[0]);
                }
                if (head.contains("Начните знакомство с")) {
                    String[] rec = TextUtils.Splitter.extractStrings(head, true, new TextUtils.Splitter("<b>Начните знакомство с</b>:", "\\n"));
                    Document recDoc = Jsoup.parseBodyFragment(rec[0]);
                    for (Element workEl : recDoc.select("li")) {
                        Work work = ParserUtils.parseWork(workEl);
                        work.setAuthor(author);
                        recommendations.add(work);
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
                                newCategory.setAuthor(author);
                                categories.add(newCategory);
                            }
                            if (line.startsWith("</small>")) {
                                newCategory = new Category();
                                a = lineDoc.select("a");
                                newCategory.setTitle(a.text());
                                if (line.contains("href=index")) {
                                    newCategory.setLink(a.attr("href"));
                                } else if (line.contains("href=/type")) {
                                    newCategory.setType(Type.parseType(a.attr("href")));
                                }
                                newCategory.setAuthor(author);
                                scanner.nextLine();
                                line = scanner.nextLine();
                                if (line.startsWith("<font")) {
                                    String annotation = line;
                                    line = scanner.nextLine();
                                    if (line.startsWith("<dd>")) {
                                        newCategory.setAnnotation(ParserUtils.cleanupHtml(Jsoup.parseBodyFragment(annotation + line.substring(line.indexOf("<dd>")))));
                                        line = scanner.nextLine();
                                    } else {
                                        newCategory.setAnnotation(ParserUtils.cleanupHtml(Jsoup.parseBodyFragment(annotation)));
                                    }
                                }
                                lineDoc = Jsoup.parseBodyFragment(line);
                                categories.add(newCategory);
                            }
                            if (line.contains("<DL>")) {
                                Elements dl = lineDoc.select("DL");
                                if (line.contains("TYPE=square")) {
                                    a = dl.select("a");
                                    Link link = new Link(a.text(), a.attr("href"), dl.select("i").text());
                                    if (newCategory == null) {
                                        link.setAuthor(author);
                                        rootLinks.add(link);
                                    } else {
                                        newCategory.addLink(link);
                                    }
                                    continue;
                                }
                                Work work = ParserUtils.parseWork(dl.first());
                                work.setAuthor(author);
                                work.setCategory(newCategory);
                                if (work.validate()) {
                                    if (newCategory == null) {
                                        work.setRootWork(true);
                                        rootWorks.add(work);
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
            for (Category category : categories) {
                if (category.getLink() != null) {
                    new CategoryParser(category).parse();
                    category.setParsed(true);
                }
            }
            if (author.isEntity() && author.isObservable()) {
                merge(author, categories, rootWorks, rootLinks);
                Log.e(TAG, "Author " + author.getTitle() + " merged");
            } else {
                for (Category category : categories) {
                    int index = author.getCategories().indexOf(category);
                    if(index > 0 && author.getCategories().get(index).isInUIExpanded()) {
                        category.setInUIExpanded(true);
                    }
                }
                author.setCategories(categories);
                author.setRootLinks(rootLinks);
                author.setRootWorks(rootWorks);
            }
            for (Work recommendation : recommendations) {
                author.addRecommendation(recommendation);
            }
        } catch (IOException | Error e) {
            Log.e(TAG, e.getMessage(), e);
            if (e instanceof IOException) {
                throw e;
            }
        }
        return author;
    }


    private void merge(Author author, List<Category> newCategories, List<Work> newRootWorks, List<Link> newRootLinks) {
        List<Category> oldCategories = author.getCategories();
        List<Work> oldRootWorks = author.getRootWorks();
        List<Link> oldRootLinks = author.getRootLinks();
        merge(newRootWorks, newRootLinks, oldRootWorks, oldRootLinks, null, author);
        Iterator<Category> ocit = oldCategories.iterator();
        while (ocit.hasNext()) {
            Category oldCategory = ocit.next();
            int newCategoryIndex = hasCategory(newCategories, oldCategory);
            if (newCategoryIndex >= 0) {
                Category newCategory = newCategories.get(newCategoryIndex);
                oldCategory.setTitle(newCategory.getTitle());
                oldCategory.setType(newCategory.getType());
                oldCategory.setLink(newCategory.getLink());
                merge(newCategory.getWorks(), newCategory.getLinks(), oldCategory.getWorks(), oldCategory.getLinks(), oldCategory, author);
                Log.e(TAG, "Category " + newCategory.getTitle() + " merged");
                newCategories.remove(newCategoryIndex);
            } else {
                oldCategory.delete();
                ocit.remove();
            }
        }
        if (!newCategories.isEmpty()) {
            for (Category category : newCategories) {
                oldCategories.add(category);
                category.setAuthor(author);
                boolean atLeastOne = false;
                for (Work work : category.getWorks()) {
                    work.setChanged(true);
                    work.setCategory(category);
                    work.setChangedDate(new Date());
                    atLeastOne = true;
                }
                if (atLeastOne) {
                    author.hasNewUpdates();
                }
            }
        }
    }

    private void merge(List<Work> newWorks, List<Link> newLinks, List<Work> oldWorks, List<Link> oldLinks, Category category, Author author) {
        Iterator<Work> owit = oldWorks.iterator();
        Iterator<Link> olit = oldLinks.iterator();
        while (olit.hasNext()) {
            Link oldLink = olit.next();
            int newLinkIndex = hasLink(newLinks, oldLink);
            if (newLinkIndex >= 0) {
                Link newLink = newLinks.get(newLinkIndex);
                oldLink.setTitle(newLink.getTitle());
                newLinks.remove(newLinkIndex);
            } else {
                oldLink.delete();
                olit.remove();
                if(author.getLinks() != oldLinks) {
                    author.getWorks().remove(oldLink);
                }
            }
        }
        if (!newLinks.isEmpty()) {
            for (Link newLink : newLinks) {
                newLink.setAuthor(author);
                newLink.setCategory(category);
                category.addLink(newLink);
            }
        }
        while (owit.hasNext()) {
            Work oldWork = owit.next();
            int newWorkIndex = hasWork(newWorks, oldWork);
            if (newWorkIndex >= 0) {
                Work newWork = newWorks.get(newWorkIndex);
                updateWork(oldWork, newWork);
                if (newWork.getSize() == null) {
                    newWork.setSize(0);
                }
                if (oldWork.getSize() == null) {
                    oldWork.setSize(0);
                }
                if (!oldWork.getSize().equals(newWork.getSize())) {
                    oldWork.setChanged(true);
                    oldWork.setChangedDate(new Date());
                    Integer oldSize = oldWork.getSize() == null ? 0 : oldWork.getSize();
                    Integer oldDiff = oldWork.getSizeDiff() == null ? 0 : oldWork.getSizeDiff();
                    oldWork.setSizeDiff(newWork.getSize() - oldWork.getSize());
                    oldWork.setSize(newWork.getSize());
                    if (oldWork.getSizeDiff() + oldWork.getSize() != oldSize + oldDiff) {
                        author.hasNewUpdates();
                    }
                }
                newWorks.remove(newWorkIndex);
            } else {
                oldWork.delete();
                owit.remove();
                if(author.getWorks() != oldWorks) {
                    author.getWorks().remove(oldWork);
                }
            }
        }
        if (!newWorks.isEmpty()) {
            for (Work newWork : newWorks) {
                newWork.setChanged(true);
                newWork.setChangedDate(new Date());
                newWork.setAuthor(author);
                if (category != null) {
                    newWork.setAuthor(author);
                    newWork.setCategory(category);
                    category.addLink(newWork);
                    author.hasNewUpdates();
                } else if (author != null) {
                    newWork.setAuthor(author);
                    author.getWorks().add(newWork);
                    newWork.setRootWork(true);
                    newWork.setCategory(null);
                    author.hasNewUpdates();
                }
            }
        }
    }

    private int hasLink(List<Link> linkables, Link linkable) {
        for (int i = 0; i < linkables.size(); i++) {
            if (linkables.get(i).getLink() != null && linkables.get(i).getLink().equals(linkable.getLink()))
                return i;
        }
        return -1;
    }

    private int hasWork(List<Work> linkables, Work linkable) {
        for (int i = 0; i < linkables.size(); i++) {
            if (linkables.get(i).equals(linkable))
                return i;
        }
        return -1;
    }

    private int hasCategory(List<Category> categories, Category category) {
        for (int i = 0; i < categories.size(); i++) {
            Category newCategory = categories.get(i);
            if (category.equals(newCategory)) {
                return i;
            }
        }
        return -1;
    }

    private boolean simpleCategory(Category category) {
        return Type.OTHER.equals(category.getType())
                && category.getLink() == null;
    }

    private void updateWork(Work into, Work from) {
        into.setTitle(from.getTitle());
        into.setLink(from.getLink());
        into.setRate(from.getRate());
        into.setKudoed(from.getKudoed());
        into.setGenres(from.getGenres());
        into.setType(from.getType());
        into.setAnnotationBlocks(from.getAnnotationBlocks());
        into.setState(from.getState());
        into.setHasIllustration(from.isHasIllustration());
        into.setHasComments(from.isHasComments());
    }

}
