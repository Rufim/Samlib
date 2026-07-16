package ru.samlib.client.ui.work

import android.content.Context

class DefaultTtsServiceLauncher(
    private val context: Context,
) : TtsServiceLauncher {
    override fun start() = TtsService.start(context)
}
