package ru.samlib.client.dagger;

import dagger.Component;
import ru.samlib.client.activity.MainActivity;
import ru.samlib.client.fragments.AuthorFragment;
import ru.samlib.client.fragments.HistoryFragment;
import ru.samlib.client.fragments.ObservableFragment;
import ru.samlib.client.fragments.WorkFragment;
import ru.samlib.client.job.ObservableUpdateJob;
import ru.samlib.client.service.DatabaseService;

import javax.inject.Singleton;

/**
 * Created by Dmitry on 28.06.2016.
 */
@Singleton
@Component(modules={AppModule.class})
public interface AppComponent {
    void inject(MainActivity activity);
    void inject(AuthorFragment fragment);
    void inject(ObservableFragment fragment);
    void inject(DatabaseService service);
    void inject(ObservableUpdateJob job);
    void inject(WorkFragment workFragment);
    void inject(HistoryFragment historyFragment);
}
