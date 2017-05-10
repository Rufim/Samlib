package ru.samlib.client.domain.events;

import ru.kazantsev.template.domain.event.Event;

/**
 * Created by 0shad on 03.11.2015.
 */
public class SelectCommentPageEvent implements Event {

    public final int pageIndex;

    public SelectCommentPageEvent(int pageIndex) {
        this.pageIndex = pageIndex;
    }
}
