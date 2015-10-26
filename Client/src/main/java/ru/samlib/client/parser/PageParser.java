package ru.samlib.client.parser;

import android.util.Log;

import ru.samlib.client.domain.Validatable;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.lister.DataSource;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.lister.ParserPageLister;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 29.06.2015.
 */
public abstract class PageParser<E extends Validatable> extends Parser implements DataSource<E> {

    protected final int pageSize;

    protected int index = 0;
    protected int lastPage = -1;
    protected ParserPageLister lister;

    public PageParser(String path, int pageSize, ParserPageLister lister) {
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
                Elements elements;
                index = skip / pageSize;
                if (lastPage > 0 && index > lastPage) return elementList;
                lister.setPage(request, index);
                Document doc = getDocument(request);
                if (lastPage < 0) {
                    lastPage = lister.getLastPage(doc);
                }
                if (doc != null) {
                    elements = doc.select(lister.getRowSelector());
                    if (elements.size() == 0) {
                        return elementList;
                    }
                } else {
                    return elementList;
                }
                parseElements(elements, skip - pageSize * index, size, elementList);
                Log.e(TAG, "Elements parsed: " + elementList.size() + " skip is " + skip);
            } catch (Exception | ExceptionInInitializerError e) {
                Log.e(TAG, e.getMessage(), e);
                if(e instanceof IOException) {
                    throw e;
                }
            }
            //  for (Element element : elementList) {
            //      Log.e(TAG, element.toString());
            //  }
            return elementList;
    }

    protected void parseElements(Elements elements, int skip, int size, List<E> elementList) {
        for (int i = skip; i < elements.size() && elementList.size() < size; i++) {
            Element row = elements.get(i);
            try {
                E element = parseRow(row);
                if (element.validate()) {
                    elementList.add(element);
                } else {
                    throw new Exception("Invalid data parsed");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Invalid row: " + elementList.size() + " skip is " + skip + " index is " + i + "" +
                        "\n row html content: " + row.html(), ex);
            }
        }
    }

    protected abstract E parseRow(Element row);

}
