package ru.samlib.client.parser;

import android.util.Log;

import org.jsoup.Jsoup;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.lister.DataSource;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.lister.JsoupPageLister;
import ru.samlib.client.lister.PageLister;
import ru.samlib.client.lister.RawPageLister;
import ru.samlib.client.util.TextUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Rufim on 29.06.2015.
 */
public abstract class PageParser<E extends Validatable> extends Parser implements DataSource<E> {

    protected final int pageSize;

    protected int index = 0;
    protected int lastPage = -1;
    protected PageLister lister;

    public PageParser(String path, int pageSize, PageLister lister) {
        try {
            setPath(path);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Wrong url " + Link.getBaseDomain() + path, e);
        }
        this.pageSize = pageSize;
        this.lister = lister;
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
                List elements = new ArrayList<>();
                if (lister instanceof JsoupPageLister) {
                    elements = doc.select(((JsoupPageLister) lister).getRowSelector());
                } else if (lister instanceof RawPageLister) {
                    RawPageLister rawPageLister = ((RawPageLister) lister);
                    elements = TextUtils.Splitter.extractLines(htmlFile, false, rawPageLister.getRowStartDelimiter(), rawPageLister.getRowEndDelimiter());
                }
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

    protected void parseElements(List elements, int skip, int size, List<E> elementList) {
        for (int i = skip; i < elements.size() && elementList.size() < size; i++) {
            Object row = elements.get(i);
            Element element;
            try {
                if (row instanceof Element) {
                    element = (Element) row;
                } else {
                    element = Jsoup.parseBodyFragment(row.toString());
                }
                E item = parseRow(element);
                if (item.validate()) {
                    elementList.add(item);
                } else {
                    throw new Exception("Invalid data parsed");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Invalid row: " + elementList.size() + " skip is " + skip + " index is " + i + "" +
                        "\n row html content: " + row, ex);
            }
        }
    }

    protected abstract E parseRow(Element row);

}
