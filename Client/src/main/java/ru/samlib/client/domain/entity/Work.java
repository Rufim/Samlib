package ru.samlib.client.domain.entity;

import android.graphics.Color;
import android.os.Parcelable;
import android.text.TextUtils;
import lombok.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Validatable;

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Rufim on 22.05.2014.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public final class Work implements Serializable, Linkable, Validatable {

    private static final long serialVersionUID = -2705011939329628695L;

    private String title;
    private String link;
    private Author author;
    private String imageLink;
    private boolean hasIllustration;
    private String sectionTitle;
    private Integer size;
    private BigDecimal rate;
    private Integer kudoed;
    private BigDecimal expertRate;
    private Integer expertKudoed;
    @Setter(AccessLevel.NONE)
    private List<Genre> genres = new ArrayList<>();
    private Type type = Type.OTHER;
    @Setter(AccessLevel.NONE)
    private List<String> annotationBlocks = new ArrayList<>();
    private Date createDate;
    private Date updateDate;
    private New state = New.EMPTY;
    private String rawContent;
    private String description;


    public String getTypeName() {
        if(sectionTitle != null) {
            return sectionTitle;
        } else {
            return type.getTitle();
        }
    }

    public String printGenres() {
        if(genres.isEmpty()) {
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

    public void addGenre(String genre) {
        Genre tryGenre = Genre.parseGenre(genre);
        if(tryGenre != null) {
            genres.add(tryGenre);
        }
    }

    public void addGenre(Genre genre) {
        if (genres == null) {
            genres = new ArrayList<Genre>();
        }
         genres.add(genre);
    }

    public String processAnnotationBloks(int color) {;
        Document an =Jsoup.parse(TextUtils.join("", annotationBlocks));
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
        if(calendarToday.get(Calendar.DAY_OF_WEEK) == calendarDate.get(Calendar.DAY_OF_WEEK)) {
            return new SimpleDateFormat("HH:mm", locale).format(date);
        } else {
            return new SimpleDateFormat("dd/MM", locale).format(date);
        }
    }

    @Override
    public boolean validate() {
        return author != null && author.validate() && title != null && link != null;
    }
}
