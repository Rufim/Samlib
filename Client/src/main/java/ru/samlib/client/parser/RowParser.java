package ru.samlib.client.parser;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.lister.JsoupRowSelector;
import ru.samlib.client.lister.RawRowSelector;
import ru.samlib.client.lister.RowSelector;
import ru.samlib.client.util.TextUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dmitry on 30.10.2015.
 */
public abstract class RowParser<E extends Validatable> extends Parser implements DataSource<E> {

    protected RowSelector selector;

    public RowParser(String path, RowSelector selector) {
        try {
            setPath(path);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Wrong url " + Link.getBaseDomain() + path, e);
        }
        this.selector = selector;
    }

    @Override
    public List<E> getItems(int skip, int size) throws IOException {
        List<E> elementList = new ArrayList<>();
        try {
            getDocument(request, MIN_BODY_SIZE);
            if (document != null) {
                List elements = selectRows(document, selector);
                if (elements.size() == 0) {
                    return elementList;
                }
                parseElements(elements, skip, size, elementList);
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

    protected List selectRows(Document document, RowSelector selector) {
        List elements = new ArrayList<>();
        if (selector instanceof JsoupRowSelector) {
            elements = document.select(((JsoupRowSelector) selector).getRowSelector());
        } else if (selector instanceof RawRowSelector) {
            RawRowSelector rawRowSelector = ((RawRowSelector) selector);
            elements = TextUtils.Splitter.extractLines(htmlFile, false, rawRowSelector.getRowStartDelimiter(), rawRowSelector.getRowEndDelimiter());
        }
        return elements;
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