package ru.samlib.client.domain.entity;

import android.graphics.Color;
import android.text.TextUtils;
import lombok.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.Findable;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.domain.events.FilterEvent;
import ru.samlib.client.net.CachedResponse;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Filter;
import java.util.regex.Pattern;

/**
 * Created by Rufim on 22.05.2014.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = {"rawContent", "rootElements", "chapters", "annotationBlocks"})
public final class Work implements Serializable, Linkable, Validatable, Parsable, Findable {

    private static final long serialVersionUID = -2705011939329628695L;
    public static final String HTML_SUFFIX = ".shtml";
    public static final String FB2_SUFFIX = ".fb2.zip";

    private String title;
    private String link;
    private Author author;
    private String imageLink;
    private Integer size;
    private BigDecimal rate;
    private Integer kudoed;
    private BigDecimal expertRate;
    private Integer expertKudoed;
    @Setter(AccessLevel.NONE)
    private List<Genre> genres = new ArrayList<>();
    private Type type = Type.OTHER;
    private Category category;
    @Setter(AccessLevel.NONE)
    private List<String> annotationBlocks = new ArrayList<>();
    private Date createDate;
    private Date updateDate;
    private New state = New.EMPTY;
    private String description;
    private boolean hasIllustration = false;
    private boolean hasComments = true;
    private boolean parsed = false;
    private String rawContent = "";
    private List<String> indents = new ArrayList<>();
    private CachedResponse cachedResponse;
    private transient Elements rootElements;
    private transient List<Chapter> chapters = new ArrayList<>();

    public Work(String link) {
        setLink(link);
    }

    public void setLink(String link) {
        if (link.contains("/")) {
            if (author == null) {
                author = new Author(link.substring(link.indexOf("/"), link.lastIndexOf("/")));
            }
            this.link = link.substring(link.lastIndexOf("/"));
        } else {
            this.link = link;
        }
    }

    public String getLink() {
        return author.getLink() + "/" + link;
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
            genres = new ArrayList<Genre>();
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

    public String getShortFormattedDate(Date date, Locale locale) {
        Calendar calendarToday = Calendar.getInstance();
        Calendar calendarDate = Calendar.getInstance();
        calendarDate.setTime(date);
        if (calendarToday.get(Calendar.DAY_OF_WEEK) == calendarDate.get(Calendar.DAY_OF_WEEK)) {
            return new SimpleDateFormat("HH:mm", locale).format(date);
        } else {
            return new SimpleDateFormat("dd/MM", locale).format(date);
        }
    }

    @Override
    public boolean validate() {
        return author != null && author.validate() && title != null && link != null;
    }


    @Override
    public boolean find(Object query) {
        String stringQuery;
        if (query.getClass() == FilterEvent.class) {
            ArrayList<Genre> genres = ((FilterEvent) query).genres;
            stringQuery = ((FilterEvent) query).query;
            if (stringQuery == null || toString().toLowerCase().contains(stringQuery)) {
                if (((FilterEvent) query).excluding) {
                    return Collections.disjoint(genres, this.genres);
                } else {
                    return genres.containsAll(this.genres);
                }
            }
        } else {
            stringQuery = query.toString().toLowerCase();
            if (toString().toLowerCase().contains(stringQuery)) {
                return true;
            }
        }
        return false;
    }
}
