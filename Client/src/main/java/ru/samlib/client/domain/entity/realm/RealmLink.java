package ru.samlib.client.domain.entity.realm;

import io.realm.RealmObject;

/**
 * Created by Rufim on 01.07.2015.
 */
public class RealmLink extends RealmObject {
    private String title;
    private String link;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
