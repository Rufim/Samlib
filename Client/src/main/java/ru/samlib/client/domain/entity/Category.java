package ru.samlib.client.domain.entity;

import android.graphics.Color;
import io.requery.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 01.07.2015.
 */
@Data
@Entity
public class Category implements Linkable, Serializable, Parsable {

    private static final long serialVersionUID = 6549621729790810154L;

    @Key
    @Generated
    Integer id;

    String title;
    String annotation;
    @ManyToOne(cascade = CascadeAction.SAVE)
    Author author;
    Type type = Type.OTHER;
    @OneToMany(cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<Work> works;
    @OneToMany(cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<Link> links;
    String link;

    @Transient
    boolean parsed = false;

    public Category() {
        if (!(getClass().equals(CategoryEntity.class))) {
            works = new ArrayList<>();
            links = new ArrayList<>();
        }
    }

    public CategoryEntity createEntity() {
        if (getClass() == CategoryEntity.class) return (CategoryEntity) this;
        CategoryEntity entity = new CategoryEntity();
        entity.setAnnotation(annotation);
        entity.setAuthor(author);
        entity.setId(id);
        entity.setLink(link);
        entity.setParsed(parsed);
        entity.setTitle(title);
        entity.setType(type);
        for (Work work : works) {
            work.setCategory(entity);
            work.setAuthor(entity.getAuthor());
            entity.getWorks().add(work.createEntity());
        }
        for (Link link1 : links) {
            link1.setCategory(entity);
            entity.getLinks().add(link1.createEntity());
        }
        return entity;
    }

    public void setTitle(String title) {
        if (title == null) return;
        title = TextUtils.trim(title);
        if(title.endsWith(":")) {
            title = title.substring(0, title.length() - 1);
        }
        this.title = title;
    }

    public String getLink() {
        if (link != null && !link.contains(author.getLink())) {
            link = author.getLink() + "/" + link;
        }
        return link;
    }

    public void addLink(Linkable linkable) {
        if (linkable instanceof Work) {
            this.getWorks().add((Work) linkable);
        }
        if (linkable instanceof Link) {
            this.getLinks().add((Link) linkable);
        }
    }

    public Linkable getLinkable() {
        if (type == type.OTHER) {
            if (link == null) return new Link(title, "", annotation);
            else
                return new Link(title, getLink(), annotation);
        } else {
            return type;
        }
    }

    public List<Linkable> getLinkables() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;

        Category category = (Category) o;

        if (title != null ? !title.equals(category.title) : category.title != null) {
            return false;
        }
        if (category.getAuthor() != null && getAuthor() != null) {
            String linkCategory = category.getAuthor().getLink();
            String link = getAuthor().getLink();
            return link != null ? link.equals(linkCategory) : linkCategory == null;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (link != null ? link.hashCode() : 0);
        return result;
    }
}
