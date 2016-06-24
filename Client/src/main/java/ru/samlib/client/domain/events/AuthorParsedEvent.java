package ru.samlib.client.domain.events;

import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.AuthorEntity;

/**
 * Created by 0shad on 16.07.2015.
 */


public class AuthorParsedEvent implements Event {

    public final Author author;

    public AuthorParsedEvent(Author author) {
        this.author = author;
    }
}
