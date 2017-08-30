package ru.samlib.client.domain.entity;

import android.util.Log;
import io.requery.Entity;
import io.requery.Key;
import ru.kazantsev.template.net.CachedResponse;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Date;

/**
 * Created by 0shad on 06.05.2017.
 */


@Entity
public abstract class AbstractSavedHtml implements Serializable {

    @Key
    String filePath;
    String url;
    long size;
    Date updated;

    protected AbstractSavedHtml(){};

    public AbstractSavedHtml(CachedResponse response) {
        filePath = response.getAbsolutePath();
    }
}
