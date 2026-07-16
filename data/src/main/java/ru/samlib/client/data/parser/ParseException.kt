package ru.samlib.client.data.parser

class ParseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
