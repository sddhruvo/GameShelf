package com.gamevault.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.gamevault.app.data.db.GameDao
import com.gamevault.app.data.db.UpdateDao
import com.gamevault.app.data.model.Game
import com.gamevault.app.data.model.GameUpdate
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageEventReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun gameDao(): GameDao
        fun updateDao(): UpdateDao
    }

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ReceiverEntryPoint::class.java
                )
                handlePackageEvent(context, intent.action, packageName, entryPoint)
            } catch (_: Exception) {
                // Hilt not initialized yet (e.g. during restore), skip
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handlePackageEvent(
        context: Context,
        action: String?,
        packageName: String,
        entryPoint: ReceiverEntryPoint
    ) {
        val gameDao = entryPoint.gameDao()
        val updateDao = entryPoint.updateDao()

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                try {
                    val pm = context.packageManager
                    val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                    if (isGame(appInfo)) {
                        val packageInfo = pm.getPackageInfo(packageName, 0)
                        val name = pm.getApplicationLabel(appInfo).toString()
                        val game = Game(
                            packageName = packageName,
                            name = name,
                            installDate = packageInfo.firstInstallTime,
                            currentVersion = packageInfo.versionName ?: "",
                            appSizeBytes = getAppSize(appInfo),
                            isInstalled = true,
                            isNew = true
                        )
                        gameDao.insertGame(game)
                    }
                } catch (_: PackageManager.NameNotFoundException) {
                    // Package no longer exists
                }
            }

            Intent.ACTION_PACKAGE_REMOVED -> {
                // Verify package is actually removed before updating DB
                val isStillInstalled = try {
                    context.packageManager.getApplicationInfo(packageName, 0)
                    true
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
                if (!isStillInstalled) {
                    gameDao.setInstalled(packageName, false)
                }
            }

            Intent.ACTION_PACKAGE_REPLACED -> {
                try {
                    val pm = context.packageManager
                    val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                    if (isGame(appInfo)) {
                        val packageInfo = pm.getPackageInfo(packageName, 0)
                        val existingGame = gameDao.getGame(packageName)
                        val oldVersion = existingGame?.currentVersion ?: ""
                        val newVersion = packageInfo.versionName ?: ""
                        val newSize = getAppSize(appInfo)

                        if (oldVersion.isNotBlank() && newVersion.isNotBlank() && oldVersion != newVersion) {
                            val update = GameUpdate(
                                packageName = packageName,
                                oldVersion = oldVersion,
                                newVersion = newVersion,
                                oldSizeBytes = existingGame?.appSizeBytes ?: 0,
                                newSizeBytes = newSize
                            )
                            updateDao.insertUpdate(update)
                        }

                        gameDao.updateVersionAndSize(packageName, newVersion, newSize)
                    }
                } catch (_: PackageManager.NameNotFoundException) {
                    // Package no longer exists
                }
            }
        }
    }

    private fun isGame(appInfo: ApplicationInfo): Boolean {
        if (appInfo.category == ApplicationInfo.CATEGORY_GAME) return true
        @Suppress("DEPRECATION")
        if (appInfo.flags and ApplicationInfo.FLAG_IS_GAME != 0) return true
        return false
    }

    private fun getAppSize(appInfo: ApplicationInfo): Long {
        return try {
            java.io.File(appInfo.sourceDir).length()
        } catch (_: Exception) {
            0L
        }
    }
}
