package ru.samlib.client.parser;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.lister.JsoupRowSelector;
import ru.samlib.client.lister.RawRowSelector;
import ru.samlib.client.lister.RowSelector;
import ru.kazantsev.template.util.TextUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dmitry on 30.10.2015.
 */
public abstract class RowParser<I extends Validatable> extends Parser {

    protected RowSelector selector;

    public RowParser(String path, RowSelector selector) throws MalformedURLException {
        setPath(path);
        this.selector = selector;
    }

    protected List selectRows(Document document) throws IOException {
        List elements = new ArrayList<>();
        if (selector instanceof JsoupRowSelector) {
            elements = document.select(((JsoupRowSelector) selector).getRowSelector());
        } else if (selector instanceof RawRowSelector) {
            RawRowSelector rawRowSelector = ((RawRowSelector) selector);
            elements = TextUtils.Splitter.extractLines(htmlFile, htmlFile.getEncoding() ,  false, rawRowSelector.getRowStartDelimiter(), rawRowSelector.getRowEndDelimiter());
        }
        return elements;
    }

    protected void parseElements(List elements, int skip, int size, List<I> items) {
        for (int i = skip; i < elements.size() && items.size() < size; i++) {
            Object row = elements.get(i);
            Element element;
            try {
                if (row instanceof Element) {
                    element = (Element) row;
                } else {
                    element = Jsoup.parseBodyFragment(row.toString());
                }
                I item = parseRow(element, i);
                if(item == null) continue;
                if (item.validate()) {
                    items.add(item);
                } else {
                    throw new Exception("Invalid data parsed");
                }
            } catch (Exception ex) {
                Log.w(TAG, "Invalid row: " + items.size() + " skip is " + skip + " index is " + i + "" +
                        "\n row html content: " + row, ex);
            }
        }
    }

    protected abstract I parseRow(Element row, int position);

}