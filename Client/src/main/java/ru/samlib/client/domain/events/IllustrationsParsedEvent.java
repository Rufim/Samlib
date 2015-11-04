package ru.samlib.client.domain.events;

import ru.samlib.client.domain.entity.Image;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dmitry on 03.11.2015.
 */
public class IllustrationsParsedEvent implements Event {

    public final List<Image> images;


    public IllustrationsParsedEvent(List<Image> images) {
        this.images = images;
    }
}
