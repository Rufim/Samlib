package ru.samlib.client.domain.entity;

import lombok.*;
import ru.samlib.client.domain.Linkable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 01.07.2015.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class Section implements Linkable, Serializable {

    private static final long serialVersionUID = 6549621729790810154L;

    private String title;
    private String annotation;
    private Type type = Type.OTHER;
    @Setter(AccessLevel.NONE)
    private List<Linkable> links = new ArrayList<>();
    private String link;

    public void addLink(Linkable link) {
        this.links.add(link);
    }

    public Linkable getLinkable() {
        if(type == type.OTHER && link != null) {
            return new Link(title, link);
        } else {
            return type;
        }
    }
}
