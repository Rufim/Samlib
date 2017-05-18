package ru.samlib.client.domain.entity;

import io.requery.Entity;
import io.requery.Key;
import io.requery.ManyToOne;

import java.io.File;
import java.util.Date;

/**
 * Created by 0shad on 17.05.2017.
 */
@Entity
public abstract class AbstractExternalWork {
    @Key
    String filePath;
    Date savedDate;
    @ManyToOne
    Work work;

    public boolean isExist() {
        return new File(filePath == null ? "" : filePath).exists();
    }
}
