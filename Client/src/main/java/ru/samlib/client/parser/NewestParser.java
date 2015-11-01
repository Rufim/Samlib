package ru.samlib.client.parser;

import android.util.Log;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Work;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.lister.JsoupRowSelector;
import ru.samlib.client.lister.RowSelector;
import ru.samlib.client.util.TextUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 22.05.2014.
 */
public class NewestParser extends RowParser implements DataSource<Work> {


    public NewestParser() throws MalformedURLException {
        super("/long.shtml", new JsoupRowSelector() {
            @Override
            public String getRowSelector() {
                return "table tbody td[width=600] table tbody tr";
            }
        });
    }

    public List<Work> getItems(int skip, int size) throws IOException {

        List<Work> works = new ArrayList<>();
        try {
            Document doc = getDocument(request, MIN_BODY_SIZE);
            Elements tableRowElements = (Elements) selectRows(doc);
            if (2 + skip > tableRowElements.size()) {
                Log.e(TAG, "Is over: skip is " + skip + " size is " + tableRowElements.size());
                return works;
            }
            parseElements(tableRowElements, 2 + skip, size, works);
        } catch (Exception | Error e) {
            Log.e(TAG,e.getMessage() , e);
            if (e instanceof IOException) {
                throw e;
            }
        }
        Log.e(TAG, "Works parsed: " + works.size() + " skip is " + skip);
        if (works.size() > 0 && size > works.size()) {
            works.addAll(getItems(skip + size, size - works.size()));
            return works;
        }
        return works;
    }

    @Override
    protected Work parseRow(Element row) {
        Elements rowItems = row.select("td");
        Work work = new Work();
        for (int j = 0; j < rowItems.size(); j++) {
            String text = rowItems.get(j).text();
            switch (j) {
                case 0:
                    work.setTitle(TextUtils.trim(text.substring(1, text.lastIndexOf("\"")).replace("\n", "")));
                    Element info = rowItems.select("small").first();
                    String workSize = info.select("b").first().ownText();
                    work.setSize(Integer.parseInt(workSize.substring(0, workSize.lastIndexOf("k"))));
                    work.setGenres(info.ownText());
                    work.setLink(rowItems.get(j).select("a[href]").attr("href"));
                    break;
                case 1:
                    Author author = new Author();
                    author.setLink(rowItems.get(j).select("a[href]").attr("href").replace("indexdate.shtml", ""));
                    author.setShortName(text);
                    work.setAuthor(author);
                    break;
                case 2:
                    work.setUpdateDate(TextUtils.parseData(text));
                    break;
            }
        }
        return work;
    }
}
