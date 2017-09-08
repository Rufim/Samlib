package ru.samlib.client;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDexApplication;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.util.JobApi;
import dagger.Module;
import io.requery.Persistable;
import io.requery.android.DefaultMapping;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.cache.EntityCacheBuilder;
import io.requery.rx.RxSupport;
import io.requery.rx.SingleEntityStore;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;
import io.requery.sql.TableCreationMode;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import ru.samlib.client.dagger.AppComponent;
import ru.samlib.client.dagger.AppModule;
import ru.samlib.client.dagger.DaggerAppComponent;
import ru.samlib.client.database.BigDecimalConverter;
import ru.samlib.client.database.ListConverter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Font;
import ru.samlib.client.domain.entity.Models;
import ru.samlib.client.job.AppJobCreator;
import ru.samlib.client.job.CleanCacheJob;
import ru.samlib.client.job.ObservableUpdateJob;
import ru.samlib.client.service.DatabaseService;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created by Rufim on 03.07.2015.
 */
@Module
@ReportsCrashes(
        mailTo = "0rufim0@gmail.com",
        mode = ReportingInteractionMode.DIALOG,
        resDialogIcon = R.drawable.sad_cat,
        resDialogTheme = R.style.AppTheme_Dialog,
        resDialogTitle = R.string.crash_title_text,
        resDialogText = R.string.crash_text
)
public class App extends MultiDexApplication {

    private static App singleton;

    private EntityDataStore<Persistable> dataStore;
    private JobManager jobManager;
    private DatabaseService databaseService;
    public static App getInstance() {
        return singleton;
    }

    private AppComponent component;

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        jobManager = JobManager.create(this);
        jobManager.addJobCreator(new AppJobCreator());
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath(Constants.Assets.ROBOTO_FONT_PATH)
                .setFontAttrId(R.attr.fontPath)
                .build());
        component = DaggerAppComponent.builder()
                .appModule(new AppModule(this)).build();
        ObservableUpdateJob.startSchedule();
        CleanCacheJob.startSchedule();
        Font.mapFonts(getAssets());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }

    public DatabaseService getDatabaseService() {
        return new DatabaseService(getDataStore());
    }

    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public EntityDataStore<Persistable> getDataStore() {
        if (dataStore == null) {
            // override onUpgrade to handle migrating to a new version
            DatabaseSource source = new DatabaseSource(this, Models.DEFAULT, Constants.App.DATABASE_VERSION);
            if (BuildConfig.DEBUG) {
                // use this in development mode to drop and recreate the tables on every upgrade
                //source.setTableCreationMode(TableCreationMode.DROP_CREATE);
                source.setLoggingEnabled(true);
            }
            Configuration configuration = new ConfigurationBuilder(source, Models.DEFAULT)
                    .useDefaultLogging()
                    .setWriteExecutor(Executors.newSingleThreadExecutor())
                    .setEntityCache(new EntityCacheBuilder(Models.DEFAULT)
                            .useReferenceCache(false)
                            .useSerializableCache(false)
                            .build())
                    .setMapping(source.getConfiguration().getMapping())
                    .build();
           // Configuration configuration = source.getConfiguration();
            ((DefaultMapping)configuration.getMapping()).addConverter(new BigDecimalConverter(), BigDecimal.class);
            ((DefaultMapping) configuration.getMapping()).addConverter(new ListConverter(), List.class);
            dataStore = new EntityDataStore<Persistable>(configuration);
        }
        return dataStore;
    }

    public AppComponent getComponent() {
        return component;
    }
}
