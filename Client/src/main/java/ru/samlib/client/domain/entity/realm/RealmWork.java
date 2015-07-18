package ru.samlib.client.domain.entity.realm;

import android.os.Parcelable;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import lombok.*;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.domain.entity.New;
import ru.samlib.client.domain.entity.Type;
import ru.samlib.client.util.RealmUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Rufim on 22.05.2014.
 */
public class RealmWork extends RealmObject{
    private String title;
    private String link;
    private RealmAuthor author;
    private String imageLink;
    private boolean hasIllustration;
    private String sectionTitle;
    private int size;
    private int kudoed;
    private int expertKudoed;
    private Date createDate;
    private Date updateDate;
    private String rawContent;
    private String description;
    @Ignore
    private BigDecimal rate;
    private String rateValue;
    @Ignore
    private BigDecimal expertRate;
    private String expertRateValue;
    @Ignore
    private Type type = Type.OTHER;
    private String typeValue;
    @Ignore
    private New state = New.EMPTY;
    private String stateValue;
    @Ignore
    private List<String> genres = new ArrayList<>();
    private RealmList<RealmString> genresValues;
    @Ignore
    private List<String> annotationBlocks = new ArrayList<>();
    private RealmList<RealmString> annotationBlocksValues;

    public BigDecimal getRate() {
        if (rate == null) {
            rate = new BigDecimal(rateValue);
        }
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
        setRateValue(rate.toString());
    }

    public BigDecimal getExpertRate() {
        if (expertRate == null) {
            expertRate = new BigDecimal(expertRateValue);
        }
        return expertRate;
    }

    public void setExpertRate(BigDecimal expertRate) {
        this.expertRate = expertRate;
        setExpertRateValue(expertRate.toString());
    }
    
    public Type getType() {
        if (type == null) {
            type = type.parseType(typeValue);
        }
        return type;
    }

    public void setType(Type type) {
        this.type = type;
        typeValue = type.getTitle();
    }

    public New getState() {
        if (state == null) {
            state = state.parseNew(stateValue);
        }
        return state;
    }

    public void setState(New state) {
        this.state = state;
        stateValue = state.name();
    }

    public List<String> getGenres() {
        if(genres == null) {
            genres = RealmUtils.fromRealmStringList(genresValues);
        }
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
        genresValues = RealmUtils.toRealmStringList(genres);
    }

    public List<String> getAnnotationBlocks() {
        if (annotationBlocks == null) {
            annotationBlocks = RealmUtils.fromRealmStringList(annotationBlocksValues);
        }
        return annotationBlocks;
    }

    public void setAnnotationBlocks(List<String> annotationBlocks) {
        this.annotationBlocks = annotationBlocks;
        annotationBlocksValues = RealmUtils.toRealmStringList(annotationBlocks);
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

    public RealmAuthor getAuthor() {
        return author;
    }

    public void setAuthor(RealmAuthor author) {
        this.author = author;
    }

    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    public boolean isHasIllustration() {
        return hasIllustration;
    }

    public void setHasIllustration(boolean hasIllustration) {
        this.hasIllustration = hasIllustration;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getKudoed() {
        return kudoed;
    }

    public void setKudoed(int kudoed) {
        this.kudoed = kudoed;
    }

    public int getExpertKudoed() {
        return expertKudoed;
    }

    public void setExpertKudoed(int expertKudoed) {
        this.expertKudoed = expertKudoed;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTypeValue() {
        return typeValue;
    }

    public void setTypeValue(String typeValue) {
        this.typeValue = typeValue;
    }

    public String getStateValue() {
        return stateValue;
    }

    public void setStateValue(String stateValue) {
        this.stateValue = stateValue;
    }

    public RealmList<RealmString> getGenresValues() {
        return genresValues;
    }

    public void setGenresValues(RealmList<RealmString> genresValues) {
        this.genresValues = genresValues;
    }

    public RealmList<RealmString> getAnnotationBlocksValues() {
        return annotationBlocksValues;
    }

    public void setAnnotationBlocksValues(RealmList<RealmString> annotationBlocksValues) {
        this.annotationBlocksValues = annotationBlocksValues;
    }

    public String getRateValue() {
        return rateValue;
    }

    public void setRateValue(String rateValue) {
        this.rateValue = rateValue;
    }

    public String getExpertRateValue() {
        return expertRateValue;
    }

    public void setExpertRateValue(String expertRateValue) {
        this.expertRateValue = expertRateValue;
    }
}
