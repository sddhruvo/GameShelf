package com.gamevault.app.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.gamevault.app.data.model.Game
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledApp(
    val packageName: String,
    val name: String,
    val isDetectedAsGame: Boolean
)

@Singleton
class GameDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getAllVisibleApps(): List<InstalledApp> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { appInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    name = pm.getApplicationLabel(appInfo).toString(),
                    isDetectedAsGame = isGame(appInfo)
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun getAppByPackageName(packageName: String): InstalledApp? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            InstalledApp(
                packageName = appInfo.packageName,
                name = pm.getApplicationLabel(appInfo).toString(),
                isDetectedAsGame = isGame(appInfo)
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
    fun detectInstalledGames(): List<Game> {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return installedApps
            .filter { isGame(it) }
            .mapNotNull { appInfo ->
                try {
                    val packageInfo = pm.getPackageInfo(appInfo.packageName, 0)
                    val name = pm.getApplicationLabel(appInfo).toString()
                    val installTime = packageInfo.firstInstallTime
                    val versionName = packageInfo.versionName ?: ""
                    val appSize = getAppSize(appInfo)

                    Game(
                        packageName = appInfo.packageName,
                        name = name,
                        installDate = installTime,
                        currentVersion = versionName,
                        appSizeBytes = appSize,
                        isInstalled = true
                    )
                } catch (_: Exception) {
                    null
                }
            }
    }

    fun getGameInfo(packageName: String): Game? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val packageInfo = pm.getPackageInfo(packageName, 0)
            val name = pm.getApplicationLabel(appInfo).toString()

            Game(
                packageName = packageName,
                name = name,
                installDate = packageInfo.firstInstallTime,
                currentVersion = packageInfo.versionName ?: "",
                appSizeBytes = getAppSize(appInfo),
                isInstalled = true,
                isNew = true
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun isGame(appInfo: ApplicationInfo): Boolean {
        // Check category (API 26+)
        if (appInfo.category == ApplicationInfo.CATEGORY_GAME) return true

        // Fallback: check flags
        @Suppress("DEPRECATION")
        if (appInfo.flags and ApplicationInfo.FLAG_IS_GAME != 0) return true

        return false
    }

    private fun getAppSize(appInfo: ApplicationInfo): Long {
        return try {
            val sourceDir = appInfo.sourceDir
            java.io.File(sourceDir).length()
        } catch (_: Exception) {
            0L
        }
    }
}
