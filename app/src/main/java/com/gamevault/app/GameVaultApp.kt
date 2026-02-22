package com.gamevault.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.gamevault.app.service.AdBlockManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GameVaultApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var adBlockManager: AdBlockManager

    private var startedActivityCount = 0
    private var wasInBackground = false
    private val handler = Handler(Looper.getMainLooper())
    private var pendingStopRunnable: Runnable? = null

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(AppLifecycleTracker())
    }

    private inner class AppLifecycleTracker : ActivityLifecycleCallbacks {

        override fun onActivityStarted(activity: Activity) {
            // Cancel any pending VPN stop (e.g., during config change or quick app switch)
            pendingStopRunnable?.let { handler.removeCallbacks(it) }
            pendingStopRunnable = null

            val wasBg = startedActivityCount == 0
            startedActivityCount++

            if (wasBg && wasInBackground) {
                // App returned to foreground — stop auto-started VPN
                onAppForeground()
            }
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivityCount--
            if (startedActivityCount == 0) {
                wasInBackground = true
                // App went to background — schedule VPN stop with delay
                // to handle config changes (rotation) and brief transitions
                onAppBackground()
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    private fun onAppForeground() {
        // User is back in GameVault — they're done playing
        if (adBlockManager.isAutoStarted() && adBlockManager.isRunning()) {
            adBlockManager.stopVpn()
        }
    }

    private fun onAppBackground() {
        // Don't stop immediately — the user might be launching a game.
        // If they swiped us from recents or system kills us,
        // onTaskRemoved() and the START_STICKY guard handle those cases.
    }
}
