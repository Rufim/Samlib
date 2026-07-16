package ru.samlib.client.domain.samlib

/** Единая точка для домена samlib.ru — не хардкодить строку в парсерах/usecase/UI. */
object SamlibUrls {
    const val BASE = "http://samlib.ru"

    /** Голый хост (без схемы) — для сравнения хостов и вырезания пути из полного URL. */
    const val HOST = "samlib.ru"
}
