package ru.samlib.client.lister;

import org.intellij.lang.annotations.RegExp;
import ru.samlib.client.util.TextUtils;

/**
 * Created by 0shad on 29.10.2015.
 */
public abstract class RawPageLister implements PageLister {
    public abstract @RegExp String getRowStartDelimiter();

    public abstract @RegExp String getRowEndDelimiter();
}
