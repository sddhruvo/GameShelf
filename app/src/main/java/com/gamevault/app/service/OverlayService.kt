package com.gamevault.app.service

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.gamevault.app.R
import com.gamevault.app.ui.MainActivity
import kotlinx.coroutines.*

class OverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "overlay_channel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_START = "com.gamevault.action.START_OVERLAY"
        const val ACTION_STOP = "com.gamevault.action.STOP_OVERLAY"
        const val EXTRA_COUNTDOWN_MS = "countdown_ms"
        const val EXTRA_GAME_NAME = "game_name"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: TextView? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var startTime = 0L
    private var countdownMs = 0L  // 0 = elapsed mode, >0 = countdown mode

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                countdownMs = intent.getLongExtra(EXTRA_COUNTDOWN_MS, 0L)
                startOverlay()
            }
            ACTION_STOP -> stopOverlay()
        }
        return START_NOT_STICKY
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        startTime = System.currentTimeMillis()
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager ?: run {
            stopSelf()
            return
        }

        overlayView = TextView(this).apply {
            text = "0:00"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xCC1A1A2E.toInt())
            setPadding(16, 8, 16, 8)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 100
        }

        // Make draggable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(overlayView, params)

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Update display — countdown or elapsed mode
        scope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - startTime

                if (countdownMs > 0) {
                    // Countdown mode: show remaining time
                    val remaining = (countdownMs - elapsed).coerceAtLeast(0)
                    val minutes = remaining / 60000
                    val seconds = (remaining % 60000) / 1000
                    overlayView?.text = "-${minutes}:${seconds.toString().padStart(2, '0')}"

                    // Change color when < 5 min remaining
                    if (remaining < 300_000) {
                        overlayView?.setTextColor(0xFFFF6B6B.toInt())
                    }

                    if (remaining <= 0) {
                        overlayView?.text = "TIME UP"
                        overlayView?.setTextColor(0xFFFF2E63.toInt())
                        delay(5000)
                        stopOverlay()
                        return@launch
                    }
                } else {
                    // Elapsed mode: show time played
                    val minutes = elapsed / 60000
                    val seconds = (elapsed % 60000) / 1000
                    overlayView?.text = "${minutes}:${seconds.toString().padStart(2, '0')}"
                }
            }
        }
    }

    private fun stopOverlay() {
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) { }
        overlayView = null
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Game Overlay",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gamepad)
            .setContentTitle("GameShelf Overlay")
            .setContentText("Tracking game session")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopOverlay()
        super.onDestroy()
    }
}
