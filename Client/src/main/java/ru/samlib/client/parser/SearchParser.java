package ru.samlib.client.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.kazantsev.template.domain.Valuable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Type;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.JsoupRowSelector;
import ru.samlib.client.lister.PageLister;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.util.TextUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Created by Rufim on 27.06.2015.
 */
public class SearchParser extends PageListParser {

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

    public SearchParser() throws MalformedURLException {
        super("/cgi-bin/seek", 59, new JsoupRowSelector() {

            @Override
            public String getRowSelector() {
                return "table[width=640][border=0]";
            }

        }, new PageLister() {
            @Override
            public void setPage(Request request, int index) {
                request.addParam(SearchParams.PAGE, index + 1);
            }

            @Override
            public int getPageCount(Document document) {
                return -1;
            }
        });
        request.initParams(SearchParams.values());
    }

    public SearchParser(String query) throws MalformedURLException {
        this();
        request.addParam(SearchParams.FIND, query);
    }

    public void setQuery(String query) {
        request.addParam(SearchParams.FIND, query);
    }

    //TODO: Need to be implemented with filters
    public List search(String string, int skip, int size) throws IOException {
        request.addParam(SearchParams.FIND, string);
        return getItems(skip, size);
    }

    @Override
    protected Validatable parseRow(Element row, int position) {
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
                work.setGenresAsString(subtext[2].substring(subtext[2].indexOf(":") + 2));
            }
            if (tbodys.size() > 1) {
                Elements p = tbodys.get(1).select("p");
                if (p.size() > 1) {
                    String fullSubtext = p.get(1).text();
                    work.setDescription(TextUtils.Splitter.extractStrings(p.html(), true, new TextUtils.Splitter("<br>", "<br>"))[0]);
                }
            }
        }
        return work;
    }
}
