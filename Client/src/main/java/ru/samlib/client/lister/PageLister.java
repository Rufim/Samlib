package ru.samlib.client.lister;

import org.jsoup.nodes.Document;
import ru.samlib.client.net.Request;

/**
 * Created by Rufim on 29.06.2015.
 */
public abstract class PageLister {
    public abstract void setPage(Request request, int index);
    public abstract int getLastPage(Document document);
}
