package ru.samlib.client.dagger;

import dagger.Module;
import dagger.Provides;
import io.requery.Persistable;
import io.requery.rx.SingleEntityStore;
import io.requery.sql.EntityDataStore;
import ru.samlib.client.App;
import ru.samlib.client.service.DatabaseService;

import javax.inject.Singleton;

/**
 * Created by Dmitry on 28.06.2016.
 */
@Module
public class AppModule {

    App app;

    public AppModule(App application) {
        app = application;
    }

    @Provides
    @Singleton
    App providesApplication() {
        return app;
    }

    @Provides
    @Singleton
    EntityDataStore<Persistable> provideDataStore() {
        return app.getDataStore();
    }

    @Provides
    @Singleton
    DatabaseService provideObservableService() {
        return new DatabaseService();
    }

}
