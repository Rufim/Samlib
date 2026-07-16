package ru.samlib.client.data.remote

import ru.samlib.client.data.parser.ParseException
import ru.samlib.client.domain.repository.SamlibError
import ru.samlib.client.domain.repository.SamlibResult
import java.io.IOException

inline fun <T> samlibCatching(block: () -> T): SamlibResult<T> =
    try {
        SamlibResult.Success(block())
    } catch (e: ParseException) {
        SamlibResult.Error(SamlibError.PARSE, e)
    } catch (e: IOException) {
        SamlibResult.Error(SamlibError.NETWORK, e)
    } catch (e: Exception) {
        SamlibResult.Error(SamlibError.UNKNOWN, e)
    }
