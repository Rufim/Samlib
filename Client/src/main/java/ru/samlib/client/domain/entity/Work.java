package ru.samlib.client.domain.entity;

import android.graphics.Color;
import com.raizlabs.android.dbflow.annotation.*;
import com.raizlabs.android.dbflow.converter.BigDecimalConverter;
import com.raizlabs.android.dbflow.structure.BaseModel;
import ru.kazantsev.template.util.TextUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.domain.Findable;
import ru.samlib.client.database.ListGenreConverter;
import ru.samlib.client.database.ListStringConverter;
import ru.samlib.client.database.MyDatabase;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.fragments.FilterDialogListFragment;
import ru.kazantsev.template.net.CachedResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;


/**
 * Created by Rufim on 22.05.2014.
 */
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"rawContent", "rootElements", "chapters", "annotationBlocks", "indents"})
@ToString(exclude = {"rawContent", "rootElements", "chapters", "annotationBlocks", "indents"})
@Table(database = MyDatabase.class, allFields = true, updateConflict = ConflictAction.REPLACE, insertConflict = ConflictAction.REPLACE)
public class Work extends BaseModel implements Serializable, Linkable, Validatable, Parsable, Findable {

    private static final long serialVersionUID = -2705011939329628695L;
    public static final String HTML_SUFFIX = ".shtml";
    public static final String FB2_SUFFIX = ".fb2.zip";

    public static final String COMMENT_PREFIX = "/comment";
    public static final String ILLUSTRATION_PREFIX = "/img";

    @PrimaryKey
    String link;
    String title;
    @ForeignKey(stubbedRelationship = true, onUpdate = ForeignKeyAction.CASCADE, onDelete = ForeignKeyAction.CASCADE)
    Category category;
    @ForeignKey(stubbedRelationship = true, onUpdate = ForeignKeyAction.CASCADE, onDelete = ForeignKeyAction.CASCADE)
    Author author;
    String imageLink;
    Integer size;
    Integer sizeDiff;
    @Column(typeConverter = BigDecimalConverter.class)
    BigDecimal rate;
    @Column(name = "kudoed")
    Integer votes;
    @Column(typeConverter = BigDecimalConverter.class)
    BigDecimal expertRate;
    @Column(name = "expertKudoed")
    Integer expertVotes;
    @Column(typeConverter = ListGenreConverter.class)
    List<Genre> genres = new ArrayList<>();
    Type type = Type.OTHER;
    @Column(typeConverter = ListStringConverter.class)
    List<String> annotationBlocks = new ArrayList<>();
    Date createDate;
    Date updateDate;
    Date cachedDate;
    Date changedDate;
    New state = New.EMPTY;
    String description;
    boolean hasIllustration = false;
    boolean hasComments = false;
    boolean hasRate = false;
    boolean changed = false;
    boolean recommendation = false;
    boolean rootWork = false;

    String md5;

    @ColumnIgnore
    ExternalWork externalWork;
    @ColumnIgnore
    Bookmark bookmark;
    @ColumnIgnore
    CachedResponse cachedResponse;
    @ColumnIgnore
    String rawContent = "";
    @ColumnIgnore
    List<String> indents = new ArrayList<>();
    @ColumnIgnore
    List<Bookmark> autoBookmarks = new ArrayList<>();
    @ColumnIgnore
    boolean parsed = false;


    public Work(){}

    public Work(String link) {
        setSmartLink(link);
    }

    public void setSmartLink(String link) {
        if (link == null) return;
        link = ru.kazantsev.template.util.TextUtils.eraseHost(link);
        if (link.contains("/")) {
            if (author == null) {
                author = new Author(link.substring(0, link.lastIndexOf("/")));
            }
            this.link = (author.getLink() + link.substring(link.lastIndexOf("/")));
        } else {
            this.link = "/" + link;
        }
        this.link = this.link.replaceAll("/+", "/");
        if(getAuthor() != null) {
            this.link = getLink();
        }
    }

    public String getLink() {
        if (link != null && !link.contains(getAuthor().getLink())) {
            link = (author.getLink() + link).replaceAll("/+", "/");
        }
        return link;
    }

    public boolean isEntity() {
        return exists();
    }


    public boolean isNotSamlib() {
        return getLink() == null;
    }

    public String getLinkWithoutSuffix() {
        return getLink().replace(HTML_SUFFIX, "");
    }

    public Author getAuthor() {
        if (author == null) {
            if (getCategory() != null) {
                return author = getCategory().getAuthor();
            }
        }
        return author;
    }

    public Link getIllustrationsLink() {
        return new Link(ILLUSTRATION_PREFIX + getLink().replace(HTML_SUFFIX, "/index" + HTML_SUFFIX));
    }

    public Link getCommentsLink() {
        return new Link(COMMENT_PREFIX + getLinkWithoutSuffix());
    }

    public String getTypeName() {
        if (getCategory() != null) {
            return getCategory().getTitle();
        } else {
            return getType().getTitle();
        }
    }

    public String printGenres() {
        if (getGenres().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Genre genre : getGenres()) {
            if (builder.length() != 0) {
                builder.append(",");
            }
            builder.append(genre.getTitle());
        }
        return builder.toString();
    }

    public void setGenresAsString(String genres) {
        if (getGenres() == null) {
            setGenres(new ArrayList<>());
        } else {
            getGenres().clear();
        }
        for (String genre : genres.split(",")) {
            addGenre(genre);
        }
    }

    public void addGenre(String genre) {
        Genre tryGenre = Genre.parseGenre(genre);
        if (Collections.emptyList().equals(getGenres())) {
            setGenres(new ArrayList<>());
        }
        if (tryGenre != null) {

            getGenres().add(tryGenre);
        } else {
            getGenres().add(Genre.EMPTY);
        }
    }

    public void addGenre(Genre genre) {
        if (Collections.emptyList().equals(getGenres())) {
            setGenres(new ArrayList<>());
        }
        if (getGenres() == null) {
            setGenres(new ArrayList<>());
        }
        getGenres().add(genre);
    }

    public String getAnnotation() {
        return android.text.TextUtils.join("", annotationBlocks);
    }

    public String processAnnotationBloks(int color) {
        Document an = Jsoup.parse(getAnnotation());
        an.select("font[color=#555555]").attr("color",
                String.format("#%02x%02x%02x",
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)));
        return an.body().html();
    }

    public void addAnnotation(String annotation) {
        this.annotationBlocks.add(annotation);
    }

    @Override
    public boolean validate() {
        return author != null && author.validate() && title != null && link != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Work)) return false;

        Work work = (Work) o;

        if (work.link == null && link == null) return true;
        if (work.link == null || link == null) return false;
        return TextUtils.trim(link).equalsIgnoreCase(TextUtils.trim(work.link));
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    @Override
    public boolean find(ItemListAdapter.FilterEvent query) {
        FilterDialogListFragment.FilterEvent filterQuery = (FilterDialogListFragment.FilterEvent) query;
        ArrayList<Genre> genres = filterQuery.genres;
        String stringQuery = filterQuery.query;
        if(stringQuery != null) {
            stringQuery = stringQuery.toLowerCase();
        }
        boolean result = false;
        if (stringQuery == null || (getAuthor().getShortName() + " " + getTitle()).toLowerCase().contains(stringQuery)) {
            if (genres == null && filterQuery.genders == null) {
                result = true;
            }
            if (genres != null) {
                result = Collections.disjoint(genres, getGenres());
                if (!filterQuery.excluding) result = !result;
            }
            if (!result) return result;
            if (filterQuery.genders != null && filterQuery.genders.size() != Gender.values().length) {
                Author author = getAuthor();
                Gender gender;
                if (author == null) gender = Gender.UNDEFINED;
                else gender = author.getGender();
                if (filterQuery.excluding) result = !filterQuery.genders.contains(gender);
                else result = filterQuery.genders.contains(gender);
            }
            return result;
        }
        return false;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        rawContent = "";
        indents = new ArrayList<>();
        autoBookmarks = new ArrayList<>();
    }
}
