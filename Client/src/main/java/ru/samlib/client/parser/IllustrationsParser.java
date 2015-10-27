package ru.samlib.client.parser;

import android.util.Log;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.entity.Image;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.util.SystemUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 26.10.2015.
 */
public class IllustrationsParser extends Parser implements DataSource<Image> {

    public IllustrationsParser(Work work) throws MalformedURLException {
        setPath("/img/" + work.getLink());
    }

    @Override
    public List<Image> getItems(int skip, int size) throws IOException {
        List<Image> imageList = new ArrayList<>();
        try {
            Document doc = getDocument(request);
            Elements images = doc.select("table[width=640][align=center] td");
            for (Element imageElement : images) {
                Image image = new Image();
                Element img = imageElement.select("img").first();
                image.setLink(img.attr("src"));
                image.setHeight(SystemUtils.parseInt(img.attr("height")));
                image.setWidth(SystemUtils.parseInt(img.attr("width")));
                image.setTitle(imageElement.select("b").text());
                image.setAnnotation(imageElement.select("i").text());
                String desc = imageElement.select("small").text();
                int start = desc.lastIndexOf(", ");
                if(start != -1) {
                    image.setSize(SystemUtils.parseInt(desc.substring(start, desc.lastIndexOf("k"))));
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
