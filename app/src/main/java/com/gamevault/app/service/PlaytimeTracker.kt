package com.gamevault.app.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.gamevault.app.data.model.PlaySession
import com.gamevault.app.data.repository.GameRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaytimeTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: GameRepository
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // Watermark: never re-process events before this timestamp
    private var lastSyncTimestamp: Long = 0L

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun syncPlaytimeSince(sinceMs: Long, gamePackages: Set<String>) {
        if (!hasUsagePermission()) return

        val effectiveSince = maxOf(sinceMs, lastSyncTimestamp)
        val now = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(effectiveSince, now)
        val event = UsageEvents.Event()
        val activeStarts = mutableMapOf<String, Long>()
        val newSessions = mutableListOf<PlaySession>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            if (pkg !in gamePackages) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    activeStarts[pkg] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = activeStarts.remove(pkg) ?: continue
                    val duration = event.timeStamp - start
                    if (duration > 5000) {
                        newSessions.add(
                            PlaySession(
                                packageName = pkg,
                                startTime = start,
                                endTime = event.timeStamp,
                                durationMs = duration
                            )
                        )
                    }
                }
            }
        }

        // Insert only truly new sessions (deduplicate by start+end time)
        for (session in newSessions) {
            if (!repository.sessionExists(session.packageName, session.startTime, session.endTime)) {
                repository.insertSession(session)
            }
        }

        // Recalculate total playtime from DB (single source of truth — no additive guessing)
        val affectedPackages = newSessions.map { it.packageName }.toSet()
        for (pkg in affectedPackages) {
            val totalMs = repository.getTotalPlaytimeForGameDirect(pkg)
            val lastEnd = repository.getLastSessionEnd(pkg) ?: 0L

            repository.updatePlaytime(
                packageName = pkg,
                lastPlayed = lastEnd,
                totalPlaytime = totalMs
            )
        }

        lastSyncTimestamp = now
    }

    fun getUsageStats(sinceMs: Long): Map<String, Long> {
        if (!hasUsagePermission()) return emptyMap()

        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            sinceMs,
            now
        )

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .associate { it.packageName to it.totalTimeInForeground }
    }
}
