package ru.samlib.client.parser;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.lister.DefaultJsoupSelector;
import ru.samlib.client.lister.DefaultPageLister;

import java.net.MalformedURLException;

/**
 * Created by Rufim on 16.01.2015.
 */
public class TopAuthorsParser extends PageListParser {

    public TopAuthorsParser() throws MalformedURLException {
        super("/rating/hits/", 100, new DefaultJsoupSelector(),  new DefaultPageLister());
    }

    @Override
    protected Validatable parseRow(Element row, int position) {
        Elements rowItems = row.children();
        Author author = new Author();
        int j = 0;
        if (rowItems.size() > 3) {
            author.setNewest(rowItems.get(j++).text().equals("New"));
        }
        String peopleViews = rowItems.get(j++).select("b").text();
        author.setViews(Integer.parseInt(peopleViews));
        Element item = rowItems.get(j++);
        author.setSmartLink(item.select("a[href]").attr("href"));
        author.setFullName(item.text());
        author.setAnnotation(row.ownText());
        if (rowItems.size() > j) {
            author.setSectionAnnotation(rowItems.get(j).text());
        }
        return author;
    }
}
