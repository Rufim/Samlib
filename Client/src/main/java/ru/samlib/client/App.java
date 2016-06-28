package ru.samlib.client;

import android.app.Application;

import com.evernote.android.job.JobManager;
import dagger.Module;
import io.requery.Persistable;
import io.requery.android.DefaultMapping;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.rx.RxSupport;
import io.requery.rx.SingleEntityStore;
import io.requery.sql.Configuration;
import io.requery.sql.EntityDataStore;
import io.requery.sql.TableCreationMode;
import ru.samlib.client.dagger.AppModule;
import ru.samlib.client.dagger.DaggerAppComponent;
import ru.samlib.client.database.BigDecimalConverter;
import ru.samlib.client.database.ListConverter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Models;
import ru.samlib.client.job.AppJobCreator;
import ru.samlib.client.dagger.AppComponent;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by Rufim on 03.07.2015.
 */
@Module
public class App extends Application {

    private static App singleton;

    private SingleEntityStore<Persistable> rxDataStore;
    private EntityDataStore<Persistable> dataStore;

    public static App getInstance() {
        return singleton;
    }

    private AppComponent component;

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        JobManager.create(this).addJobCreator(new AppJobCreator());
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath(Constants.Assets.ROBOTO_FONT_PATH)
                .setFontAttrId(R.attr.fontPath)
                .build());
        component = DaggerAppComponent.builder()
                .appModule(new AppModule(singleton)).build();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }


    public EntityDataStore<Persistable> getDataStore() {
        if (dataStore == null) {
            // override onUpgrade to handle migrating to a new version
            DatabaseSource source = new DatabaseSource(this, Models.DEFAULT, 1);
            if (BuildConfig.DEBUG) {
                // use this in development mode to drop and recreate the tables on every upgrade
                source.setTableCreationMode(TableCreationMode.DROP_CREATE);
                source.setLoggingEnabled(true);
            }

            Configuration configuration = source.getConfiguration();
            ((DefaultMapping)configuration.getMapping()).addConverter(new BigDecimalConverter(), BigDecimal.class);
            ((DefaultMapping) configuration.getMapping()).addConverter(new ListConverter(), List.class);
            dataStore = new EntityDataStore<Persistable>(configuration);

            rxDataStore = RxSupport.toReactiveStore(
                    new EntityDataStore<Persistable>(configuration));
        }
        return dataStore;
    }

    public SingleEntityStore<Persistable> getRxDataStore() {
        getDataStore();
        return rxDataStore;
    }

    public AppComponent getComponent() {
        return component;
    }
}
