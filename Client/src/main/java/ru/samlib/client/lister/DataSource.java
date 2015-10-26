package ru.samlib.client.lister;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;

/**
 * Created by Rufim on 01.07.2015.
 */
public interface DataSource<E> {
    public abstract List<E> getItems(int skip, int size) throws IOException;
}
