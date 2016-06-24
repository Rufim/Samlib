package ru.samlib.client.domain.entity;

import android.graphics.Color;
import io.requery.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
@Entity
public class Category implements Linkable, Serializable, Parsable {

    private static final long serialVersionUID = 6549621729790810154L;

    @Key @Generated
    Integer id;

    String title;
    String annotation;
    @ManyToOne
    Author author;
    Type type = Type.OTHER;
    List<Work> works = new ArrayList<>();
    List<Link> links = new ArrayList<>();
    String link;

    @Transient
    boolean parsed = false;

    public Category(Category other) {
        this.id = other.id;
        this.title = other.title;
        this.annotation = other.annotation;
        this.author = other.author;
        this.type = other.type;
        this.works = other.works;
        this.links = other.links;
        this.link = other.link;
        this.parsed = other.parsed;
    }

    public String getLink() {
        if(link != null && !link.contains(author.getLink())) {
            link = author.getLink() + "/" + link;
        }
        return link;
    }

    public void addLink(Linkable linkable) {
        if(linkable instanceof Work) {
            this.works.add((Work) linkable);
        }
        if(linkable instanceof Link) {
            this.links.add((Link) linkable);
        }
    }

    public Linkable getLinkable() {
        if (type == type.OTHER) {
            if (link == null) return new Link(title, "", annotation); else
                return new Link(title, getLink(), annotation);
        } else {
            return type;
        }
    }

     public List<Linkable> getLinkables()  {
         List<Linkable> linkables = new ArrayList<>();
         linkables.addAll(works);
         linkables.addAll(links);
         return linkables;
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

    public String getTitle() {
        return getLinkable().getTitle();
    }

    @Override
    public String toString() {
        return title;
    }
}
