package ru.samlib.client.domain.entity;

import android.graphics.Color;


import com.raizlabs.android.dbflow.annotation.*;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.database.MyDatabase;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rufim on 01.07.2015.
 */
@Data
@Table(database = MyDatabase.class, allFields = true)
public class Category implements Linkable, Serializable, Parsable {

    private static final long serialVersionUID = 6549621729790810154L;

    @PrimaryKey(autoincrement = true)
    Integer id;

    String title;
    String annotation;
    @ForeignKey
    Author author;
    Type type = Type.OTHER;
    //@OneToMany(cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<Work> works;
    //@OneToMany(cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<Link> links;
    String link;

    @ColumnIgnore
    boolean parsed = false;
    @ColumnIgnore
    boolean inUIExpanded = false;

    public Category() {
    }

    public List<Work> getOriginalWorks() {
        return works;
    }

    public List<Link> getOriginalLinks() {
        return links;
    }

    @OneToMany(methods = OneToMany.Method.ALL, variableName = "businesses")
    public List<Work> loadWorks() {
        if (works == null) {
            works = SQLite.select()
                    .from(Work.class)
                   // .where(Work_Table.userId.eq(id))
                    .queryList();
        }
        return works;
    }

    public boolean isEntity() {
        return true;
    }

    public void setTitle(String title) {
        if (title == null) return;
        title = TextUtils.trim(title);
        if (title.endsWith(":")) {
            title = title.substring(0, title.length() - 1);
        }
        this.title = title;
    }

    public String getLink() {
        if (link != null && !link.contains(author.getLink())) {
            link = author.getLink() + "/" + link;
        }
        return link;
    }

    public void addLink(Linkable linkable) {
        if (linkable instanceof Work) {
            this.getWorks().add((Work) linkable);
        }
        if (linkable instanceof Link) {
            this.getLinks().add((Link) linkable);
        }
    }

    public Linkable getLinkable() {
        if (type == type.OTHER) {
            if (link == null) return new Link(title, "", annotation);
            else
                return new Link(title, getLink(), annotation);
        } else {
            return type;
        }
    }

    public List<Linkable> getLinkables() {
        List<Linkable> linkables = new ArrayList<>();
        linkables.addAll(getWorks());
        linkables.addAll(getLinks());
        return linkables;
    }

    public void setParsed(boolean parsed) {
        if (link != null) {
            this.parsed = parsed;
        }
    }

    public String processAnnotation(int color) {
        Document an = Jsoup.parse(annotation);
        an.select("font[color=#393939]").attr("color",
                String.format("#%02x%02x%02x",
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)));
        an.select("dd").unwrap();
        return an.body().html();
    }

    public String getTitle() {
        return getLinkable().getTitle();
    }

    @Override
    public String toString() {
        return getTitle();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category category = (Category) o;
        return isTitleEquals(this, category) && isLinkEquals(this, category);
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (link != null ? link.hashCode() : 0);
        return result;
    }

    public static boolean isTitleEquals(Category one, Category two) {
        if (one.getTitle() == null && two.getTitle() == null) {
            return true;
        }
        if (one.getTitle() == null || two.getTitle() == null) {
            return false;
        }
        return TextUtils.trim(one.getTitle()).equalsIgnoreCase(TextUtils.trim(two.getTitle()));
    }

    public static boolean isLinkEquals(Category one, Category two) {
        if (one.getLink() == null && two.getLink() == null) {
            return true;
        }
        if (one.getLink() == null || two.getLink() == null) {
            return false;
        }
        return TextUtils.trim(one.getLink()).equalsIgnoreCase(TextUtils.trim(two.getLink()));
    }

    public boolean isHasUpdates() {
        for (Work work : getWorks()) {
            if(work.isChanged() || (work.getSizeDiff() != null && work.getSizeDiff() > 0)) {
                return true;
            }
        }
        return false;
    }
}
