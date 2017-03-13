package ru.samlib.client.parser;

import org.jsoup.nodes.Element;
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.DefaultJsoupSelector;
import ru.samlib.client.lister.DefaultPageLister;
import ru.samlib.client.net.Request;
import ru.samlib.client.util.ParserUtils;

import java.net.MalformedURLException;

/**
 * Created by Rufim on 07.01.2015.
 */
public class GenreParser extends PageListParser<Work> {

    public GenreParser(Genre genre) throws MalformedURLException {
        super(genre.getLink(), 200, new DefaultJsoupSelector(), new DefaultPageLister() {
            @Override
            public void setPage(Request request, int index) {
                request.setSuffix("-" + (index + 1) + ".shtml");
            }
        });
    }

    @Override
    protected Work parseRow(Element row, int position) {
       return ParserUtils.parseWork(row);
    }
}
