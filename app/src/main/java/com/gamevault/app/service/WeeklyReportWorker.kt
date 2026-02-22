package com.gamevault.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gamevault.app.R
import com.gamevault.app.data.repository.GameRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class WeeklyReportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: GameRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "weekly_report_channel"
        const val NOTIFICATION_ID = 2001
    }

    override suspend fun doWork(): Result {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val totalPlaytime = repository.getTotalPlaytimeSince(weekAgo).first() ?: 0L
        val topGames = repository.getMostPlayedGames(3).first()

        val hours = totalPlaytime / 3600000
        val minutes = (totalPlaytime % 3600000) / 60000

        val topGamesText = topGames.take(3).joinToString(", ") { it.name }

        createNotificationChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gamepad)
            .setContentTitle("Weekly Gaming Report")
            .setContentText("You played ${hours}h ${minutes}m this week")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You played ${hours}h ${minutes}m this week.\nTop games: $topGamesText")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)

        return Result.success()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Weekly Reports",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
