package ru.samlib.client.domain.events;

import ru.kazantsev.template.domain.event.Event;
import ru.samlib.client.parser.CommentsParser;

/**
 * Created by 0shad on 05.11.2015.
 */
public class CommentSendEvent implements Event {

    public final String name;
    public final String email;
    public final String link;
    public final String comment;
    public final String msgid;
    public final CommentsParser.Operation operation;
    public final Integer indexPage;

    public CommentSendEvent(String name, String email, String link, String comment, String msgid, CommentsParser.Operation operation, Integer indexPage){
        this.name = name;
        this.email = email;
        this.link = link;
        this.comment = comment;
        this.msgid = msgid;
        this.operation = operation;
        this.indexPage = indexPage;
    }
}
