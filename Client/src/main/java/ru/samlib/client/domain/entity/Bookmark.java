package ru.samlib.client.domain.entity;

import com.j256.ormlite.field.DatabaseField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by 0shad on 23.07.2015.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class Bookmark implements Serializable {
    @DatabaseField(generatedId = true)
    private Integer id;
    @DatabaseField
    private String title;
    @DatabaseField
    private float percent = 0;
    @DatabaseField
    private int index = 0;
    @DatabaseField
    private String indent;

    public Bookmark(String title){
        this.title = title;
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
