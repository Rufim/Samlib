package ru.samlib.client.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.samlib.client.MainActivity
import ru.samlib.client.domain.model.ObservedWork
import ru.samlib.client.domain.repository.AuthorRepository
import ru.samlib.client.ui.R
import java.util.concurrent.TimeUnit

/** Периодически проверяет отслеживаемых авторов; шлёт уведомление на каждую обновлённую книгу. */
class FollowedUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val authorRepo: AuthorRepository by inject()

    override suspend fun doWork(): Result {
        val updated =
            runCatching { authorRepo.checkFollowedUpdates() }.getOrElse { return Result.retry() }
        updated.forEach { notifyWork(it) }
        if (updated.isNotEmpty()) {
            authorRepo.clearNotNotified(
                updated
                    .map { it.authorLink }
                    .distinct(),
            )
        }
        return Result.success()
    }

    private fun notifyWork(w: ObservedWork) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    ctx.getString(R.string.followed_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        // Тап открывает обновлённую книгу (extra разбирает MainActivity).
        val intent =
            Intent(ctx, MainActivity::class.java)
                .putExtra(EXTRA_WORK_LINK, w.link)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending =
            PendingIntent.getActivity(
                ctx,
                w.link.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val text = ctx.getString(R.string.followed_work_updated, w.title ?: w.link)
        val notification =
            NotificationCompat
                .Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_newest)
                .setContentTitle(w.authorName ?: w.authorLink)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
        // id — стабильный хэш ссылки: повторное обновление той же книги заменяет уведомление
        runCatching { NotificationManagerCompat.from(ctx).notify(w.link.hashCode(), notification) }
    }

    companion object {
        const val EXTRA_WORK_LINK = "extra_work_link"
        private const val CHANNEL = "followed_updates"
        private const val UNIQUE = "followed_update_check"

        /** Планирует периодическую проверку (раз в 6 часов, при наличии сети). */
        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<FollowedUpdateWorker>(6, TimeUnit.HOURS)
                    .setConstraints(
                        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                    ).build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
