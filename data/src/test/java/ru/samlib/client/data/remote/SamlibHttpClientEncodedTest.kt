package ru.samlib.client.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SamlibHttpClientEncodedTest {
    @Test
    fun `getEncoded шлёт cp1251 percent-encoding без повторного кодирования`() =
        runTest {
            var requested = ""
            val engine =
                MockEngine { request ->
                    requested = request.url.toString()
                    respond(ByteReadChannel("<html></html>".toByteArray()))
                }
            val client = SamlibHttpClient(engine)
            client.getEncoded("/cgi-bin/seek", "FIND=%CF%F3%F8%EA%E8%ED&PAGE=1")
            assertTrue(
                "URL должен содержать %CF...: $requested",
                requested.contains("%CF%F3%F8%EA%E8%ED"),
            )
            assertTrue(requested.contains("/cgi-bin/seek"))
        }
}
