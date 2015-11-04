package ru.samlib.client.domain.events;

import ru.samlib.client.domain.entity.Image;

/**
 * Created by 0shad on 03.11.2015.
 */
public class IllustrationSelectedEvent implements Event {

    public final int index;

    public IllustrationSelectedEvent(int index) {
        this.index = index;
    }
}
