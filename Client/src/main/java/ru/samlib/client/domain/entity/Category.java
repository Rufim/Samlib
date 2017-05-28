package ru.samlib.client.domain.entity;

import android.graphics.Color;
import io.requery.*;
import io.requery.sql.MissingKeyException;
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
    @ManyToOne
    Author author;
    Type type = Type.OTHER;
    @OneToMany(cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<Work> works;
    @OneToMany(cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<Link> links;
    String link;

    @Transient
    boolean parsed = false;
    @Transient
    boolean inUIExpanded = false;

    public Category() {
        if (!(getClass().equals(CategoryEntity.class))) {
            works = new ArrayList<>();
            links = new ArrayList<>();
        }
    }

    public Integer getIdNoDB() {
        if (id != null) return id;
        try {
            id = getId();
        } catch (MissingKeyException ex) {
            id = null;
        }
        return id;
    }

    public CategoryEntity createEntity(AuthorEntity authorEntity) {
        CategoryEntity entity;
        if (isEntity()) {
            entity = (CategoryEntity) this;
        } else {
            entity = new CategoryEntity();
        }
        setAuthor(author = authorEntity == null ? getAuthor() : authorEntity);
        if (authorEntity != null) {
            if (authorEntity.getCategories() == null) {
                authorEntity.setCategories(new ArrayList<>());
            }
            boolean found = false;
            for (int i = 0; i < authorEntity.getCategories().size(); i++) {
                Category category = authorEntity.getCategories().get(i);
                if (category.equals(this)) {
                    found = true;
                    if (category.isEntity()) {
                        entity = (CategoryEntity) category;
                    } else {
                        authorEntity.getCategories().set(i, entity);
                    }
                }
            }
            if (!found) {
                authorEntity.getCategories().add(entity);
            }
        }
        if (isEntity()) {
            return entity;
        }
        entity.setAnnotation(annotation);
        entity.setId(id);
        entity.setLink(link);
        entity.setParsed(parsed);
        entity.setTitle(getTitle());
        entity.setType(type);
        for (Work work : works) {
            work.createEntity(getAuthor().createEntity(), entity);
        }
        for (Link link1 : links) {
            link1.createEntity(getAuthor().createEntity(), entity);
        }
        return entity;
    }

    public CategoryEntity createEntity() {
        return createEntity(author != null ? author.createEntity() : null);
    }

    public List<Work> getOriginalWorks() {
        return works;
    }

    public List<Link> getOriginalLinks() {
        return links;
    }

    public void setTitle(String title) {
        if (title == null) return;
        title = TextUtils.trim(title);
        if (title.endsWith(":")) {
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
        return getTitle();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category category = (Category) o;
        return isTitleEquals(this, category) && isLinkEquals(this, category);
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (link != null ? link.hashCode() : 0);
        return result;
    }

    public boolean isEntity() {
        return getClass() == CategoryEntity.class;
    }

    public static boolean isTitleEquals(Category one, Category two) {
        if (one.getTitle() == null && two.getTitle() == null) {
            return true;
        }
        if (one.getTitle() == null || two.getTitle() == null) {
            return false;
        }
        return TextUtils.trim(one.getTitle()).equalsIgnoreCase(TextUtils.trim(two.getTitle()));
    }

    public static boolean isLinkEquals(Category one, Category two) {
        if (one.getLink() == null && two.getLink() == null) {
            return true;
        }
        if (one.getLink() == null || two.getLink() == null) {
            return false;
        }
        return TextUtils.trim(one.getLink()).equalsIgnoreCase(TextUtils.trim(two.getLink()));
    }

    public boolean isHasUpdates() {
        for (Work work : getWorks()) {
            if(work.isChanged() || (work.getSizeDiff() != null && work.getSizeDiff() > 0)) {
                return true;
            }
        }
        return false;
    }
}
