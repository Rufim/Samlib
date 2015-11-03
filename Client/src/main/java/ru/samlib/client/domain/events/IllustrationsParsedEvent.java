package ru.samlib.client.domain.events;

import ru.samlib.client.domain.entity.Image;
import java.util.ArrayList;

/**
 * Created by Dmitry on 03.11.2015.
 */
public class IllustrationsParsedEvent implements Event {

    public ArrayList<Image> images;

    public ArrayList<Image> getImages() {
        return images;
    }
}
