package ru.samlib.client.domain.events;

import ru.kazantsev.template.domain.event.Event;

/**
 * Created by 0shad on 05.11.2015.
 */
public class CommentSuccessEvent implements Event {

    public final String name;
    public final String email;
    public final String link;
    public final String comment;

    public CommentSuccessEvent(String name, String email, String link, String comment){
        this.name = name;
        this.email = email;
        this.link = link;
        this.comment = comment;
    }
}
