package ru.samlib.client.data.remote

import java.net.URLEncoder

fun cp1251Encode(value: String): String = URLEncoder.encode(value, "windows-1251")
