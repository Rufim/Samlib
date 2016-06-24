package ru.samlib.client.domain.events;

import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.AuthorEntity;

/**
 * Created by Dmitry on 24.06.2016.
 */
public class AuthorUpdatedEvent implements Event {

    public final AuthorEntity author;

    public AuthorUpdatedEvent(AuthorEntity author) {
        this.author = author;
    }

}
