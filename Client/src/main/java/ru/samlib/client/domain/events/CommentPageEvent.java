package ru.samlib.client.domain.events;

import ru.kazantsev.template.domain.event.Event;

/**
 * Created by 0shad on 05.11.2015.
 */
public class CommentPageEvent implements Event {
    public final int pageIndex;

    public CommentPageEvent(int pageIndex) {
        this.pageIndex = pageIndex;
    }
}
