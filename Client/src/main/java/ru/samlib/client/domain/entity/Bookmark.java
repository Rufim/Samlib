package ru.samlib.client.domain.entity;


import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.kazantsev.template.domain.Findable;
import ru.samlib.client.database.MyDatabase;
import ru.samlib.client.fragments.FilterDialogListFragment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by 0shad on 23.07.2015.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(database = MyDatabase.class, allFields = true)
public class Bookmark extends BaseModel implements Serializable, Findable {
    @PrimaryKey
    String workUrl;
    String authorUrl;
    String title;
    Double percent = 0d;
    Integer indentIndex = 0;
    String indent;
    String workTitle;
    String genres;
    String authorShortName;
    Date savedDate;

    public Bookmark(){};

    public Bookmark(String title){
        this.title = title;
    }

    public String toString() {
        if(title != null) {
            return getTitle();
        }
        if(indent != null) {
            return Jsoup.parse(indent).text();
        }
        return "Без Имени";
    }

    @Override
    public boolean find(ItemListAdapter.FilterEvent query) {
        FilterDialogListFragment.FilterEvent filterQuery = (FilterDialogListFragment.FilterEvent) query;
        ArrayList<Genre> genres = filterQuery.genres;
        String stringQuery = filterQuery.query;
        if(stringQuery != null) {
            stringQuery = stringQuery.toLowerCase();
        }
        boolean result = false;
        if (stringQuery == null || (authorShortName + " " + workTitle).toLowerCase().contains(stringQuery)) {
            if (genres == null && filterQuery.genders == null) {
                result = true;
            }
            if (genres != null) {
                List<Genre> genreList = new ArrayList<>();
                for (String s : this.genres.split(",")) {
                    genreList.add(Genre.parseGenre(s));
                }
                result = Collections.disjoint(genres, genreList);
                if (!filterQuery.excluding) result = !result;
            }
            if (!result) return result;
            if (filterQuery.genders != null && filterQuery.genders.size() != Gender.values().length) {
                Gender gender =  Gender.parseLastName(authorShortName.split(" ")[0]);
                if (filterQuery.excluding) result = !filterQuery.genders.contains(gender);
                else result = filterQuery.genders.contains(gender);
            }
            return result;
        }
        return false;
    }
}
