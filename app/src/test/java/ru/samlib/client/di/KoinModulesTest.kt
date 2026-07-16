package ru.samlib.client.di

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import ru.samlib.client.domain.repository.AuthorRepository
import ru.samlib.client.domain.repository.NewestRepository
import ru.samlib.client.domain.repository.WorkRepository

@RunWith(RobolectricTestRunner::class)
class KoinModulesTest {
    @After
    fun tearDown() = stopKoin()

    @Test
    fun `граф dataModule резолвит репозитории`() {
        val app =
            startKoin {
                androidContext(ApplicationProvider.getApplicationContext<Application>())
                modules(dataModules)
            }
        assertNotNull(app.koin.get<NewestRepository>())
        assertNotNull(app.koin.get<AuthorRepository>())
        assertNotNull(app.koin.get<WorkRepository>())
    }
}
