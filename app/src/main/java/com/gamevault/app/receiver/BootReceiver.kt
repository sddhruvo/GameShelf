package com.gamevault.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gamevault.app.service.WeeklyReportWorker
import androidx.work.*
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule weekly report worker
            val workRequest = PeriodicWorkRequestBuilder<WeeklyReportWorker>(
                7, TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "weekly_report",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
