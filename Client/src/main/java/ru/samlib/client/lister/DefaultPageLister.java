package ru.samlib.client.lister;

import ru.samlib.client.net.Request;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import ru.samlib.client.util.TextUtils;

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
    public int getLastPage(Document document) {
        return TextUtils.extractInt(document.select("center > b:contains(Страниц)").text());
    }
}
