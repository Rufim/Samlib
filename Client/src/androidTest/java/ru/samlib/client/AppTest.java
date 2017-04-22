package ru.samlib.client;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.samlib.client.dagger.AppModule;
import ru.samlib.client.dagger.DaggerTestComponent;
import ru.samlib.client.dagger.TestComponent;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.AuthorEntity;
import ru.samlib.client.parser.AuthorParser;
import ru.samlib.client.service.DatabaseService;

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
    DatabaseService databaseService;

    @Before
    public void setUp() {
        App app = (App) InstrumentationRegistry.getTargetContext().getApplicationContext();
        TestComponent component = DaggerTestComponent.builder().appModule(new AppModule(app)).build();
        component.inject(this);
    }

    @Test
    public void observableServiceTest() throws Exception {
        Author author = new AuthorParser("http://samlib.ru/s/sedrik/").parse();
        AuthorEntity entity = databaseService.insertAuthor(author.createEntry());
        databaseService.deleteAuthor(entity);
    }
}
