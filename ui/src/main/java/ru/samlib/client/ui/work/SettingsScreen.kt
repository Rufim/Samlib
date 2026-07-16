package ru.samlib.client.ui.work

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.domain.repository.TtsEngineStatus
import ru.samlib.client.ui.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.collectAsState()
    val settings = state.settings
    val ctx = LocalContext.current
    // Выбор файла старой БД вручную (запасной путь, если авто-поиск не нашёл).
    val legacyDbPicker =
        androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            if (uri != null) {
                val copied =
                    runCatching {
                        val f = File(ctx.cacheDir, "legacy_import.db")
                        ctx.contentResolver.openInputStream(uri)?.use { input ->
                            f.outputStream().use { input.copyTo(it) }
                        }
                        f.absolutePath
                    }.getOrNull()
                if (copied != null) {
                    viewModel.importLegacyFile(copied)
                } else {
                    Toast
                        .makeText(
                            ctx,
                            ctx.getString(R.string.settings_read_file_failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    viewModel.collectSideEffect { effect ->
        when (effect) {
            is SettingsSideEffect.CacheCleared ->
                Toast
                    .makeText(
                        ctx,
                        ctx.getString(R.string.settings_cache_cleared),
                        Toast.LENGTH_SHORT,
                    ).show()

            is SettingsSideEffect.LegacyImported -> {
                val r = effect.report
                val msg =
                    if (r.total == 0) {
                        ctx.getString(R.string.settings_legacy_empty)
                    } else {
                        ctx.getString(
                            R.string.settings_legacy_report,
                            r.subscriptions,
                            r.history,
                            r.savedBooks,
                        )
                    }
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
            }

            is SettingsSideEffect.LegacyNotFound -> {
                Toast
                    .makeText(
                        ctx,
                        ctx.getString(R.string.settings_legacy_not_found),
                        Toast.LENGTH_SHORT,
                    ).show()
                legacyDbPicker.launch("*/*")
            }
        }
    }
    val ttsStatus = state.ttsStatus
    LaunchedEffect(Unit) { viewModel.observeSettings() }
    LaunchedEffect(Unit) { viewModel.observeOffline() }
    LaunchedEffect(Unit) { viewModel.observeFileLogging() }
    LaunchedEffect(Unit) { viewModel.observeNetwork() }
    LaunchedEffect(Unit) { viewModel.observeTtsStatus() }
    LaunchedEffect(Unit) { viewModel.observeBoundEngine() }
    // Прогрев выбранного движка, чтобы списки движков и голосов появились в настройках.
    LaunchedEffect(settings.ttsEngine) { viewModel.warmUpTts(settings.ttsEngine) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.reader_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SettingsCategory(stringResource(R.string.settings_category_text), first = true)
            FontSizeSection(settings, viewModel::setFontSize)
            FontSection(settings, viewModel::setFont)
            MonoFontSection(settings, viewModel::setMonoFont)
            JustifySection(settings, viewModel::setJustify)
            ThemeSection(settings, viewModel::setTheme)
            ColorsSection(settings, viewModel::setBgColor, viewModel::setTextColor)
            AutoScrollSection(settings, viewModel::setAutoScrollSpeed)

            val translateLanguages = remember { viewModel.translateLanguages() }
            SettingsCategory(stringResource(R.string.settings_category_translate))
            TranslateLanguageSection(
                settings = settings,
                languages = translateLanguages,
                onTranslateTarget = viewModel::setTranslateTarget,
                onTranslateSource = viewModel::setTranslateSource,
            )

            SettingsCategory(stringResource(R.string.settings_category_tts))
            HighlightColorSection(settings, viewModel::setHighlightColor)
            TtsSpeedSection(settings, viewModel::setTtsRate, viewModel::setTtsPitch)
            SleepTimerSection(settings, viewModel::setSleepTimer)
            // ttsStatus — триггер рекомпозиции и различение «грузится» / «готово» / «нет движка».
            val engines =
                if (ttsStatus == TtsEngineStatus.READY) {
                    viewModel.availableTtsEngines()
                } else {
                    emptyList()
                }
            // boundEngine — движок, который ФАКТИЧЕСКИ забинден в TtsPlayer (см.
            // TtsPlayer.boundEngine). settings.ttsEngine меняется мгновенно по тапу в чипе, а
            // реальное переключение движка асинхронное (LaunchedEffect срабатывает уже после
            // коммита композиции) — между этими двумя событиями есть кадр, где ttsStatus всё ещё
            // READY, но относится к СТАРОМУ движку. Если завязать голоса на settings.ttsEngine,
            // в этот кадр покажутся голоса старого движка под видом голосов только что выбранного.
            // Сверяемся с boundEngine, а не с настройкой — тогда голоса пересчитываются только
            // когда движок действительно совпал с выбором.
            val boundEngine = state.boundEngine
            val engineSynced = ttsStatus == TtsEngineStatus.READY && boundEngine == settings.ttsEngine
            val voices =
                remember(settings.voiceLanguage, boundEngine, ttsStatus) {
                    if (engineSynced) {
                        viewModel.availableTtsVoices(settings.voiceLanguage)
                    } else {
                        emptyList()
                    }
                }
            when {
                ttsStatus == TtsEngineStatus.LOADING ->
                    TtsInfoNotice(stringResource(R.string.settings_tts_loading))

                ttsStatus == TtsEngineStatus.UNAVAILABLE || engines.isEmpty() ->
                    TtsInfoNotice(stringResource(R.string.settings_tts_unavailable))

                else -> {
                    TtsEngineSection(
                        ttsEngines = engines,
                        currentEngine = settings.ttsEngine,
                        onTtsEngine = viewModel::setTtsEngine,
                    )
                    // Язык голоса — после выбора движка: он фильтрует список "Голос" ниже.
                    VoiceLanguageSection(
                        settings = settings,
                        languages = translateLanguages,
                        onVoiceLanguage = viewModel::setVoiceLanguage,
                    )
                    if (voices.isEmpty()) {
                        TtsInfoNotice(stringResource(R.string.settings_tts_no_voices))
                    } else {
                        TtsVoiceSection(
                            ttsVoices = voices,
                            currentVoice = settings.ttsVoice,
                            onTtsVoice = viewModel::setTtsVoice,
                        )
                    }
                }
            }

            SettingsCategory(stringResource(R.string.settings_category_network))
            NetworkSection(state.domain, state.proxy) { d, p ->
                viewModel.setSamlibDomain(d)
                viewModel.setProxy(p)
            }

            SettingsCategory(stringResource(R.string.settings_category_other))
            OfflineSection(state.offlineForced, viewModel::setOfflineForced)
            LoggingSection(state.fileLogging, viewModel::setFileLogging)
            OutlinedButton(
                onClick = { viewModel.clearCache() },
                modifier = Modifier.padding(top = 4.dp),
            ) { Text(stringResource(R.string.settings_clear_cache)) }
            OutlinedButton(
                onClick = { viewModel.importLegacyDefault() },
                modifier = Modifier.padding(top = 4.dp),
            ) { Text(stringResource(R.string.settings_import_legacy)) }
        }
    }
}
