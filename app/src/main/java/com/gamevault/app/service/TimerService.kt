package com.gamevault.app.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gamevault.app.R
import com.gamevault.app.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "game_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.gamevault.action.START_TIMER"
        const val ACTION_STOP = "com.gamevault.action.STOP_TIMER"
        const val EXTRA_DURATION_MS = "duration_ms"
        const val EXTRA_GAME_NAME = "game_name"
    }

    private val binder = TimerBinder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs

    private var timerJob: Job? = null
    private var gameName: String = ""
    private var totalDurationMs: Long = 0L
    private var startTimeMs: Long = 0L

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                totalDurationMs = intent.getLongExtra(EXTRA_DURATION_MS, 0L)
                gameName = intent.getStringExtra(EXTRA_GAME_NAME) ?: ""
                startTimer()
            }
            ACTION_STOP -> stopTimer()
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        startTimeMs = System.currentTimeMillis()
        _remainingMs.value = totalDurationMs
        _isRunning.value = true

        startForeground(NOTIFICATION_ID, createNotification("Timer started: $gameName"))

        timerJob?.cancel()
        timerJob = scope.launch {
            while (_remainingMs.value > 0 && isActive) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - startTimeMs
                _elapsedMs.value = elapsed
                _remainingMs.value = (totalDurationMs - elapsed).coerceAtLeast(0)

                updateNotification()

                if (_remainingMs.value <= 0) {
                    onTimerExpired()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _isRunning.value = false
        _remainingMs.value = 0
        _elapsedMs.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerExpired() {
        _isRunning.value = false
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gamepad)
            .setContentTitle(getString(R.string.timer_expired))
            .setContentText("You've played $gameName for the set time limit")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID + 1, notification)
        stopTimer()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.timer_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gamepad)
            .setContentTitle(getString(R.string.timer_notification_title))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val minutes = _remainingMs.value / 60000
        val seconds = (_remainingMs.value % 60000) / 1000
        val text = "$gameName — ${minutes}m ${seconds}s remaining"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, createNotification(text))
    }

    override fun onDestroy() {
        timerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
