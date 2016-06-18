package ru.samlib.client.domain.entity;

import io.requery.*;
import lombok.Data;
import lombok.NoArgsConstructor;
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


    String title;
    String link;
    String annotation;

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
