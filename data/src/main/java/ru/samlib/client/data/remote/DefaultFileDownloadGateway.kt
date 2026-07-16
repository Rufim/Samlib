package ru.samlib.client.data.remote

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import ru.samlib.client.domain.repository.FileDownloadGateway

/** [FileDownloadGateway] поверх системного [DownloadManager]. */
class DefaultFileDownloadGateway(
    private val context: Context,
) : FileDownloadGateway {
    override fun enqueue(
        url: String,
        displayName: String,
    ) {
        val request =
            DownloadManager
                .Request(Uri.parse(url))
                .setTitle(displayName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, displayName)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }
}
