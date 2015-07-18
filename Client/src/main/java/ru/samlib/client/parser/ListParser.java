package ru.samlib.client.parser;

import java.util.List;

/**
 * Created by Rufim on 01.07.2015.
 */
public interface ListParser<E> {
    public abstract List<E> getElements(int skip, int size);
}
