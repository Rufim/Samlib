package ru.samlib.client.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.util.Log;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Bookmark;
import ru.samlib.client.domain.entity.Work;

import java.sql.SQLException;

/**
 * Created by aleksandr on 30.12.15.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private Dao<Bookmark, Integer>  bookmarksDao = null;
    private Dao<Work, Integer> workDao = null;
    private Dao<Author, Integer> authorDao = null;

    public DatabaseHelper(Context context) {
        super(context, Constants.App.DATABASE_NAME, null, Constants.App.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Bookmark.class);
            TableUtils.createTable(connectionSource, Work.class);
        } catch (SQLException e) {
            Log.e(TAG, "error creating DB " + Constants.App.DATABASE_NAME);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVer,
                          int newVer) {
        try {
            TableUtils.dropTable(connectionSource, Bookmark.class, true);
            TableUtils.dropTable(connectionSource, Work.class, true);
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e(TAG, "error upgrading db " + Constants.App.DATABASE_NAME + "from ver " + oldVer);
            throw new RuntimeException(e);
        }
    }

    public Dao<Bookmark, Integer> getBookmarkDao() throws SQLException {
        if (bookmarksDao == null) {
            bookmarksDao = getDao(Bookmark.class);
        }
        return bookmarksDao;
    }

    public Dao<Work, Integer> getWorkDao() throws SQLException {
        if (workDao == null) {
            workDao = getDao(Work.class);
        }
        return workDao;
    }

    public Dao<Author, Integer> getAuthorDaoDao() throws SQLException {
        if (authorDao == null) {
            authorDao = getDao(Author.class);
        }
        return authorDao;
    }

    @Override
    public void close() {
        workDao = null;
        bookmarksDao = null;
        authorDao = null;
        super.close();
    }

    public static void close(DatabaseHelper databaseHelper) {
        if(databaseHelper != null) {
            databaseHelper.close();
        }
    }
}