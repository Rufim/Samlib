package ru.samlib.client.lister;

import java.util.List;

/**
 * Created by Rufim on 01.07.2015.
 */
public interface Lister<E> {
    public abstract List<E> getItems(int skip, int size);
}
