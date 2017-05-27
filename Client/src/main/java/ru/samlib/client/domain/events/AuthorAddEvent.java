package ru.samlib.client.domain.events;

import ru.kazantsev.template.domain.event.Event;
import ru.samlib.client.domain.entity.Author;

/**
 * Created by 0shad on 27.05.2017.
 */
public class AuthorAddEvent implements Event {

    public final String link;

    public AuthorAddEvent(String link) {
        this.link = link;
    }
}
