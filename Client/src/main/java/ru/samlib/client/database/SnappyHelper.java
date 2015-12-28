package ru.samlib.client.database;

import android.content.Context;
import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.domain.entity.Work;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by 0shad on 27.12.2015.
 */
public class SnappyHelper {

    private static final String WORK_KEY_NAME = "work";
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
        return snappyDB.get(WORK_KEY_NAME + ":" + link, Work.class);
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
