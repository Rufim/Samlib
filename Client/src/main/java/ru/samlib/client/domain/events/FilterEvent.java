package ru.samlib.client.domain.events;

import ru.samlib.client.domain.Findable;
import ru.samlib.client.domain.entity.Genre;

import java.util.ArrayList;

/**
 * Created by 0shad on 25.10.2015.
 */
public class FilterEvent implements Event {

    public final ArrayList<Genre> genres;
    public final boolean excluding;
    public String query;

    public FilterEvent(ArrayList<Genre> genres, boolean excluding) {
        this.genres = genres;
        this.excluding = excluding;
    }

    public FilterEvent(String query) {
        this.query = query;
        this.genres = null;
        this.excluding = false;
    }
}
