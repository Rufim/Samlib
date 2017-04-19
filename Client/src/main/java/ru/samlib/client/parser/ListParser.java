package ru.samlib.client.parser;

import android.util.Log;
import ru.kazantsev.template.lister.DataSource;
import ru.samlib.client.lister.RowSelector;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 01.11.2015.
 */
public abstract class ListParser<I> extends RowParser implements DataSource<I> {

    public ListParser(String path, RowSelector selector) throws MalformedURLException {
        super(path, selector);
    }

    @Override
    public List<I> getItems(int skip, int size) throws IOException {
        List<I> elementList = new ArrayList<>();
        try {
            getDocument(request, MIN_BODY_SIZE);
            if (document != null) {
                List elements = selectRows(document);
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

}
