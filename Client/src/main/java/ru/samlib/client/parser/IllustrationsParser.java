package ru.samlib.client.parser;

import android.util.Log;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.entity.Image;
import ru.samlib.client.domain.entity.Work;
import ru.kazantsev.template.lister.DataSource;
import ru.kazantsev.template.util.TextUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 26.10.2015.
 */
public class IllustrationsParser extends Parser implements DataSource<Image> {

    public IllustrationsParser(Work work) throws MalformedURLException {
        setPath(work.getIllustrationsLink().getLink());
    }

    @Override
    public List<Image> getItems(int skip, int size) throws IOException {
        List<Image> imageList = new ArrayList<>();
        try {
            Document doc = getDocument(request);
            Elements images = doc.select("table[width=640][align=center] td");
            for (int i = skip; i < images.size() && i < size; i++) {
                Image image = new Image();
                Element imageElement = images.get(i);
                Element img = imageElement.select("img").first();
                image.setNumber(i);
                image.setLink(img.attr("src"));
                image.setHeight(TextUtils.parseInt(img.attr("height")));
                image.setWidth(TextUtils.parseInt(img.attr("width")));
                image.setTitle(imageElement.select("b").text());
                image.setAnnotation(imageElement.select("i").text());
                String desc = imageElement.select("small").text();
                int start = desc.lastIndexOf(", ");
                if (start != -1) {
                    image.setSize(TextUtils.parseInt(desc.substring(start, desc.lastIndexOf("k"))));
                }
                imageList.add(image);
            }
        } catch (Exception | Error e) {
            Log.e(TAG, e.getMessage(), e);
            if (e instanceof IOException) {
                throw e;
            }
        }
        return imageList;
    }
}
