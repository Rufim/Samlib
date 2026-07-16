package ru.samlib.client.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Абстракция управления TTS-движком/голосами для экрана настроек (развязка от TextToSpeech). */
interface TtsEngineSettings {
    val engineStatus: StateFlow<TtsEngineStatus>

    /** Движок, реально забинденный сейчас (в отличие от просто запрошенного). */
    val boundEngine: StateFlow<String?>

    /** Движки TTS, доступные на устройстве (имя, отображаемая метка). */
    fun engines(): List<Pair<String, String>>

    /** Голоса движка для языка [language] (BCP-47/ISO2, напр. «ru»/«en»), уже отфильтрованные. */
    fun voices(language: String): List<String>

    /** Длительность таймера сна (минуты); при активной озвучке перепланирует отсчёт. */
    fun setSleepTimer(minutes: Int)

    /** Прогрев движка без старта озвучки — чтобы списки движков/голосов появились в настройках. */
    fun warmUp(engine: String?)
}
