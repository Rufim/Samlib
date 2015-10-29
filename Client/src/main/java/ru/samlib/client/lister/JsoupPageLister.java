package ru.samlib.client.lister;

/**
 * Created by 0shad on 29.10.2015.
 */
public abstract class JsoupPageLister implements PageLister{
    public abstract String getRowSelector();
}
