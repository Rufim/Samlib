package ru.samlib.client.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.samlib.client.data.remote.SamlibHttpClient
import ru.samlib.client.domain.repository.SamlibResult
import java.io.File

class AuthRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun store(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { File(tmp.newFolder(), "auth.preferences_pb") },
        )

    @Test
    fun `login с неверными данными возвращает Error`() =
        runTest {
            // Без Set-Cookie в ответе SamlibHttpClient.login() возвращает null.
            val engine = MockEngine { respond(ByteReadChannel(ByteArray(0))) }
            val repo = DefaultAuthRepository(store(), SamlibHttpClient(engine))

            val result = repo.login("user", "wrong")

            assertTrue(result is SamlibResult.Error)
            assertFalse(repo.isLoggedIn.first())
        }
}
