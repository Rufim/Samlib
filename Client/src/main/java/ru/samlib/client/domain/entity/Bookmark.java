package ru.samlib.client.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 23.07.2015.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class Bookmark implements Serializable {
    private String title;
    private float percent = 0;
    private int index = 0;
    private String indent;
    private List<Bookmark> subBookmarks = new ArrayList<>();

    public Bookmark(String title){
        this.title = title;
    }

    public void addSubBookmark(Bookmark bookmark) {
        subBookmarks.add(bookmark);
    }

    public String toString() {
        if(title != null) {
            return title;
        }
        if(indent != null) {
            return Jsoup.parse(indent).text();
        }
        return "Без Имени";
    }
}
