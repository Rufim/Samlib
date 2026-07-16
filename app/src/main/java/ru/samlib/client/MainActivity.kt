package ru.samlib.client

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import ru.samlib.client.domain.repository.LegacyImporter
import ru.samlib.client.domain.repository.ReaderSettingsRepository
import ru.samlib.client.domain.repository.WorkRepository
import ru.samlib.client.domain.settings.LastBookPrefix
import ru.samlib.client.domain.settings.ReaderSettings
import ru.samlib.client.domain.settings.ReaderTheme
import ru.samlib.client.ui.R
import ru.samlib.client.ui.navigation.HomeRoute
import ru.samlib.client.ui.navigation.LocalWorkRoute
import ru.samlib.client.ui.navigation.SamlibNavHost
import ru.samlib.client.ui.navigation.WorkRoute
import ru.samlib.client.ui.theme.SamlibTheme
import ru.samlib.client.work.FollowedUpdateWorker

class MainActivity : ComponentActivity() {
    private val settingsRepo: ReaderSettingsRepository by inject()
    private val legacyImporter: LegacyImporter by inject()
    private val workRepo: WorkRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        importLegacyOnce()
        // Главный экран всегда корневой: книгу (открытую по файлу или последнюю) кладём
        // ПОВЕРХ него, чтобы «назад» из читалки всегда вёл на главную, а не выходил из
        // приложения. Открытие файла из другого приложения важнее авто-восстановления книги.
        val notified = intent?.getStringExtra(FollowedUpdateWorker.EXTRA_WORK_LINK)
        val incoming = incomingFileUri()

        // Заставка держится, пока не разрешён автооткрываемый маршрут (без блокировки потока).
        // autoOpen — Compose State: SamlibNavHost должен узнать о значении, разрешённом уже
        // после первой композиции (обычный var такую смену не триггерит перекомпозицию).
        val autoOpen = mutableStateOf<Any?>(null)
        var resolved = false
        splashScreen.setKeepOnScreenCondition { !resolved }
        lifecycleScope.launch {
            autoOpen.value =
                when {
                    // уведомление об обновлении важнее восстановления последней книги
                    notified != null -> WorkRoute(notified)
                    incoming != null -> LocalWorkRoute(incoming)
                    else -> lastBookRoute()
                }
            resolved = true
        }

        enableEdgeToEdge()
        setContent {
            // Тема приложения следует выбранной теме читалки (Светлая/Тёмная/Сепия/Системная).
            val settings by settingsRepo.settings.collectAsState(initial = ReaderSettings())
            val dark =
                when (settings.theme) {
                    ReaderTheme.DARK -> true
                    ReaderTheme.LIGHT, ReaderTheme.SEPIA -> false
                    ReaderTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                }
            SamlibTheme(darkTheme = dark) {
                SamlibNavHost(rememberNavController(), HomeRoute, autoOpen.value)
            }
        }
    }

    /** Однократный перенос данных из старой версии приложения (если её БД осталась на устройстве). */
    private fun importLegacyOnce() {
        lifecycleScope.launch {
            runCatching {
                if (settingsRepo.legacyImportDoneOnce()) return@runCatching
                val db = legacyImporter.defaultLegacyDb() ?: return@runCatching
                val report = legacyImporter.import(db)
                settingsRepo.setLegacyImportDone()
                if (report.total > 0) {
                    Toast
                        .makeText(
                            this@MainActivity,
                            getString(
                                R.string.main_legacy_imported,
                                report.subscriptions,
                                report.history,
                                report.savedBooks,
                            ),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }
    }

    private fun incomingFileUri(): String? =
        if (intent?.action == Intent.ACTION_VIEW) intent?.data?.toString() else null

    /** Маршрут последней открытой книги ("work:<link>" | "local:<uri>") или null. */
    private suspend fun lastBookRoute(): Any? {
        val saved = runCatching { settingsRepo.lastBookOnce() }.getOrNull() ?: return null
        return when {
            saved.startsWith(LastBookPrefix.WORK) -> {
                val link = saved.removePrefix(LastBookPrefix.WORK)
                // Офлайн и без кеша книгу не автооткрываем: вместо экрана ошибки
                // пользователь попадает на главное меню.
                val cached = runCatching { workRepo.hasCached(link) }.getOrDefault(false)
                if (cached || isNetworkAvailable()) WorkRoute(link) else null
            }

            saved.startsWith(LastBookPrefix.LOCAL) -> LocalWorkRoute(saved.removePrefix(LastBookPrefix.LOCAL))
            else -> null
        }
    }

    /** Быстрая проверка наличия сети без сетевых запросов (ConnectivityManager). */
    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
