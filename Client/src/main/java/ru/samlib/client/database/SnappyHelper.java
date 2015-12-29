package ru.samlib.client.database;

import android.content.Context;
import android.util.Log;
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

    private static final String TAG = SnappyHelper.class.getSimpleName();

    private static final String WORK_KEY_NAME = "work";
    private static final String SAVED_POSITION_KEY_NAME = "saved_position";
    private static DB snappyDB;
    private final Context context;

    public SnappyHelper(Context context) throws SnappydbException {
        this.context = context;
    }

    public SnappyHelper putSerializable(String key, Serializable value) throws SnappydbException {
        open();
        int keys = snappyDB.findKeys(key).length;
        snappyDB.put(key, value + ":" + String.format("%10d", keys));
        return this;
    }

    public SnappyHelper setSerializable(String key, int index, Serializable value) throws SnappydbException {
        open();
        snappyDB.put(key, value + ":" + String.format("%10d", index));
        return this;
    }

    public SnappyHelper putWork(Work work) throws SnappydbException {
        open();
        snappyDB.put(WORK_KEY_NAME + ":" + work.getLink(), work);
        return this;
    }

    public Work getWork(String link) throws SnappydbException {
        open();
        String key = WORK_KEY_NAME + ":" + link;
        if (snappyDB.exists(key))
            return snappyDB.get(key, Work.class);
        else
            return null;
    }

    public SnappyHelper putSavedPostiton(Bookmark bookmark, Work work) throws SnappydbException {
        open();
        snappyDB.put(SAVED_POSITION_KEY_NAME + ":" + work.getLink(), bookmark);
        return this;
    }

    public Bookmark getSavedPostiton(Work work) throws SnappydbException {
        open();
        String key = SAVED_POSITION_KEY_NAME + ":" + work.getLink();
        if (snappyDB.exists(key))
            return snappyDB.get(key, Bookmark.class);
        else
            return null;
    }

    public List<Work> getWorks() throws SnappydbException {
        open();
        String[] keys = snappyDB.findKeys(WORK_KEY_NAME);
        ArrayList<Work> works = new ArrayList<>();
        for (String key : keys) {
            works.add(snappyDB.get(key, Work.class));
        }
        return works;
    }

    private synchronized void open() throws SnappydbException {
        if (snappyDB == null || !snappyDB.isOpen()) {
            snappyDB = DBFactory.open(context);
        }
    }

    public synchronized void close() throws SnappydbException {
        if (snappyDB != null && snappyDB.isOpen()) {
            snappyDB.close();
        }
    }

    public static void close(SnappyHelper helper) {
        if (helper != null) {
            try {
                helper.close();
            } catch (SnappydbException e) {
                Log.e(TAG, "Unknown exception", e);
            }
        }
    }
}
