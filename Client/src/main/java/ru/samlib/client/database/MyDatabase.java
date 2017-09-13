package ru.samlib.client.database;


import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.Database;
import ru.samlib.client.domain.Constants;

@Database(name = Constants.App.DATABASE_NAME, version = Constants.App.VERSION, insertConflict = ConflictAction.REPLACE, updateConflict = ConflictAction.REPLACE)
public class MyDatabase {
}
