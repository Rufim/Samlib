package ru.samlib.client.domain.entity;


import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.samlib.client.database.MyDatabase;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Validatable;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Rufim on 01.07.2015.
 */
@Data
@Table(database = MyDatabase.class, allFields = true)
public class Link extends BaseModel implements Validatable, Linkable, Serializable {

    @PrimaryKey(autoincrement = true, quickCheckAutoIncrement = true)
    Integer id = 0;
    @ForeignKey(stubbedRelationship = true, onUpdate = ForeignKeyAction.CASCADE, onDelete = ForeignKeyAction.CASCADE)
    Author author;
    @ForeignKey(stubbedRelationship = true, onUpdate = ForeignKeyAction.CASCADE, onDelete = ForeignKeyAction.CASCADE)
    Category category;

    boolean rootLink = false;

    String title;
    String link;
    String annotation;

    public Link(){}

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
