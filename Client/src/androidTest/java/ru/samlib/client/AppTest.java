package ru.samlib.client;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.samlib.client.dagger.AppModule;
import ru.samlib.client.dagger.DaggerAppComponent;
import ru.samlib.client.dagger.DaggerTestComponent;
import ru.samlib.client.dagger.TestComponent;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.AuthorEntity;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.service.ObservableService;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AppTest {

    App app;

    @Inject
    ObservableService observableService;

    @Before
    public void setUp() {
        App app = (App) InstrumentationRegistry.getTargetContext().getApplicationContext();
        TestComponent component = DaggerTestComponent.builder().appModule(new AppModule(app)).build();
        component.inject(this);
    }

    @Test
    public void observableServiceTest() throws Exception {
        Author author = new AuthorParser("http://samlib.ru/s/sedrik/").parse();
        AuthorEntity entity = observableService.insertAuthor(author.createEntry());
        observableService.deleteAuthor(entity);
    }
}
