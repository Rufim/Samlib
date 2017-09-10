package ru.samlib.client.domain.entity;

import android.util.Log;


import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import lombok.Data;
import ru.kazantsev.template.net.CachedResponse;
import ru.samlib.client.database.MyDatabase;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Date;

/**
 * Created by 0shad on 06.05.2017.
 */


@Table(database = MyDatabase.class, allFields= true)
@Data
public abstract class SavedHtml implements Serializable {

    @PrimaryKey
    String filePath;
    String url;
    long size;
    Date updated;

    protected SavedHtml(){};

    public SavedHtml(CachedResponse response) {
        filePath = response.getAbsolutePath();
    }
}
