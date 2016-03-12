package ru.samlib.client.database;

import android.content.Context;
import android.util.Log;
import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;
import ru.samlib.client.domain.entity.Bookmark;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.util.SystemUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 0shad on 27.12.2015.
 */
public class SnappyHelper {

    private static final String TAG = SnappyHelper.class.getSimpleName();

    private static final String WORK_KEY_NAME = "work";
    private static final String SAVED_POSITION_KEY_NAME = "saved_position";
    private static final int MAX_HOLD_TIMEOUT = 5000;
    private static final AtomicInteger currentHoldTimeout = new AtomicInteger(0);
    private static volatile SnappyHelper holder;
    private static volatile DB snappyDB;
    private final Context context;
    private final String trackTag;

    public SnappyHelper(Context context) {
        this(context, null);
    }

    public SnappyHelper(Context context, String trackTag) {
        this.context = context;
        this.trackTag = trackTag;
        if (holder == null) {
            holder = this;
        }
    }


    public SnappyHelper storeSerializable(Serializable value) throws SnappydbException {
        return storeSerializable(value.getClass().getCanonicalName(), value);
    }

    public SnappyHelper storeSerializable(String key, Serializable value) throws SnappydbException {
        open();
        snappyDB.put(key, value);
        return this;
    }

    public SnappyHelper putSerializable(String key, Serializable value) throws SnappydbException {
        open();
        int keys = snappyDB.findKeys(key).length;
        snappyDB.put(indexKey(key, keys), value);
        return this;
    }

    public SnappyHelper setSerializable(String key, int index, Serializable value) throws SnappydbException {
        open();
        snappyDB.put(indexKey(key, index), value);
        return this;
    }

    public <T extends Serializable> T getSerializable(String key, int index, Class<T> type) throws SnappydbException {
        return getSerializable(indexKey(key, index), type);
    }

    public <T extends Serializable> T getSerializable(String key, int index, Class<T> type, T defaultValue) throws SnappydbException {
        T value = getSerializable(key, index, type);
        return value == null ? defaultValue : value;
    }


    public <T extends Serializable> T getSerializable(Class<T> type) throws SnappydbException {
        return getSerializable(type.getCanonicalName(), type);
    }

    public <T extends Serializable> T getSerializable(Class<T> type, T defaultValue) throws SnappydbException {
        return getSerializable(type.getCanonicalName(), type, defaultValue);
    }

    public <T extends Serializable> T getSerializable(String key, Class<T> type) throws SnappydbException {
        open();
        if (snappyDB.exists(key))
            return snappyDB.get(key, type);
        else
            return null;
    }

    public <T extends Serializable> T getSerializable(String key, Class<T> type, T defaultValue) throws SnappydbException {
        T value = getSerializable(key, type);
        return value == null ? defaultValue : value;
    }

    private static String indexKey(String key, int index) {
        return key + ":" + String.format("%10d", index);
    }

    private synchronized void open() throws SnappydbException {
        if (holder != this) {
            while (holder != null) {
                SystemUtils.sleepQuietly(100);
                currentHoldTimeout.set(currentHoldTimeout.intValue() - 100);
                if (currentHoldTimeout.intValue() < 0) {
                    if (holder != null) {
                        Log.e(TAG, "Close unused holder. Holder Track Tag = " + holder.trackTag);
                        holder.close();
                    }
                    holder = this;
                    break;
                }
            }
            holder = this;
        }
        currentHoldTimeout.set(MAX_HOLD_TIMEOUT);
        if (snappyDB == null || !snappyDB.isOpen()) {
            snappyDB = DBFactory.open(context);
        }
    }

    public synchronized void close() {
        try {
            if (snappyDB != null && snappyDB.isOpen()) {
                snappyDB.close();
            }
        } catch (SnappydbException e) {
            Log.e(TAG, "Unknown exception while closing snappyDB. Track Tag = " + trackTag, e);
        } finally {
            holder = null;
        }
    }

    public static void close(SnappyHelper helper) {
        if (helper != null) {
            helper.close();
        }
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

    public SnappyHelper putSavedPosition(Bookmark bookmark, Work work) throws SnappydbException {
        open();
        snappyDB.put(SAVED_POSITION_KEY_NAME + ":" + work.getLink(), bookmark);
        return this;
    }

    public Bookmark getSavedPosition(Work work) throws SnappydbException {
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
}
