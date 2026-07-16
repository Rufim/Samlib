package ru.samlib.client.domain.usecase

import ru.samlib.client.domain.repository.LegacyImportReport
import ru.samlib.client.domain.repository.LegacyImporter
import ru.samlib.client.domain.repository.ReaderSettingsRepository
import java.io.File

/**
 * Перенос данных из legacy-БД: находит файл (явно выбранный или БД по умолчанию при
 * апгрейде поверх старой версии), выполняет импорт и отмечает перенос выполненным —
 * та же комбинация двух шагов, что раньше дублировалась в `importLegacyDefault`/
 * `importLegacyFile` (`SettingsViewModel`).
 */
class ImportLegacyDataUseCase(
    private val legacyImporter: LegacyImporter,
    private val settingsRepo: ReaderSettingsRepository,
) {
    sealed interface Result {
        data class Imported(
            val report: LegacyImportReport,
        ) : Result

        /** Только при [invoke] с `file = null` — БД по умолчанию не найдена на устройстве. */
        data object NotFound : Result
    }

    /** [file] — явно выбранный файл БД; null — искать legacy-БД по умолчанию. */
    suspend operator fun invoke(file: File?): Result {
        val db = file ?: legacyImporter.defaultLegacyDb() ?: return Result.NotFound
        val report = legacyImporter.import(db)
        settingsRepo.setLegacyImportDone()
        return Result.Imported(report)
    }
}
