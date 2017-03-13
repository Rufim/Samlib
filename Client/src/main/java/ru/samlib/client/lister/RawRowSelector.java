package ru.samlib.client.lister;

import org.intellij.lang.annotations.RegExp;

/**
 * Created by 0shad on 29.10.2015.
 */
public abstract class RawRowSelector implements RowSelector {
    public abstract @RegExp String getRowStartDelimiter();

    public abstract @RegExp String getRowEndDelimiter();
}
