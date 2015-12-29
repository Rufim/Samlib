package ru.samlib.client.domain.events;

import ru.samlib.client.domain.entity.Bookmark;

/**
 * Created by Dmitry on 24.07.2015.
 */
public class ChapterSelectedEvent implements Event {

    public final Bookmark bookmark;

    public ChapterSelectedEvent(Bookmark bookmark) {
        this.bookmark = bookmark;
    }
}
