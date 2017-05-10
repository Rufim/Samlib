package ru.samlib.client.domain.events;

import ru.kazantsev.template.domain.event.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dmitry on 03.11.2015.
 */
public class CommentsParsedEvent implements Event {

    final public List<Integer> pages;

    public CommentsParsedEvent(List<Integer> pages) {
        this.pages = pages;
    }
}
