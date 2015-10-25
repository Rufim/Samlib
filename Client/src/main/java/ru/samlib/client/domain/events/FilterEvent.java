package ru.samlib.client.domain.events;

import ru.samlib.client.domain.entity.Gender;
import ru.samlib.client.domain.entity.Genre;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Created by 0shad on 25.10.2015.
 */
public class FilterEvent implements Event {

    public final ArrayList<Genre> genres;
    public final boolean excluding;
    public final EnumSet<Gender> genders;
    public String query;

    public FilterEvent(ArrayList<Genre> genres, EnumSet<Gender> genders, boolean excluding) {
        this.genres = genres;
        this.excluding = excluding;
        this.genders = genders;
    }

    public FilterEvent(ArrayList<Genre> genres, boolean excluding) {
        this.genres = genres;
        this.excluding = excluding;
        this.genders = null;
    }

    public FilterEvent(String query) {
        this.query = query;
        this.genres = null;
        this.excluding = false;
        this.genders = null;
    }
}
