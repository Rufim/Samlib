package ru.samlib.client;

import android.app.Application;

import io.requery.EntityStore;
import io.requery.Persistable;
import io.requery.android.DefaultMapping;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.rx.RxSupport;
import io.requery.rx.SingleEntityStore;
import io.requery.sql.Configuration;
import io.requery.sql.EntityDataStore;
import io.requery.sql.TableCreationMode;
import ru.samlib.client.database.BigDecimalConverter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Models;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

import java.math.BigDecimal;

/**
 * Created by Rufim on 03.07.2015.
 */
public class SamlibApplication extends Application {

    private static SamlibApplication singleton;

    private SingleEntityStore<Persistable> rxDataStore;
    private EntityDataStore<Persistable> dataStore;

    public static SamlibApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath(Constants.Assets.ROBOTO_FONT_PATH)
                .setFontAttrId(R.attr.fontPath)
                .build());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public EntityDataStore<Persistable> getData() {
        if (dataStore == null) {
            // override onUpgrade to handle migrating to a new version
            DatabaseSource source = new DatabaseSource(this, Models.DEFAULT, 1);
            if (BuildConfig.DEBUG) {
                // use this in development mode to drop and recreate the tables on every upgrade
                source.setTableCreationMode(TableCreationMode.DROP_CREATE);
            }

            Configuration configuration = source.getConfiguration();
            ((DefaultMapping)configuration.getMapping()).addConverter(new BigDecimalConverter(), BigDecimal.class);
            dataStore = new EntityDataStore<Persistable>(configuration);

            rxDataStore = RxSupport.toReactiveStore(
                    new EntityDataStore<Persistable>(configuration));
        }
        return dataStore;
    }

    public SingleEntityStore<Persistable> getRxDataStore() {
        return rxDataStore;
    }
}
