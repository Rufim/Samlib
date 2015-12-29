package ru.samlib.client.database;

import android.content.Context;
import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;
import ru.samlib.client.domain.entity.Bookmark;
import ru.samlib.client.domain.entity.Work;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 0shad on 27.12.2015.
 */
public class SnappyHelper {

    private static final String WORK_KEY_NAME = "work";
    private static final String SAVED_POSITION_KEY_NAME = "saved_position";
    private final DB snappyDB;

    public SnappyHelper(Context context) throws SnappydbException {
        this.snappyDB = DBFactory.open(context);
    }

    public SnappyHelper putSerializable(String key,  Serializable value) throws SnappydbException {
        int keys = snappyDB.findKeys(key).length;
        snappyDB.put(key, value + ":" + String.format("%10d", keys));
        return this;
    }

    public SnappyHelper setSerializable(String key, int index, Serializable value) throws SnappydbException {
        snappyDB.put(key, value + ":" + String.format("%10d", index));
        return this;
    }

    public SnappyHelper putWork(Work work) throws SnappydbException {
        snappyDB.put(WORK_KEY_NAME + ":" + work.getLink(), work);
        return this;
    }

    public Work getWork(String link) throws SnappydbException {
        String key = WORK_KEY_NAME + ":" + link;
        if(snappyDB.exists(key))
            return snappyDB.get(key, Work.class);
        else
            return null;
    }

    public SnappyHelper putSavedPostiton(Bookmark bookmark, Work work) throws SnappydbException {
        snappyDB.put(SAVED_POSITION_KEY_NAME + ":" + work.getLink(), bookmark);
        return this;
    }

    public Bookmark getSavedPostiton(Work work) throws SnappydbException {
        String key  = SAVED_POSITION_KEY_NAME + ":" + work.getLink();
        if(snappyDB.exists(key))
            return snappyDB.get(key, Bookmark.class);
        else
            return null;
    }

    public List<Work> getWorks() throws SnappydbException {
        String [] keys =  snappyDB.findKeys(WORK_KEY_NAME);
        ArrayList<Work> works = new ArrayList<>();
        for (String key : keys) {
            works.add(snappyDB.get(key, Work.class));
        }
        return works;
    }

    public void close() throws SnappydbException {
        if (snappyDB != null) {
            snappyDB.close();
        }
    }
}
