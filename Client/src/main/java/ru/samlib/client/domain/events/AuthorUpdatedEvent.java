package ru.samlib.client.domain.events;

import ru.kazantsev.template.domain.event.Event;
import ru.samlib.client.domain.entity.Author;

/**
 * Created by Dmitry on 24.06.2016.
 */
public class AuthorUpdatedEvent implements Event {

    public final Author author;

    public AuthorUpdatedEvent(Author author) {
        this.author = author;
    }

}
