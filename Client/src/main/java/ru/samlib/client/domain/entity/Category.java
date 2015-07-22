package ru.samlib.client.domain.entity;

import android.graphics.Color;
import android.text.TextUtils;
import lombok.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 01.07.2015.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class Category implements Linkable, Serializable, Parsable {

    private static final long serialVersionUID = 6549621729790810154L;

    private String title;
    private String annotation;
    private Author author;
    private Type type = Type.OTHER;
    @Setter(AccessLevel.NONE)
    private List<Linkable> links = new ArrayList<>();
    private String link;
    private boolean parsed = false;

    public void addLink(Linkable link) {
        this.links.add(link);
    }

    public Linkable getLinkable() {
        if (type == type.OTHER && link != null) {
            return new Link(title, link);
        } else {
            return type;
        }
    }

    public void setParsed(boolean parsed) {
        if (link != null) {
            this.parsed = parsed;
        }
    }

    public String processAnnotation(int color) {
        Document an = Jsoup.parse(annotation);
        an.select("font[color=#393939]").attr("color",
                String.format("#%02x%02x%02x",
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)));
        an.select("dd").unwrap();
        return an.body().html();
    }
}
