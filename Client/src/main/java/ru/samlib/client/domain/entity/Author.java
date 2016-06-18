package ru.samlib.client.domain.entity;

import android.net.Uri;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;
import lombok.*;
import ru.samlib.client.database.AppDatabase;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.util.TextUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by Rufim on 22.05.2014.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Table(database = AppDatabase.class)
public class Author extends BaseModel implements Serializable, Linkable, Validatable, Parsable {

    private static final long serialVersionUID = -2312409864781561240L;

    private static final String AVATAR = ".photo2.jpg";

    @PrimaryKey(autoincrement = true)
    private Integer id;
    @Column
    private String link;
    @Column
    private String fullName;;
    @Column
    private String shortName;
    @Column
    private String email;
    @Column
    private String annotation;
    @Column
    private Gender gender;
    @Column
    private Date dateBirth;
    @Column
    private String address;
    @Column
    private Link site;
    @Column
    private boolean isNew = false;
    @Column
    private boolean hasUpdates = false;
    @Column
    private boolean hasAvatar = false;
    @Column
    private boolean hasAbout = false;
    @Column
    private Date lastUpdateDate;
    @Column
    private Integer size;
    @Column
    private Integer workCount;
    @Column
    private BigDecimal rate;
    @Column
    private Integer kudoed;
    @Column
    private Integer views;
    @Column
    private String about;
    @Column
    private String sectionAnnotation;
    @Column
    private Integer friends;
    @Column
    private Integer friendsOf;


    private boolean parsed = false;

    private List<Work> recommendations = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private List<Linkable> rootLinks = new ArrayList<>();

    //TODO:Need to parse, view and make many to many
    private List<Author> friendList = new ArrayList<>();
    private List<Author> friendOfList = new ArrayList<>();

    public Author() {};

    public Author(String link) {
        setLink(link);
    }

    public void setLink(String link) {
        if (link == null) return;
        link = TextUtils.eraseHost(link);
        if (link.contains("/")) {
            if(link.startsWith(Work.COMMENT_PREFIX)) {
                link = link.replace(Work.COMMENT_PREFIX, "");
            }
            if(link.startsWith(Work.ILLUSTRATION_PREFIX)) {
                link = link.replace(Work.ILLUSTRATION_PREFIX, "");
            }
            if(link.contains(Work.HTML_SUFFIX)) {
                this.link = new Work(link).getAuthor().getLink();
            } else {
                this.link = link;
            }
        }
    }

    public Gender getGender(){
        if(gender != null) return gender;
        String lastName = null;
        if (shortName == null && fullName != null) {
            String names[] = fullName.split(" ");
            lastName = names[0];
        }
        if (shortName != null && fullName == null) {
            String names[] = shortName.split(" ");
            lastName = names[0];
        }
        gender = Gender.parseLastName(lastName);
        return gender;
    }

    public String getShortName() {
        if (shortName == null && fullName != null) {
            StringBuilder builder = new StringBuilder();
            String authors[] = fullName.split(",");
            for (int i = 0; i < authors.length; i++) {
                String names[] = authors[i].split(" ");
                builder.append(names[0]);
                for (int j = 1; j < names.length; j++) {
                    if (!names[j].isEmpty()) {
                        builder.append(" " + names[j].charAt(0) + ".");
                    }
                }
                if(i + 1 < authors.length) {
                    builder.append(",");
                }
            }
            return shortName = builder.toString();
        }
        return shortName;
    }

    public List<Category> getLinkableCategory() {
        return Stream.of(categories)
                .filter(sec -> sec.getLink() != null)
                .collect(Collectors.toList());
    }

    public List<Category> getStaticCategory() {
        return Stream.of(categories)
                .filter(sec -> sec.getLink() == null)
                .collect(Collectors.toList());
    }


    public void addCategory(Category category) {
        this.categories.add(category);
    }

    public void addFriend(Author friend) {
        this.friendList.add(friend);
    }

    public void addFriendOf(Author friend) {
        this.friendOfList.add(friend);
    }

    public List<Work> getRecommendations() {
        return recommendations;
    }

    public void addRecommendation(Work work) {
        this.recommendations.add(work);
    }


    public void addRootLink(Linkable linkable) {
        this.rootLinks.add(linkable);
    }


    @Override
    public boolean validate() {
        return (fullName != null || shortName != null) && link != null;
    }

    @Override
    public String toString() {
        return getShortName();
    }


    @Override
    public String getTitle() {
        return getShortName();
    }

    public String getImageLink() {
        return getFullLink() + AVATAR;
    }
}
