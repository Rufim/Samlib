package ru.samlib.client.domain.entity.realm;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import lombok.*;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.domain.entity.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 01.07.2015.
 */
public class RealmSection extends RealmObject {
    private String title;
    private String annotation;
    private RealmList<RealmLink> links;
    private String link;
    @Ignore
    private Type type;
    private String typeValue;

    public Type getType() {
        if(type != null) {
            type = type.parseType(typeValue);
        }
        return type;
    }

    public void setType(Type type) {
        this.type = type;
        typeValue = type.getTitle();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public RealmList<RealmLink> getLinks() {
        return links;
    }

    public void setLinks(RealmList<RealmLink> links) {
        this.links = links;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getTypeValue() {
        return typeValue;
    }

    public void setTypeValue(String typeValue) {
        this.typeValue = typeValue;
    }
}
