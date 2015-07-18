package ru.samlib.client.lister;

import ru.samlib.client.net.Request;
import org.jsoup.nodes.Document;

/**
 * Created by Rufim on 29.06.2015.
 */
public interface ParserPageLister {
    public  void setPage(Request request, int index);
    public  String getRowSelector();
    public  int getLastPage(Document document);
}
