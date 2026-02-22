package com.gamevault.app.service

import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DndManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var previousFilter = NotificationManager.INTERRUPTION_FILTER_ALL
    private var enabledByUs = false

    fun hasPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun enableDnd() {
        if (!hasPermission()) return
        previousFilter = notificationManager.currentInterruptionFilter
        enabledByUs = true
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
    }

    fun disableDnd() {
        if (!hasPermission()) return
        if (!enabledByUs) return
        enabledByUs = false
        notificationManager.setInterruptionFilter(previousFilter)
    }

    fun isDndActive(): Boolean {
        return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }
}
