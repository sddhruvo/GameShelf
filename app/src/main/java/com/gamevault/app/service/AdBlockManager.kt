package com.gamevault.app.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gamevault.app.ui.home.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdBlockManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_AD_WHITELIST = stringPreferencesKey("ad_block_whitelist")
    }

    /**
     * Lazy-loaded blocklist from assets/ad_domains.txt.
     */
    val blocklist: Set<String> by lazy {
        loadBlocklist()
    }

    /**
     * Check if VPN permission has already been granted.
     * Note: VpnService.prepare() requires an Activity context for the consent dialog,
     * but passing null to check returns null when already granted.
     */
    fun hasPermission(): Boolean {
        return try {
            VpnService.prepare(context) == null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns the VPN consent dialog Intent, or null if already approved.
     * The caller (an Activity) should launch this with an ActivityResultLauncher.
     */
    fun prepareVpn(): Intent? {
        return try {
            VpnService.prepare(context)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Start the ad-blocking VPN service.
     */
    fun startVpn() {
        val intent = Intent(context, AdBlockVpnService::class.java).apply {
            action = AdBlockVpnService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    /**
     * Stop the ad-blocking VPN service.
     */
    fun stopVpn() {
        val intent = Intent(context, AdBlockVpnService::class.java).apply {
            action = AdBlockVpnService.ACTION_STOP
        }
        context.startService(intent)
    }

    /**
     * Whether the VPN service is currently running.
     */
    fun isRunning(): Boolean = AdBlockVpnService.isRunning

    /**
     * Toggle a game's whitelist status (allow ads for that game).
     */
    suspend fun toggleWhitelist(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = parseWhitelist(prefs[KEY_AD_WHITELIST] ?: "")
            val updated = if (packageName in current) {
                current - packageName
            } else {
                current + packageName
            }
            prefs[KEY_AD_WHITELIST] = updated.joinToString(",")
        }
    }

    /**
     * Flow of whitelisted package names.
     */
    fun getWhitelistedPackages(): Flow<Set<String>> {
        return context.dataStore.data.map { prefs ->
            parseWhitelist(prefs[KEY_AD_WHITELIST] ?: "")
        }
    }

    /**
     * Notify the VPN service to rebuild its interface (e.g., after whitelist change).
     */
    fun rebuildVpn() {
        if (!isRunning()) return
        val intent = Intent(context, AdBlockVpnService::class.java).apply {
            action = AdBlockVpnService.ACTION_REBUILD
        }
        context.startService(intent)
    }

    private fun parseWhitelist(raw: String): Set<String> {
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun loadBlocklist(): Set<String> {
        val domains = mutableSetOf<String>()
        try {
            context.assets.open("ad_domains.txt").use { stream ->
                BufferedReader(InputStreamReader(stream)).useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            domains.add(trimmed.lowercase())
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Asset missing — no domains blocked
        }
        return domains
    }
}
