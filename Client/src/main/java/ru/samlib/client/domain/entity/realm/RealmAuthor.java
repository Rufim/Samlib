package ru.samlib.client.domain.entity.realm;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import lombok.*;
import ru.samlib.client.domain.entity.Section;
import ru.samlib.client.domain.entity.Work;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by Rufim on 22.05.2014.
 */
public class RealmAuthor extends RealmObject {
    private String title;
    private String link;
    private String fullName;
    private String shortName;
    private String email;
    private Date dateBirth;
    private String address;
    private RealmLink site;
    private boolean hasAvatar = false;
    private boolean hasAbout = false;
    private Date lastUpdateDate;
    private int size;
    private int workCount;
    private String rateValue;
    @Ignore
    private BigDecimal rate;
    private int kudoed;
    private int views;
    private String about;
    private String annotation;
    private String sectionAnnotation;
    private String comment;
    private boolean isNew;
    private int friends;
    private int friendsOf;
    private RealmList<RealmWork> recommendations;
    private RealmList<RealmSection> sections;
    private RealmList<RealmLink> rootLinks;
    private RealmList<RealmAuthor> friendList;
    private RealmList<RealmAuthor> friendOfList;

    public RealmAuthor(){};

    public BigDecimal getRate() {
        if(rate == null) {
            rate = new BigDecimal(rateValue);
        }
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
        setRateValue(rate.toString());
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getDateBirth() {
        return dateBirth;
    }

    public void setDateBirth(Date dateBirth) {
        this.dateBirth = dateBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public RealmLink getSite() {
        return site;
    }

    public void setSite(RealmLink site) {
        this.site = site;
    }

    public boolean isHasAvatar() {
        return hasAvatar;
    }

    public void setHasAvatar(boolean hasAvatar) {
        this.hasAvatar = hasAvatar;
    }

    public boolean isHasAbout() {
        return hasAbout;
    }

    public void setHasAbout(boolean hasAbout) {
        this.hasAbout = hasAbout;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getWorkCount() {
        return workCount;
    }

    public void setWorkCount(int workCount) {
        this.workCount = workCount;
    }

    public String getRateValue() {
        return rateValue;
    }

    public void setRateValue(String rateValue) {
        this.rateValue = rateValue;
    }

    public int getKudoed() {
        return kudoed;
    }

    public void setKudoed(int kudoed) {
        this.kudoed = kudoed;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public String getSectionAnnotation() {
        return sectionAnnotation;
    }

    public void setSectionAnnotation(String sectionAnnotation) {
        this.sectionAnnotation = sectionAnnotation;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public int getFriends() {
        return friends;
    }

    public void setFriends(int friends) {
        this.friends = friends;
    }

    public int getFriendsOf() {
        return friendsOf;
    }

    public void setFriendsOf(int friendsOf) {
        this.friendsOf = friendsOf;
    }

    public RealmList<RealmWork> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(RealmList<RealmWork> recommendations) {
        this.recommendations = recommendations;
    }

    public RealmList<RealmSection> getSections() {
        return sections;
    }

    public void setSections(RealmList<RealmSection> sections) {
        this.sections = sections;
    }

    public RealmList<RealmLink> getRootLinks() {
        return rootLinks;
    }

    public void setRootLinks(RealmList<RealmLink> rootLinks) {
        this.rootLinks = rootLinks;
    }

    public RealmList<RealmAuthor> getFriendList() {
        return friendList;
    }

    public void setFriendList(RealmList<RealmAuthor> friendList) {
        this.friendList = friendList;
    }

    public RealmList<RealmAuthor> getFriendOfList() {
        return friendOfList;
    }

    public void setFriendOfList(RealmList<RealmAuthor> friendOfList) {
        this.friendOfList = friendOfList;
    }
}
