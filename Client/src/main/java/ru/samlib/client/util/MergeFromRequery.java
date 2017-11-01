package ru.samlib.client.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import net.vrallev.android.cat.Cat;
import org.jdom2.Content;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.samlib.client.database.ListConverter;
import ru.samlib.client.database.ListGenreConverter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Bookmark;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.service.DatabaseService;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MergeFromRequery {


    private static final String DATA_FLAG = "dataMergedFromRequery";

    public static void merge(Context context, DatabaseService databaseService) {
        // load subscriptions from Requery
        File oldDatabase;
        SharedPreferences preferences = AndroidSystemUtils.getDefaultPreference(context);
        if ((oldDatabase = context.getDatabasePath("default")).exists() && preferences.getInt(DATA_FLAG, 0) != 2) {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(oldDatabase.getAbsolutePath(), null, 2);
            db.beginTransaction();
            if (preferences.getInt(DATA_FLAG, 0) == 1) {
                db.execSQL("DROP TABLE IF EXISTS Bookmark");
                db.execSQL("DROP TABLE IF EXISTS ExternalWork");
                db.execSQL("DROP TABLE IF EXISTS SavedHtml");
                db.execSQL("DROP TABLE IF EXISTS Work");
                db.execSQL("DROP TABLE IF EXISTS Link");
                db.execSQL("DROP TABLE IF EXISTS Category");
                db.execSQL("DROP TABLE IF EXISTS Author");
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(DATA_FLAG, 2);
                editor.commit();
            } else {
                Cursor cursor = null;
                boolean authorsCopied = false;
                boolean bookmarksCopied = false;
                if (AndroidSystemUtils.isNetworkAvailable(context)) {
                    try {
                        cursor = db.rawQuery("SELECT link FROM Author;", null);
                    } catch (SQLiteException ex) {
                        authorsCopied = true;
                    }
                    if (!authorsCopied) {
                        try {
                            while (cursor.moveToNext()) {
                                Author author = new Author(cursor.getString(0));
                                if (!author.exists()) {
                                    databaseService.insertObservableAuthor(new AuthorParser(author).parse());
                                }
                            }
                            authorsCopied = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    cursor = db.rawQuery(" SELECT * FROM Bookmark;", null);
                } catch (SQLiteException ex) {
                    bookmarksCopied = true;
                }
                if (!bookmarksCopied) {
                    while (cursor.moveToNext()) {
                        Bookmark bookmark = getFromCursor(cursor);
                        if (!bookmark.exists()) {
                            bookmark.insert();
                        }
                    }
                    bookmarksCopied = true;
                }
                if (bookmarksCopied && authorsCopied) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(DATA_FLAG, 1);
                    editor.commit();
                }
            }
            db.endTransaction();
            db.close();
        }
    }

    private static Bookmark getFromCursor(Cursor cursor) {
        Bookmark bookmark = new Bookmark();
        int index_workUrl = cursor.getColumnIndex("workUrl");
        int index_authorUrl = cursor.getColumnIndex("authorUrl");
        int index_title = cursor.getColumnIndex("title");
        int index_percent = cursor.getColumnIndex("percent");
        int index_indentIndex = cursor.getColumnIndex("indentIndex");
        int index_indent = cursor.getColumnIndex("indent");
        int index_workTitle = cursor.getColumnIndex("workTitle");
        int index_genres = cursor.getColumnIndex("genres");
        int index_authorShortName = cursor.getColumnIndex("authorShortName");
        int index_savedDate = cursor.getColumnIndex("savedDate");
        bookmark.setWorkUrl(cursor.getString(index_workUrl));
        bookmark.setAuthorUrl(cursor.getString(index_authorUrl));
        bookmark.setTitle(cursor.getString(index_title));
        bookmark.setPercent(cursor.getDouble(index_percent));
        bookmark.setIndentIndex(cursor.getInt(index_indentIndex));
        bookmark.setIndent(cursor.getString(index_indent));
        bookmark.setWorkTitle(cursor.getString(index_workTitle));
        bookmark.setGenres(cursor.getString(index_genres));
        bookmark.setAuthorShortName(cursor.getString(index_authorShortName));
        try {
            bookmark.setSavedDate(new SimpleDateFormat(Constants.Pattern.DATA_ISO_8601_24H_FULL_FORMAT_WITHOUT_MC).parse(cursor.getString(index_savedDate)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return bookmark;
    }

}
