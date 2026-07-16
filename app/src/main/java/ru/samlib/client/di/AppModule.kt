package ru.samlib.client.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.samlib.client.ui.work.DefaultTtsServiceLauncher
import ru.samlib.client.ui.work.TtsServiceLauncher

val appModule =
    module {
        single<TtsServiceLauncher> { DefaultTtsServiceLauncher(androidContext()) }
    }
