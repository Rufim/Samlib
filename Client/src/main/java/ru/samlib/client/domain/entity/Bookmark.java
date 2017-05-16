package ru.samlib.client.domain.entity;

import io.requery.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by 0shad on 23.07.2015.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
public class Bookmark implements Serializable {
    @Key @Generated
    Integer id;
    String title;
    Double percent = 0d;
    Integer indentIndex = 0;
    String indent;
    @OneToOne
    Work work;

    public Bookmark(String title){
        this.title = title;
    }

    public BookmarkEntity createEntry() {
        if(getClass() == BookmarkEntity.class) return (BookmarkEntity) this;
        BookmarkEntity entity = new BookmarkEntity();
        entity.setIndent(indent);
        entity.setIndentIndex(indentIndex);
        entity.setPercent(percent);
        entity.setTitle(title);
        entity.setId(id);
        entity.setWork(work);
        return entity;
    }

    public String toString() {
        if(title != null) {
            return getTitle();
        }
        if(indent != null) {
            return Jsoup.parse(indent).text();
        }
        return "Без Имени";
    }
}
