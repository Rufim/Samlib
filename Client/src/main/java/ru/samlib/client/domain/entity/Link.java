package ru.samlib.client.domain.entity;

import io.requery.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Validatable;

import java.io.Serializable;

/**
 * Created by Rufim on 01.07.2015.
 */
@NoArgsConstructor
@Data
@Entity
public class Link implements Validatable, Linkable, Serializable {

    @Key @Generated
    Integer id;

    @ForeignKey
    @OneToOne(mappedBy = "site")
    Author authorSite;

    @ManyToOne
    Author author;
    @ManyToOne
    Category category;

    String title;
    String link;
    String annotation;

    public LinkEntity createEntity() {
        if(getClass() == LinkEntity.class) return (LinkEntity) this;
        LinkEntity entity = new LinkEntity();
        entity.setAnnotation(annotation);
        entity.setAuthor(author);
        entity.setAuthorSite(authorSite);
        entity.setId(id);
        entity.setLink(link);
        entity.setTitle(title);
        entity.setCategory(category);
        return entity;
    }

    public Link(String title, String link, String annotation) {
        this.title = title;
        this.link = link;
        this.annotation = annotation;
    }

    public Link(String link) {
        this.link = link;
    }

    @Override
    public String toString() {
        return link;
    }

    @Override
    public boolean validate() {
        return link != null && title != null;
    }

}
