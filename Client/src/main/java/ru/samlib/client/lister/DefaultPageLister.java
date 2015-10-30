package ru.samlib.client.lister;

import ru.samlib.client.net.Request;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Created by Rufim on 29.06.2015.
 */
public class DefaultPageLister extends PageLister {

    @Override
    public void setPage(Request request, int index) {
        if (index != 0) {
            request.setSuffix("index-" + (index + 1) + ".shtml");
        }
    }

    @Override
    public int getLastPage(Document doc) {
        Elements pages = doc.select("center");
        if (pages.size() > 0) {
            return Integer.parseInt(pages.get(1).select("a").last().text());
        }
        return -1;
    }
}
