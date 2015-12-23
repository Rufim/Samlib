package ru.samlib.client.domain.events;

/**
 * Created by 0shad on 05.11.2015.
 */
public class CommentPageEvent implements Event {
    public final int pageIndex;

    public CommentPageEvent(int pageIndex) {
        this.pageIndex = pageIndex;
    }
}
