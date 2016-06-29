package ru.samlib.client.domain.entity;

import android.graphics.Color;
import android.text.TextUtils;
import io.requery.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.domain.Findable;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.fragments.FilterDialogListFragment;
import ru.samlib.client.net.CachedResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * Created by Rufim on 22.05.2014.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"rawContent", "rootElements", "chapters", "annotationBlocks", "indents"})
@ToString(exclude = {"rawContent", "rootElements", "chapters", "annotationBlocks", "indents"})
@Entity
public class Work implements Serializable, Linkable, Validatable, Parsable, Findable {

    private static final long serialVersionUID = -2705011939329628695L;
    public static final String HTML_SUFFIX = ".shtml";
    public static final String FB2_SUFFIX = ".fb2.zip";

    public static final String COMMENT_PREFIX = "/comment";
    public static final String ILLUSTRATION_PREFIX = "/img";

    @Key
    @Generated
    Integer id;
    String title;
    String link;
    @ManyToOne(cascade = {CascadeAction.SAVE, CascadeAction.DELETE})
    Author author;
    String imageLink;
    Integer size;
    Integer sizeDiff;
    BigDecimal rate;
    Integer kudoed;
    BigDecimal expertRate;
    Integer expertKudoed;
    List<Genre> genres = new ArrayList<>();
    Type type = Type.OTHER;
    @ManyToOne(cascade = {CascadeAction.SAVE, CascadeAction.DELETE})
    Category category;
    List<String> annotationBlocks = new ArrayList<>();
    Date createDate;
    Date updateDate;
    Date cachedDate;
    New state = New.EMPTY;
    String description;
    boolean hasIllustration = false;
    boolean hasComments = false;
    boolean changed = false;
    @ForeignKey
    @OneToOne
    Bookmark bookmark;
    String md5;


    @Transient
    CachedResponse cachedResponse;
    @Transient
    String rawContent = "";
    @Transient
    List<String> indents = new ArrayList<>();
    @Transient
    List<Bookmark> autoBookmarks = new ArrayList<>();
    @Transient
    boolean parsed = false;

    public Work(String link) {
        setLink(link);
    }

    public WorkEntity createEntity() {
        if (getClass() == WorkEntity.class) return (WorkEntity) this;
        WorkEntity entity = new WorkEntity();
        entity.setTitle(title);
        entity.setLink(getLink());
        entity.setChanged(changed);
        entity.setCreateDate(createDate);
        entity.setDescription(description);
        entity.setExpertKudoed(expertKudoed);
        entity.setExpertRate(expertRate);
        entity.setImageLink(imageLink);
        entity.setBookmark(bookmark);
        entity.setMd5(md5);
        entity.setCachedDate(cachedDate);
        entity.setAuthor(author);
        entity.setUpdateDate(updateDate);
        entity.setGenres(genres);
        entity.setType(type);
        entity.setHasComments(hasComments);
        entity.setHasIllustration(hasIllustration);
        entity.setAnnotationBlocks(annotationBlocks);
        entity.setId(id);
        entity.setSizeDiff(sizeDiff);
        entity.setSize(size);
        entity.setCategory(category);
        entity.setKudoed(kudoed);
        entity.setRate(rate);
        entity.setState(state);
        return entity;
    }

    public void setLink(String link) {
        if (link == null) return;
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
        if (link != null && !link.contains(author.getLink())) {
            link = author.getLink() + link;
        }
        return link;
    }

    public Link getIllustrationsLink() {
        return new Link(ILLUSTRATION_PREFIX + author.getLink() + link.replace(HTML_SUFFIX, "/index" + HTML_SUFFIX));
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

    public void setGenresAsString(String genres) {
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
    public boolean find(ItemListAdapter.FilterEvent query) {
        FilterDialogListFragment.FilterEvent filterQuery = (FilterDialogListFragment.FilterEvent) query;
        ArrayList<Genre> genres = filterQuery.genres;
        String stringQuery = filterQuery.query;
        boolean result = false;
        if (stringQuery == null || toString().toLowerCase().contains(stringQuery)) {
            if (stringQuery != null) {
                result = true;
            }
            if (genres != null) {
                if (filterQuery.excluding) result = Collections.disjoint(genres, this.genres);
                else result = genres.containsAll(this.genres);
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
