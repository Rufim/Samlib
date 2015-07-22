package ru.samlib.client.domain.events;

import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Work;

/**
 * Created by Dmitry on 22.07.2015.
 */
public class WorkParsedEvent implements Event {

    public final Work work;

    public WorkParsedEvent(Work work) {
        this.work = work;
    }
    
}
