package ru.samlib.client.parser;

import android.util.Log;
import org.jsoup.nodes.Document;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.lister.PageDataSource;
import ru.samlib.client.lister.PageLister;
import ru.samlib.client.lister.RowSelector;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 01.11.2015.
 */
public abstract class PageParser<I extends Validatable> extends RowParser<I> implements PageDataSource<I> {

    protected int index = 0;
    protected int pageCount = -1;
    protected PageLister lister;

    public PageParser(String path, RowSelector selector, PageLister lister) throws MalformedURLException {
        super(path, selector);
        this.lister = lister;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int requestPageCount() throws IOException {
     if(pageCount > 0) return pageCount;
        lister.setPage(request, index);
        Document doc = getDocument(request);
        return pageCount = lister.getPageCount(doc);
    }

    @Override
    public List<I> getPage(int index) throws IOException {
        List<I> elementList = new ArrayList<>();
        try {
            if (pageCount > 0 && index > pageCount) return elementList;
            lister.setPage(request, index);
            Document doc = getDocument(request);
            if (pageCount < 0) {
                pageCount = lister.getPageCount(doc);
            }
            if (doc != null) {
                List elements = selectRows(document);
                if (elements.size() == 0) {
                    return elementList;
                }
                parseElements(elements, 0, elements.size(), elementList);
            } else {
                return elementList;
            }
            Log.e(TAG, "Elements parsed: " + elementList.size() + " page is " + index);
        } catch (Exception | ExceptionInInitializerError e) {
            Log.e(TAG, e.getMessage(), e);
            if (e instanceof IOException) {
                throw e;
            }
        }
        return elementList;
    }

}
