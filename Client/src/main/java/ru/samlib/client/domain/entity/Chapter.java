package ru.samlib.client.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 23.07.2015.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public  class Chapter {
    private String title;
    private float percent = 0;
    private int index = 0;
    private String content;
    private List<Chapter> subChapters = new ArrayList<>();
    private List<Element> elements = new ArrayList<>();

    public Chapter(String title){
        this.title = title;
    }

    public void addElement(Element element) {
        elements.add(element);
    }

    public void addSubChapter(Chapter chapter) {
        subChapters.add(chapter);
    }
}
