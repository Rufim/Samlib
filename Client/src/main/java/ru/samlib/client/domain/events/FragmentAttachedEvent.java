package ru.samlib.client.domain.events;

import android.support.v4.app.Fragment;
import ru.kazantsev.template.domain.event.Event;

/**
 * Created by 0shad on 21.07.2015.
 */
public class FragmentAttachedEvent implements Event {

    public final Fragment fragment;

    public FragmentAttachedEvent(Fragment fragment) {
        this.fragment = fragment;
    }
}
