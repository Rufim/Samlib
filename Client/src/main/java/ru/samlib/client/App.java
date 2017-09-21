package ru.samlib.client;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.support.multidex.MultiDexApplication;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.util.JobApi;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowLog;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.converter.BigDecimalConverter;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import dagger.Module;








import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.config.ConfigurationBuilder;
import ru.samlib.client.dagger.AppComponent;
import ru.samlib.client.dagger.AppModule;
import ru.samlib.client.dagger.DaggerAppComponent;
import ru.samlib.client.database.ListConverter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Font;
import ru.samlib.client.job.AppJobCreator;
import ru.samlib.client.job.CleanCacheJob;
import ru.samlib.client.job.ObservableUpdateJob;
import ru.samlib.client.service.DatabaseService;
import ru.samlib.client.util.MergeFromRequery;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

import java.io.File;
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
        FlowManager.init(new FlowConfig.Builder(this).build());
        FlowLog.setMinimumLoggingLevel(FlowLog.Level.V);
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
        return databaseService == null ? new DatabaseService() : databaseService;
    }

    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }


    public AppComponent getComponent() {
        return component;
    }
}
