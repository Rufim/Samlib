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
    @ManyToOne(cascade = {CascadeAction.SAVE, CascadeAction.DELETE})
    Author author;
    Type type = Type.OTHER;
    @OneToMany(cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<Work> works = new ArrayList<>();
    @OneToMany(cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<Link> links = new ArrayList<>();
    String link;

    @Transient
    boolean parsed = false;

    public CategoryEntity createEntry(){
        if(getClass() == CategoryEntity.class) return (CategoryEntity) this;
        CategoryEntity entity = new CategoryEntity();
        entity.setAnnotation(annotation);
        entity.setAuthor(author);
        entity.setId(id);
        entity.setLink(link);
        entity.setParsed(parsed);
        entity.setTitle(title);
        entity.setType(type);
        entity.works = null;
        entity.links = null;
        for (Work work : works) {
            work.setAuthor(author);
            work.setCategory(entity);
            entity.getWorks().add(work.createEntity());
        }
        for (Link link1 : links) {
            link1.setAuthor(author);
            link1.setCategory(entity);
            entity.getLinks().add(link1.createEntity());
        }
        return entity;
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
         linkables.addAll(getWorks());
         linkables.addAll(getLinks());
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
