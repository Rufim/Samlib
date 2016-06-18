package ru.samlib.client.domain.entity;

import android.graphics.Color;
import android.text.TextUtils;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;
import lombok.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import ru.samlib.client.database.AppDatabase;
import ru.samlib.client.domain.Findable;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.fragments.FilterDialogListFragment;
import ru.samlib.client.net.CachedResponse;
import ru.samlib.client.util.SystemUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;


/**
 * Created by Rufim on 22.05.2014.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"rawContent", "rootElements", "chapters", "annotationBlocks", "indents"})
@ToString(exclude = {"rawContent", "rootElements", "chapters", "annotationBlocks", "indents"})
@Table(database = AppDatabase.class)
public class Work extends BaseModel implements Serializable, Linkable, Validatable, Parsable, Findable {

    private static final long serialVersionUID = -2705011939329628695L;
    public static final String HTML_SUFFIX = ".shtml";
    public static final String FB2_SUFFIX = ".fb2.zip";

    public static final String COMMENT_PREFIX = "/comment";
    public static final String ILLUSTRATION_PREFIX = "/img";

    @PrimaryKey(autoincrement = true)
    private Integer id;
    @Column
    private Author author;
    @Column
    private String title;
    @Column
    private String link;
    @Column
    private String imageLink;
    @Column
    private Integer size;
    @Column
    private BigDecimal rate;
    @Column
    private Integer kudoed;
    @Column
    private BigDecimal expertRate;
    @Column
    private Integer expertKudoed;
    @Column
    private List<Genre> genres = new ArrayList<>();
    @Column
    private Type type = Type.OTHER;
    @Column
    private Category category;
    @Column
    private List<String> annotationBlocks = new ArrayList<>();
    @Column
    private Date createDate;
    @Column
    private Date updateDate;
    @Column
    private Date cachedDate;
    @Column
    private New state = New.EMPTY;
    @Column
    private String description;
    @Column
    private boolean hasIllustration = false;
    @Column
    private boolean hasComments = false;
    @Column
    private boolean parsed = false;
    @Column
    private boolean changed = false;


    private CachedResponse cachedResponse;
    private String rawContent = "";
    private List<String> indents = new ArrayList<>();
    private List<Bookmark> autoBookmarks = new ArrayList<>();
    @Column
    private String md5;


    public Work(String link) {
        setLink(link);
    }

    public void setLink(String link) {
        if(link == null) return;
        link = ru.samlib.client.util.TextUtils.eraseHost(link);
        if (link.contains("/")) {
            if (author == null) {
                author = new Author(link.substring(0, link.lastIndexOf("/")));
            }
            this.link = link.substring(link.lastIndexOf("/"));
        } else {
            this.link = "/" + link;
        }
    }

    public String getLink() {
        return author.getLink() + link;
    }

    public Link getIllustrationsLink() {
        return new Link(ILLUSTRATION_PREFIX + author.getLink() + link.replace(HTML_SUFFIX, "/index"+ HTML_SUFFIX));
    }

    public Link getCommentsLink() {
        return new Link(COMMENT_PREFIX + author.getLink() + link.replace(HTML_SUFFIX, ""));
    }

    public String getTypeName() {
        if (category != null) {
            return category.getTitle();
        } else {
            return type.getTitle();
        }
    }

    public String printGenres() {
        if (genres.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Genre genre : genres) {
            if (builder.length() != 0) {
                builder.append(",");
            }
            builder.append(genre.getTitle());
        }
        return builder.toString();
    }

    public void setGenres(String genres) {
        if (this.genres == null) {
            this.genres = new ArrayList<>();
        } else {
            this.genres.clear();
        }
        for (String genre : genres.split(",")) {
            addGenre(genre);
        }
    }

    public void addGenre(String genre) {
        Genre tryGenre = Genre.parseGenre(genre);
        if (tryGenre != null) {
            genres.add(tryGenre);
        } else {
            genres.add(Genre.EMPTY);
        }
    }

    public void addGenre(Genre genre) {
        if (genres == null) {
            genres = new ArrayList<>();
        }
        genres.add(genre);
    }

    public String getAnnotation() {
        return TextUtils.join("", annotationBlocks);
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
    public boolean find(Object query) {
        String stringQuery;
        if (query.getClass() == FilterDialogListFragment.FilterEvent.class) {
            FilterDialogListFragment.FilterEvent filterQuery = (FilterDialogListFragment.FilterEvent) query;
            ArrayList<Genre> genres = filterQuery.genres;
            stringQuery = filterQuery.query;
            boolean result = false;
            if (stringQuery == null || toString().toLowerCase().contains(stringQuery)) {
                if(stringQuery != null) {
                    result = true;
                }
                if(genres != null) {
                    if (filterQuery.excluding) result = Collections.disjoint(genres, this.genres);
                    else result = genres.containsAll(this.genres);
                }
                if(!result) return result;
                if(filterQuery.genders != null && filterQuery.genders.size() != Gender.values().length) {
                    Author author = getAuthor();
                    Gender gender;
                    if(author == null) gender = Gender.UNDEFINED;
                    else gender = author.getGender();
                    if(filterQuery.excluding) result = !filterQuery.genders.contains(gender);
                    else result = filterQuery.genders.contains(gender);
                }
                return result;
            }
        } else {
            stringQuery = query.toString().toLowerCase();
            if (toString().toLowerCase().contains(stringQuery)) {
                return true;
            }
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
