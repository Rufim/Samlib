package ru.samlib.client.domain.events;

import ru.kazantsev.template.domain.event.Event;
import ru.samlib.client.domain.entity.Category;

/**
 * Created by 0shad on 21.07.2015.
 */
public class CategorySelectedEvent implements Event {

    public final Category category;

    public CategorySelectedEvent(Category category) {
        this.category = category;
    }

}
