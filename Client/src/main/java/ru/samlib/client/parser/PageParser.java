package ru.samlib.client.parser;

import android.util.Log;

import org.jsoup.Jsoup;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.lister.*;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.samlib.client.util.TextUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 29.06.2015.
 */
public abstract class PageParser<E extends Validatable> extends RowParser<E> implements DataSource<E> {

    protected final int pageSize;
    protected final int firstPageSize;

    protected int index = 0;
    protected int lastPage = -1;
    protected PageLister lister;

    public PageParser(String path, int pageSize, RowSelector selector, PageLister lister) throws MalformedURLException {
        super(path, selector);
        this.pageSize = pageSize;
        this.lister = lister;
        this.firstPageSize = pageSize;
    }

    public PageParser(String path, int pageSize, int firstPageSize, RowSelector selector, PageLister lister) throws MalformedURLException {
        super(path, selector);
        this.pageSize = pageSize;
        this.lister = lister;
        this.firstPageSize = firstPageSize;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getFirstPageSize() {
        return firstPageSize;
    }

    @Override
    public List<E> getItems(int skip, int size) throws IOException {
        List<E> elementList = new ArrayList<>();
        try {
            if(skip == 0) {
                index = 0;
            } else {
                index = Math.abs(skip - firstPageSize) / pageSize;
            }
            if(skip >= firstPageSize) {
                index ++;
            }
            if (lastPage > 0 && index > lastPage) return elementList;
            lister.setPage(request, index);
            Document doc = getDocument(request);
            if (lastPage < 0) {
                lastPage = lister.getLastPage(doc);
            }
            if (doc != null) {
                List elements = selectRows(document, selector);
                if (elements.size() == 0) {
                    return elementList;
                }
                if(index > 0) {
                    parseElements(elements, skip - (pageSize * (index - 1) + firstPageSize), size, elementList);
                } else {
                    parseElements(elements, skip, size, elementList);
                }
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
