package ru.samlib.client.domain.entity;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import io.requery.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.domain.Findable;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Parsable;
import ru.samlib.client.domain.Validatable;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.domain.google.Page;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Rufim on 22.05.2014.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
public class Author implements Serializable, Linkable, Validatable, Parsable, Findable {

    private static final long serialVersionUID = -2312409864781561240L;

    private static final String AVATAR = ".photo2.jpg";

    @Key
    String link;
    String fullName;;
    String shortName;
    String email;
    String annotation;
    Gender gender;
    Date dateBirth;
    String address;
    String authorSiteUrl;
    boolean hasAvatar = false;
    boolean hasAbout = false;
    boolean hasUpdates = false;
    boolean newest = false;
    boolean notNotified = false;
    boolean observable = false;
    Date lastUpdateDate;
    Integer size;
    Integer workCount;
    BigDecimal rate;
    Integer kudoed;
    Integer views;
    String about;
    String sectionAnnotation;
    @OneToMany(mappedBy = "author",cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<Category> categories;
    @OneToMany(mappedBy = "author", cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<Link> links;
    @OneToMany(mappedBy = "author", cascade = {CascadeAction.DELETE, CascadeAction.SAVE})
    List<Work> works;
    @Transient
    List<Author> friendList  = new ArrayList<>();
    @Transient
    List<Author> friendOfList  = new ArrayList<>();
    Integer friends;
    Integer friendsOf;

    @Transient
    boolean parsed = false;

    public Author() {
        if(!isEntity()) {
            categories = new ArrayList<>();
            works = new ArrayList<>();
            links = new ArrayList<>();
        }
    }

    public Author(Author other) {
        this.link = other.getLink();
        this.fullName = other.getFullName();
        this.shortName = other.getShortName();
        this.email = other.getEmail();
        this.annotation = other.getAnnotation();
        this.gender = other.getGender();
        this.dateBirth = other.getDateBirth();
        this.address = other.getAddress();
        this.authorSiteUrl = other.getAuthorSiteUrl();
        this.hasAvatar = other.isHasAbout();
        this.hasAbout = other.isHasAbout();
        this.hasUpdates = other.isHasUpdates();
        this.observable = other.isObservable();
        this.lastUpdateDate = other.getLastUpdateDate();
        this.newest = other.isNewest();
        this.size = other.getSize();
        this.workCount = other.getWorkCount();
        this.rate = other.getRate();
        this.kudoed = other.getKudoed();
        this.views = other.getViews();
        this.about = other.getAbout();
        this.sectionAnnotation = other.sectionAnnotation;
        this.categories = other.getCategories();
        this.friendList = other.getFriendList();
        this.friendOfList = other.getFriendOfList();
        this.friends = other.getFriends();
        this.friendsOf = other.getFriendsOf();
        this.parsed = other.isParsed();
    }

    public AuthorEntity createEntity() {
        if(isEntity()) return (AuthorEntity) this;
        AuthorEntity entity = new AuthorEntity();
        entity.setHasUpdates(hasUpdates);
        entity.setHasAvatar(hasAvatar);
        entity.setHasAbout(hasAbout);
        entity.setObservable(observable);
        entity.setDateBirth(dateBirth);
        entity.setRate(rate);
        entity.setSize(size);
        entity.setKudoed(kudoed);
        entity.setLink(link);
        entity.setNewest(newest);
        entity.setShortName(getShortName());
        entity.setFullName(fullName);
        entity.setAnnotation(annotation);
        entity.setEmail(email);
        entity.setWorkCount(workCount);
        entity.setSectionAnnotation(sectionAnnotation);
        entity.setGender(getGender());
        entity.setAbout(about);
        entity.setViews(views);
        entity.setAddress(address);
        entity.setParsed(parsed);
        entity.setAuthorSiteUrl(authorSiteUrl);
        entity.setLastUpdateDate(lastUpdateDate);
        for (Category category : categories) {
            category.createEntity(entity);
        }
        for (Work rootWork : getRootWorks()) {
            entity.addRootLink(rootWork.createEntity(entity, null));
        }
        for (Link rootLink : getRootLinks()) {
            entity.addRootLink(rootLink.createEntity(entity, null));
        }
        for (Author author : friendList) {
            entity.addFriend(author.createEntity());
        }
        for (Author author : friendOfList) {
            entity.addFriendOf(author.createEntity());
        }
        return entity;
    }

    public boolean isEntity() {
        return getClass() == AuthorEntity.class;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public Author(String link) {
        this();
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
        if(this.link != null && !this.link.endsWith("/")) {
            this.link += "/";
        }
        this.link = this.link.replace("//", "/");
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
        return Stream.of(getCategories())
                .filter(sec -> sec.getLink() != null)
                .collect(Collectors.toList());
    }

    public List<Category> getStaticCategory() {
        return Stream.of(getCategories())
                .filter(sec -> sec.getLink() == null)
                .collect(Collectors.toList());
    }

    public void addCategory(Category category) {
        this.getCategories().add(category);
    }

    public void addFriend(Author friend) {
        this.getFriendList().add(friend);
    }

    public void addFriendOf(Author friend) {
        this.getFriendOfList().add(friend);
    }

    public void addRecommendation(Work work) {
        int workRecomanndationIndex = getWorks().indexOf(work);
        if(workRecomanndationIndex < 0) {
            work.setRecommendation(true);
            if(!isEntity()) {
                this.getWorks().add(work);
            } else {
                getWorks().add(work.createEntity((AuthorEntity) this, null));
            }
        } else {
            getWorks().get(workRecomanndationIndex).setRecommendation(true);
        }
    }


    public void addRootLink(Linkable linkable) {
        if(linkable instanceof Work) {
            ((Work)linkable).setRootWork(true);
            this.getWorks().add((Work) linkable);
        }
        if(linkable instanceof Link) {
            ((Link)linkable).setRootLink(true);
            this.getLinks().add((Link) linkable);
        }
    }

    public List<Linkable> getLinkables()  {
        List<Linkable> linkables = new ArrayList<>();
        if (getRootLinks() != null) {
            linkables.addAll(getRootWorks());
        }
        if (getRootWorks() != null) {
            linkables.addAll(getRootLinks());
        }
        return linkables;
    }

    public List<Link> getRootLinks() {
        return Stream.of(getLinks()).filter(Link::isRootLink).collect(Collectors.toList());
    }

    public List<Work> getRootWorks() {
        return Stream.of(getWorks()).filter(Work::isRootWork).collect(Collectors.toList());
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

    @Override
    public boolean find(ItemListAdapter.FilterEvent query) {
        String shortName = getShortName().toLowerCase();
        if(shortName == null) return false;
        if(query.query == null) return true;
        return shortName.toLowerCase().contains(query.query.toLowerCase());
    }

    public void hasNewUpdates(){
        setHasUpdates(true);
        setNotNotified(true);
    }

    public void setRootLinks(List<Link> rootLinks) {
        for (Link rootLink : rootLinks) {
            rootLink.setRootLink(true);
        }
        if(getLinks() == null) setLinks(new ArrayList<>());
        getLinks().addAll(rootLinks);
    }

    public void setRootWorks(List<Work> rootWorks) {
        for (Work rootLink : rootWorks) {
            rootLink.setRootWork(true);
        }
        if(getWorks() == null) setWorks(new ArrayList<>());
        getWorks().addAll(rootWorks);
    }

    public List<Work> getRecommendations() {
        return Stream.of(getWorks()).filter(Work::isRecommendation).collect(Collectors.toList());
    }
}
