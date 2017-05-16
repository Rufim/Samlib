package ru.samlib.client.domain.entity;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import io.requery.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.domain.Findable;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.fragments.FilterDialogListFragment;
import ru.kazantsev.template.net.CachedResponse;

import java.io.File;
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
    String link;
    String title;
    @ManyToOne
    Author author;
    @ManyToOne
    Author rootAuthor;
    String imageLink;
    Integer size;
    Integer sizeDiff;
    BigDecimal rate;
    Integer kudoed;
    BigDecimal expertRate;
    Integer expertKudoed;
    List<Genre> genres = new ArrayList<>();
    Type type = Type.OTHER;
    @ManyToOne(cascade = CascadeAction.SAVE)
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
    @ForeignKey(update = ReferentialAction.CASCADE)
    @OneToOne(cascade = CascadeAction.SAVE)
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
        WorkEntity entity;
        if (getClass() == WorkEntity.class) {
            entity = (WorkEntity) this;
            entity.setBookmark(bookmark == null ? null : bookmark.createEntry());
            entity.setAuthor(author == null ? null : author.createEntry());
            entity.setRootAuthor(rootAuthor == null ? null : rootAuthor.createEntry());
            entity.setCategory(category == null ? null : category.createEntity());
            if (entity.getCategory() != null) {
                entity.getCategory().setAuthor(entity.getAuthor());
            }
        } else {
            entity = new WorkEntity();
            entity.setTitle(title);
            entity.setLink(getLink());
            entity.setChanged(changed);
            entity.setCreateDate(createDate);
            entity.setDescription(description);
            entity.setExpertKudoed(expertKudoed);
            entity.setExpertRate(expertRate);
            entity.setImageLink(imageLink);
            entity.setBookmark(bookmark == null ? null : bookmark.createEntry());
            entity.setMd5(md5);
            entity.setCachedDate(cachedDate);
            entity.setAuthor(author == null ? null : author.createEntry());
            entity.setRootAuthor(rootAuthor == null ? null : rootAuthor.createEntry());
            entity.setUpdateDate(updateDate);
            entity.setGenres(genres);
            entity.setType(type);
            entity.setHasComments(hasComments);
            entity.setHasIllustration(hasIllustration);
            entity.setAnnotationBlocks(annotationBlocks);
            entity.setSizeDiff(sizeDiff);
            entity.setSize(size);
            entity.setCategory(category == null ? null : category.createEntity());
            if (entity.getCategory() != null) {
                entity.getCategory().setAuthor(entity.getAuthor());
            }
            entity.setKudoed(kudoed);
            entity.setRate(rate);
            entity.setState(state);
        }
        return entity;
    }

    public void setLink(String link) {
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
        this.link = this.link.replace("//", "/");
    }

    public String getLink() {
        if (link != null && !link.contains(getAuthor().getLink())) {
            link = (author.getLink() + link).replace("//", "/");
        }
        return link;
    }

    public String getLinkWithoutSuffix() {
        return getLink().replace(HTML_SUFFIX, "");
    }

    public Author getAuthor() {
        if (author == null) {
            if (getCategory() != null) {
                return author = getCategory().getAuthor();
            }
            if (getRootAuthor() != null) {
                return author = getRootAuthor();
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
        if( Collections.emptyList().equals(getGenres())) {
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


    public void setChanged(boolean changed) {
        this.changed = changed;
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
