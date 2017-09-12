package ru.samlib.client.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.raizlabs.android.dbflow.sql.language.SQLite;
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


    public static void merge(Context context, DatabaseService databaseService) {
// load subscriptions from Requery
        File oldDatabase;
        if ((oldDatabase = context.getDatabasePath("default")).exists()) {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(oldDatabase.getAbsolutePath(), null, 0);
            db.beginTransaction();
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
            boolean hasOldData = false;
            while (cursor.moveToNext()) {
                if ("Author".equals(cursor.getString(0))) {
                    hasOldData = true;
                    break;
                }
            }
            if (hasOldData) {
                boolean authorsCopied = false;
                if (AndroidSystemUtils.isNetworkAvailable(context)) {
                    cursor = db.rawQuery("SELECT link FROM Author;", null);
                    while (cursor.moveToNext()) {
                        Author author = new Author(cursor.getString(0));
                        if (!author.exists()) {
                            try {
                                databaseService.insertObservableAuthor(new AuthorParser(author).parse());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    authorsCopied  = true;
                }
                cursor = db.rawQuery(" SELECT * FROM Bookmark;", null);
                while (cursor.moveToNext()) {
                    Bookmark bookmark = getFromCursor(cursor);
                    if (!bookmark.exists()) {
                        bookmark.insert();
                    }
                }
                if(authorsCopied) {
                    db.execSQL("DROP TABLE Bookmark;");
                    db.execSQL("DROP TABLE Author;");
                    db.execSQL("DROP TABLE SavedHtml;");
                    db.execSQL("DROP TABLE Category;");
                    db.execSQL("DROP TABLE ExternalWork;");
                    db.execSQL("DROP TABLE Work;");
                    db.execSQL("DROP TABLE Link;");
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
