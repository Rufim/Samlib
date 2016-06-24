package ru.samlib.client.domain.entity;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import io.requery.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.util.TextUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Rufim on 22.05.2014.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
public class Author implements Serializable, Linkable, Validatable, Parsable {

    private static final long serialVersionUID = -2312409864781561240L;

    private static final String AVATAR = ".photo2.jpg";

    @Key @Generated
    Integer id;
    String link;
    String fullName;;
    String shortName;
    String email;
    String annotation;
    Gender gender;
    Date dateBirth;
    String address;
    @OneToOne(mappedBy = "authorSite")
    Link site;
    boolean hasAvatar = false;
    boolean hasAbout = false;
    boolean hasUpdates = false;
    boolean isNew = false;
    Date lastUpdateDate;
    Integer size;
    Integer workCount;
    BigDecimal rate;
    Integer kudoed;
    Integer views;
    String about;
    String sectionAnnotation;
    @OneToMany
    List<Work> recommendations = new ArrayList<>();
    @OneToMany
    List<Category> categories = new ArrayList<>();
    @OneToMany(mappedBy = "author")
    List<Link> rootLinks = new ArrayList<>();
    @OneToMany
    List<Work> rootWorks = new ArrayList<>();
    @ManyToMany
    List<Author> friendList = new ArrayList<>();
    @ManyToMany
    List<Author> friendOfList = new ArrayList<>();
    Integer friends;
    Integer friendsOf;

    @Transient
    boolean parsed = false;

    public Author(Author other) {
        this.id = other.id;
        this.link = other.link;
        this.fullName = other.fullName;
        this.shortName = other.shortName;
        this.email = other.email;
        this.annotation = other.annotation;
        this.gender = other.gender;
        this.dateBirth = other.dateBirth;
        this.address = other.address;
        this.site = other.site;
        this.hasAvatar = other.hasAvatar;
        this.hasAbout = other.hasAbout;
        this.hasUpdates = other.hasUpdates;
        this.isNew = other.isNew;
        this.lastUpdateDate = other.lastUpdateDate;
        this.size = other.size;
        this.workCount = other.workCount;
        this.rate = other.rate;
        this.kudoed = other.kudoed;
        this.views = other.views;
        this.about = other.about;
        this.sectionAnnotation = other.sectionAnnotation;
        this.recommendations = other.recommendations;
        this.categories = other.categories;
        this.rootLinks = other.rootLinks;
        this.rootWorks = other.rootWorks;
        this.friendList = other.friendList;
        this.friendOfList = other.friendOfList;
        this.friends = other.friends;
        this.friendsOf = other.friendsOf;
        this.parsed = other.parsed;
    }

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
        if(linkable instanceof Work) {
            this.rootWorks.add((Work) linkable);
        }
        if(linkable instanceof Link) {
            this.rootLinks.add((Link) linkable);
        }
    }

    public List<Linkable> getLinkables()  {
        List<Linkable> linkables = new ArrayList<>();
        linkables.addAll(rootLinks);
        linkables.addAll(rootWorks);
        return linkables;
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


    public boolean hasCategory(Category category) {
        for (Category next : getCategories()) {
            if (next.title != null ? !next.title.equals(category.title) : category.title != null) continue;
            if (next.annotation != null ? !next.annotation.equals(category.annotation) : category.annotation != null) continue;
            if (next.author != null ? !next.author.equals(category.author) : category.author != null) continue;
            if (next.type != category.type) continue;
            if(!(next.link != null ? next.link.equals(category.link) : category.link == null)) {
                return true;
            }
        }
        return false;
    }
}
