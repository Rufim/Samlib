package ru.samlib.client.database;

import com.raizlabs.android.dbflow.annotation.Database;
import ru.samlib.client.domain.Constants;

/**
 * Created by 0shad on 17.06.2016.
 */
@Database(name = AppDatabase.NAME, version = AppDatabase.VERSION)
public class AppDatabase {

    public static final String NAME = Constants.App.DATABASE_NAME; // we will add the .db extension

    public static final int VERSION = Constants.App.DATABASE_VERSION;
}