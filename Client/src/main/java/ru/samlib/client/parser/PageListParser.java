package ru.samlib.client.parser;

import android.util.Log;
import org.jsoup.nodes.Document;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.lister.PageLister;
import ru.samlib.client.lister.RowSelector;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 29.06.2015.
 */
public abstract class PageListParser<E extends Validatable> extends RowParser<E> implements DataSource<E> {

    protected final int pageSize;

    protected int index = 0;
    protected int lastPage = -1;
    protected PageLister lister;

    public PageListParser(String path, int pageSize, RowSelector selector, PageLister lister) throws MalformedURLException {
        super(path, selector);
        this.pageSize = pageSize;
        this.lister = lister;
    }

    public int getPageSize() {
        return pageSize;
    }

    @Override
    public List<E> getItems(int skip, int size) throws IOException {
        List<E> elementList = new ArrayList<>();
        try {
            index = skip / pageSize;
            if (lastPage > 0 && index > lastPage) return elementList;
            lister.setPage(request, index);
            Document doc = getDocument(request);
            if (lastPage < 0) {
                lastPage = lister.getLastPage(doc);
            }
            if (doc != null) {
                List elements = selectRows(doc);
                if (elements.size() == 0) {
                    return elementList;
                }
                parseElements(elements, skip - pageSize * index, size, elementList);
            } else {
                return elementList;
            }
            Log.e(TAG, "Elements parsed: " + elementList.size() + " skip is " + skip);
        } catch (Exception | ExceptionInInitializerError e) {
            Log.e(TAG, e.getMessage(), e);
            if (e instanceof IOException) {
                throw e;
            }
        }
        //  for (Element element : elementList) {
        //      Log.e(TAG, element.toString());
        //  }
        return elementList;
    }

}
