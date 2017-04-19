package ru.samlib.client.domain.events;

import ru.kazantsev.template.domain.event.Event;

/**
 * Created by 0shad on 03.11.2015.
 */
public class ScrollToCommentEvent implements Event {

    public final int index;
    public final int pageIndex;

    public ScrollToCommentEvent(int index, int pageIndex) {
        this.index = index;
        this.pageIndex = pageIndex;
    }
}
