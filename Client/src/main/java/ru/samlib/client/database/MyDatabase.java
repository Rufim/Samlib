package ru.samlib.client.database;


import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.*;

import java.util.Date;

@Database(name = Constants.App.DATABASE_NAME, version = Constants.App.DATABASE_VERSION, insertConflict = ConflictAction.REPLACE, updateConflict = ConflictAction.REPLACE)
public class MyDatabase {
    @Migration(version = 17, database = MyDatabase.class)
    public static class MigrationAuthor extends AlterTableMigration<Author> {


        public MigrationAuthor() {
            super(Author.class);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.get(Boolean.class.getName()), Author_Table.deleted.getNameAlias().nameRaw());
        }
    }

    @Migration(version = 18, database = MyDatabase.class)
    public static class MigrationBookmark extends AlterTableMigration<Bookmark> {


        public MigrationBookmark() {
            super(Bookmark.class);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.get(Boolean.class.getName()), Bookmark_Table.userBookmark.getNameAlias().nameRaw());
        }
    }

    @Migration(version = 20, database = MyDatabase.class)
    public static class MigrationWork extends AlterTableMigration<Work>  {

        public MigrationWork() {
            super(Work.class);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.get(Boolean.class.getName()), Work_Table.hasRate.getNameAlias().nameRaw());
        }
    }

    @Migration(version = 21, database = MyDatabase.class)
    public static class MigrationAuthor21 extends AlterTableMigration<Author> {


        public MigrationAuthor21() {
            super(Author.class);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.get(Date.class.getName()), Author_Table.lastCheckedDate.getNameAlias().nameRaw());
        }
    }
    
}
