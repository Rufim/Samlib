package ru.samlib.client.lister;

import java.io.IOException;
import java.util.List;

/**
 * Created by 0shad on 01.11.2015.
 */
public interface PageDataSource<I>{
    public abstract List<I> getPage(int index) throws IOException;
}
