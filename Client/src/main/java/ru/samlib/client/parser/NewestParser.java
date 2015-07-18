package ru.samlib.client.parser;

import android.util.Log;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Work;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.lister.Lister;
import ru.samlib.client.util.ParserUtils;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 22.05.2014.
 */
public class NewestParser extends Parser implements Lister<Work> {

    public NewestParser() {
        try {
            setPath("/long.shtml");
        } catch (MalformedURLException e) {
        }
    }

    public List<Work> getItems(int skip, int size) {

        List<Work> works = new ArrayList<>();
        try {
            Document doc = getDocument(request, MIN_BODY_SIZE);
            Elements tableElements = doc.select("table tbody td[width=600] table tbody");
            Elements tableRowElements = tableElements.select("tr");
            if (2 + skip > tableRowElements.size()) {
                Log.e(TAG, "Is over: skip is " + skip + " size is " + tableRowElements.size());
                return works;
            }
            for (int i = 2 + skip; i < tableRowElements.size() && works.size() < size; i++) {
                Element row = tableRowElements.get(i);
                try {
                    //   System.out.println("row");
                    Elements rowItems = row.select("td");
                    Work work = new Work();
                    for (int j = 0; j < rowItems.size(); j++) {
                        String text = rowItems.get(j).text();
                        switch (j) {
                            case 0:
                                work.setTitle(ParserUtils.trim(text.substring(1, text.lastIndexOf("\"")).replace("\n", "")));
                                if (text.contains("и др.")) {
                                    work.setSize(Integer.parseInt(text.substring(work.getTitle().length() + 11, text.lastIndexOf("k "))));
                                } else {
                                    work.setSize(Integer.parseInt(text.substring(work.getTitle().length() + 5, text.lastIndexOf("k "))));
                                }
                                String[] genres = text.substring(text.indexOf("k ") + 2, text.length()).split(",");
                                for (String genre : genres) {
                                    work.addGenre(genre);
                                }
                                work.setLink(rowItems.get(j).select("a[href]").attr("href"));
                                break;
                            case 1:
                                Author author = new Author();
                                author.setLink(rowItems.get(j).select("a[href]").attr("href").replace("indexdate.shtml", ""));
                                author.setShortName(text);
                                work.setAuthor(author);
                                break;
                            case 2:
                                work.setUpdateDate(ParserUtils.parseData(text));
                                break;
                        }
                    }
                    if (work.validate() && work.getUpdateDate() != null) {
                        works.add(work);
                    } else {
                        throw new Exception("Invalid work");
                    }
                } catch (Exception | Error e) {
                    Log.e(TAG, "Invalid row: " + works.size() + " skip is " + skip + " index is " + i + "" +
                            "row html content:" + row != null ? row.html() : " row not exist", e);
                }
            }

        } catch (Exception | Error e) {
            Log.e(TAG,e.getMessage() , e);
        }
        //      for (Work update : works) {
        //          Log.e("Parser", update.toString());
        //      }
        Log.e(TAG, "Works parsed: " + works.size() + " skip is " + skip);
        if (works.size() > 0 && size > works.size()) {
            works.addAll(getItems(skip + size, size - works.size()));
            return works;
        }
        return works;
    }

}
