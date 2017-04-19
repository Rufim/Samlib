package ru.samlib.client.domain.events;

import ru.kazantsev.template.domain.event.Event;

/**
 * Created by Dmitry on 03.11.2015.
 */
public class CommentsParsedEvent implements Event {

    final public int lastPage;

    public CommentsParsedEvent(int lastPage) {
        this.lastPage = lastPage;
    }
}
