package ru.samlib.client.domain.entity;

import android.os.Parcel;
import android.os.Parcelable;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import lombok.*;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Validatable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by Rufim on 22.05.2014.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class Author implements Serializable, Linkable, Validatable {

    private static final long serialVersionUID = -2312409864781561240L;

    private static final String AVATAR = ".photo2.jpg";

    private String link;
    private String fullName;;
    private String shortName;
    private String email;
    private String annotation;
    private Date dateBirth;
    private String address;
    private Link site;
    private boolean hasAvatar = false;
    private boolean hasAbout = false;
    private boolean parsed = false;
    private Date lastUpdateDate;
    private Integer size;
    private Integer workCount;
    private BigDecimal rate;
    private Integer kudoed;
    private Integer views;
    private String about;
    private String sectionAnnotation;
    @Setter(AccessLevel.NONE)
    private List<Work> recommendations = new ArrayList<>();
    private Boolean isNew;
    @Setter(AccessLevel.NONE)
    private List<Section> sections = new ArrayList<>();
    @Setter(AccessLevel.NONE)
    private List<Linkable> rootLinks = new ArrayList<>();
    @Setter(AccessLevel.NONE)
    private List<Author> friendList = new ArrayList<>();
    @Setter(AccessLevel.NONE)
    private List<Author> friendOfList = new ArrayList<>();
    private Integer friends;
    private Integer friendsOf;

    public Author(String link) {
        this.link = link;
    }

    public String getShortName() {
        if (shortName == null && fullName != null) {
            String names[] = fullName.split(" ");
            shortName = names[0];
            for (int i = 1; i < names.length; i++) {
                if (!names[i].isEmpty()) {
                    shortName += " " + names[i].charAt(0) + ".";
                }
            }
        }
        return shortName;
    }

    public List<Section> getLinkableSections() {
        return Stream.of(sections)
                .filter(sec -> sec.getLink() != null)
                .collect(Collectors.toList());
    }

    public List<Section> getStaticSections() {
        return Stream.of(sections)
                .filter(sec -> sec.getLink() == null)
                .collect(Collectors.toList());
    }


    public void addSection(Section section) {
        this.sections.add(section);
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
