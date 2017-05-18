package ru.samlib.client.domain.entity;

import io.requery.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Validatable;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Rufim on 01.07.2015.
 */
@NoArgsConstructor
@Data
@Entity
public class Link implements Validatable, Linkable, Serializable {

    @Key @Generated
    Integer id;
    @ManyToOne
    Author author;
    @ManyToOne
    Category category;

    boolean rootLink = false;

    String title;
    String link;
    String annotation;

    public LinkEntity createEntity(AuthorEntity authorEntity, CategoryEntity categoryEntity) {
        LinkEntity entity = new LinkEntity();
        if (getClass() == LinkEntity.class) {
            entity = (LinkEntity) this;
        } else {
            entity = new LinkEntity();
        }
        if (categoryEntity != null) {
            if (categoryEntity.getLinks() == null) {
                categoryEntity.setLinks(new ArrayList<>());
            }
            boolean found = false;
            for (int i = 0; i < categoryEntity.getLinks().size(); i++) {
                Link link = categoryEntity.getLinks().get(i);
                if (link.getLink().equals(getLink())) {
                    found = true;
                    if (link.getClass() == LinkEntity.class) {
                        entity = (LinkEntity) link;
                    } else {
                        categoryEntity.getLinks().set(i, entity);
                    }
                }
            }
            if (!found) {
                categoryEntity.getLinks().add(entity);
            }
        }
        if(getClass() == LinkEntity.class) {
            setAuthor(author = authorEntity == null ? getAuthor() : authorEntity);
            setCategory(category = categoryEntity == null ? getCategory() : categoryEntity);
            return (LinkEntity) this;
        }
        entity.setAnnotation(annotation);
        entity.setAuthor(author = authorEntity == null ? author : authorEntity);
        entity.setId(id);
        entity.setLink(link);
        entity.setTitle(title);
        entity.setCategory(category = categoryEntity == null ? category : categoryEntity);
        return entity;
    }

    public LinkEntity createEntity() {
        AuthorEntity  authorEntity = author == null ? null : author.createEntity();
        CategoryEntity categoryEntity = null;
        if(authorEntity == null) {
            categoryEntity = category == null ? null : category.createEntity();
        } else {
            categoryEntity = category == null ? null : category.createEntity(authorEntity);
        }
        return createEntity(authorEntity, categoryEntity);
    }

    public Link(String title, String link, String annotation) {
        this.title = title;
        this.link = link;
        this.annotation = annotation;
    }

    public Author getAuthor() {
        if(author == null) {
            if(getCategory() != null) {
                return author = getCategory().getAuthor();
            }
        }
        return author;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Link)) return false;
        Link link = (Link) o;
        return this.link == null ? link.link == null : this.link.equalsIgnoreCase(link.link);
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
