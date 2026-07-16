package ru.samlib.client.domain.settings

/**
 * Префиксы значения [ReaderSettingsRepository.setLastBook] ("work:<link>" | "local:<uri>") —
 * единая точка, чтобы запись (WorkViewModel/LocalWorkViewModel) и разбор (MainActivity)
 * не дублировали одни и те же строки-литералы.
 */
object LastBookPrefix {
    const val WORK = "work:"
    const val LOCAL = "local:"
}
