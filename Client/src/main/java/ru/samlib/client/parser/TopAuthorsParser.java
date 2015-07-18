package ru.samlib.client.parser;

import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.lister.DefaultPageLister;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by Rufim on 16.01.2015.
 */
public class TopAuthorsParser extends PageParser {

    public TopAuthorsParser() {
        super("/rating/hits/", 100, new DefaultPageLister());
    }

    @Override
    protected Validatable parseRow(Element row) {
        Elements rowItems = row.children();
        Author author = new Author();
        int j = 0;
        if (rowItems.size() > 3) {
            author.setIsNew(rowItems.get(j++).text().equals("New"));
        }
        String peopleViews = rowItems.get(j++).select("b").text();
        author.setViews(Integer.parseInt(peopleViews));
        Element item = rowItems.get(j++);
        author.setLink(item.select("a[href]").attr("href"));
        author.setFullName(item.text());
        author.setAnnotation(row.ownText());
        if (rowItems.size() > j) {
            author.setSectionAnnotation(rowItems.get(j).text());
        }
        return author;
    }
}
