package ru.samlib.client.domain.events;

import ru.samlib.client.domain.entity.Chapter;

/**
 * Created by Dmitry on 24.07.2015.
 */
public class ChapterSelectedEvent implements Event {

    public final Chapter chapter;

    public ChapterSelectedEvent(Chapter chapter) {
        this.chapter = chapter;
    }
}
