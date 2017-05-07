package ru.samlib.client.lister;

import org.jsoup.nodes.Document;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.util.TextUtils;

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
        int i =  TextUtils.extractInt(document.select("center > b:contains(Страниц)").text());
        if(i < 0) return 0;
        return i;
    }
}
