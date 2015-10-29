package ru.samlib.client.parser;

import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.Valuable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.domain.entity.Type;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.JsoupPageLister;
import ru.samlib.client.net.Request;
import ru.samlib.client.lister.PageLister;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.util.TextUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by Rufim on 27.06.2015.
 */
public class SearchParser extends PageParser {

    protected static final String TAG = SearchParser.class.getSimpleName();

    public enum SearchParams implements Valuable {
        DIR(""), PLACE("index"), FIND(""), JANR("0"), TYPE("0"), PAGE("0");

        private final String defaultValue;

        SearchParams(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public Object value() {
            return defaultValue;
        }
    }

    public SearchParser() {
        super("/cgi-bin/seek", 59, new JsoupPageLister() {
            @Override
            public void setPage(Request request, int index) {
                request.setParam(SearchParams.PAGE, index + 1);
            }

            @Override
            public String getRowSelector() {
                return "table[width=640][border=0]";
            }

            @Override
            public int getLastPage(Document document) {
                return -1;
            }
        });
        request.initParams(SearchParams.values());
    }

    public void setQuery(String query) {
        request.setParam(SearchParams.FIND, query);
    }

    //TODO: Need to be implemented with filters
    public List search(String string, int skip, int size) throws IOException {
        request.setParam(SearchParams.FIND, string);
        return getItems(skip, size);
    }

    @Override
    protected Validatable parseRow(Element row) {
        Elements tbodys = row.select("tbody");
        Work work = new Work();
        if (tbodys.size() > 0) {
            Elements workElements = tbodys.get(0).select("a");
            Author author = new Author();
            author.setLink(workElements.get(0).select("a[href]").attr("href"));
            author.setFullName(workElements.get(0).text());
            work.setAuthor(author);
            work.setTitle(workElements.get(1).text());
            work.setLink(workElements.get(1).select("a[href]").attr("href"));
            work.setSize(Integer.parseInt(tbodys.get(0).select("ul").text().split("k")[0]));
            String[] subtext = tbodys.get(1).select("font[size=2]").text().split("\"");
            if (subtext.length > 1 && !subtext[1].isEmpty()) {
                work.setType(Type.parseType(subtext[1].substring(subtext[1].indexOf(":") + 2)));
            }
            if (subtext.length > 2 && !subtext[2].isEmpty()) {
                work.setGenres(subtext[2].substring(subtext[2].indexOf(":") + 2));
            }
            if (tbodys.size() > 1) {
                Elements p = tbodys.get(1).select("p");
                if (p.size() > 1) {
                    String fullSubtext = p.get(1).text();
                    work.setDescription(TextUtils.Splitter.extractString(p.html(), true, new TextUtils.Splitter("<br>", "<br>"))[0]);
                }
            }
        }
        return work;
    }
}
