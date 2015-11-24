package ru.samlib.client.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Validatable;

import java.io.Serializable;

/**
 * Created by Rufim on 01.07.2015.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Link implements Validatable, Linkable, Serializable {

    protected static String baseDomain = "";

    protected String title;
    protected String link;
    protected String annotation;

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
