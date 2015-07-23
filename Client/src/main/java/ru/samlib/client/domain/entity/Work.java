package ru.samlib.client.domain.entity;

import android.graphics.Color;
import android.text.TextUtils;
import com.koushikdutta.async.callback.ListenCallback;
import lombok.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.fragments.ListFragment;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Rufim on 22.05.2014.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public final class Work implements Serializable, Linkable, Validatable, Parsable {

    private static final long serialVersionUID = -2705011939329628695L;
    private static final String HTML_SUFFIX = ".shtml";
    private static final String FB2_SUFFIX = ".fb2.zip";

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
    private String rawContent;
    private String description;
    private boolean hasIllustration = false;
    private boolean hasComments = true;
    private boolean parsed = false;
    private Document parsedContent;
    private List<Chapter> chapters = new ArrayList<>();

    public Work(String link) {
        setLink(link);
    }

    public void addChapter(Chapter chapter) {
        chapters.add(chapter);
    }

    public List<Chapter> getSortedChapters() {
        Collections.sort(chapters, (lhs, rhs) -> (int) (lhs.getPercent() - rhs.getPercent()));
        return chapters;
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
        }
    }

    public void addGenre(Genre genre) {
        if (genres == null) {
            genres = new ArrayList<Genre>();
        }
        genres.add(genre);
    }

    public String processAnnotationBloks(int color) {
        ;
        Document an = Jsoup.parse(TextUtils.join("", annotationBlocks));
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

    public Document getParsedContent() {
        if (parsedContent == null) {
            parsedContent = Jsoup.parseBodyFragment(rawContent);
        }
        return parsedContent;
    }

    @Override
    public boolean validate() {
        return author != null && author.validate() && title != null && link != null;
    }
}
