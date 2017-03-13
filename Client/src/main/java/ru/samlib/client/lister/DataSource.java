package ru.samlib.client.lister;

import java.io.IOException;
import java.util.List;

/**
 * Created by Rufim on 01.07.2015.
 */
public interface DataSource<I> {
    public abstract List<I> getItems(int skip, int size) throws IOException;
}
