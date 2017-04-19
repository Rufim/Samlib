package ru.samlib.client.dagger;

import dagger.Component;
import ru.samlib.client.AppTest;

import javax.inject.Singleton;

/**
 * Created by Admin on 19.04.2017.
 */
@Singleton
@Component(modules={AppModule.class})
public interface TestComponent  {
    void inject(AppTest appTest);
}
