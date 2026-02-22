package com.gamevault.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.gamevault.app.R
import com.gamevault.app.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

class AdBlockVpnService : VpnService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface VpnServiceEntryPoint {
        fun adBlockManager(): AdBlockManager
    }

    companion object {
        const val CHANNEL_ID = "ad_block_channel"
        const val NOTIFICATION_ID = 1003
        const val ACTION_START = "com.gamevault.action.START_AD_BLOCK"
        const val ACTION_STOP = "com.gamevault.action.STOP_AD_BLOCK"
        const val ACTION_REBUILD = "com.gamevault.action.REBUILD_VPN"

        @Volatile
        var isRunning: Boolean = false
            private set

        private val adsBlockedCounter = AtomicInteger(0)
        val adsBlocked: Int get() = adsBlockedCounter.get()
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var blocklist: Set<String> = emptySet()
    private var whitelistedPackages: Set<String> = emptySet()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                scope.launch { startVpn() }
            }
            ACTION_STOP -> stopVpn()
            ACTION_REBUILD -> {
                scope.launch { rebuildVpnInterface() }
            }
            null -> {
                // Service restarted by system (START_STICKY with null intent)
                scope.launch { startVpn() }
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        scope.cancel()
        stopVpn()
        super.onDestroy()
    }

    private fun getAdBlockManager(): AdBlockManager {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            VpnServiceEntryPoint::class.java
        )
        return entryPoint.adBlockManager()
    }

    private suspend fun startVpn() {
        if (isRunning) return

        // Load blocklist
        val manager = getAdBlockManager()
        blocklist = manager.blocklist

        // Load whitelist
        whitelistedPackages = try {
            manager.getWhitelistedPackages().first()
        } catch (_: Exception) {
            emptySet()
        }

        // Build VPN interface
        val builder = buildVpnInterface(whitelistedPackages)
        vpnInterface = builder?.establish() ?: return

        adsBlockedCounter.set(0)
        isRunning = true

        startForeground(NOTIFICATION_ID, createNotification())

        // Start packet processing loop
        scope.launch { processPackets() }
    }

    private fun stopVpn() {
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
        scope.coroutineContext[Job]?.cancelChildren()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun rebuildVpnInterface() {
        if (!isRunning) return

        val manager = getAdBlockManager()
        whitelistedPackages = try {
            manager.getWhitelistedPackages().first()
        } catch (_: Exception) {
            emptySet()
        }

        // Close old interface
        vpnInterface?.close()

        // Build new one
        val builder = buildVpnInterface(whitelistedPackages)
        vpnInterface = builder?.establish() ?: run {
            stopVpn()
            return
        }

        // Restart packet loop
        scope.coroutineContext[Job]?.cancelChildren()
        scope.launch { processPackets() }
    }

    private fun buildVpnInterface(whitelist: Set<String>): Builder? {
        return try {
            Builder().apply {
                addAddress("10.0.0.2", 32)
                addRoute("8.8.8.8", 32)
                addRoute("1.1.1.1", 32)
                addDnsServer("8.8.8.8")
                addDnsServer("1.1.1.1")
                setMtu(1500)
                setSession("GameVault Ad Blocker")

                // Exclude self from VPN
                try {
                    addDisallowedApplication(packageName)
                } catch (_: Exception) {}

                // Exclude whitelisted games
                whitelist.forEach { pkg ->
                    try {
                        addDisallowedApplication(pkg)
                    } catch (_: Exception) {
                        // Package not installed, skip
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun processPackets() {
        val fd = vpnInterface ?: return
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteBuffer.allocate(32767)

        try {
            withContext(Dispatchers.IO.limitedParallelism(1)) {
            while (isRunning &&  coroutineContext.isActive) {
                buffer.clear()
                val length = input.read(buffer.array())
                if (length <= 0) {
                    delay(10)
                    continue
                }

                val packet = buffer.array()
                val query = DnsPacketProcessor.extractDnsQuery(packet, length)

                if (query == null) {
                    // Not a DNS query — only DNS-server-bound traffic enters the TUN,
                    // so non-DNS packets here are unexpected; safely ignore them
                    continue
                }

                if (DnsPacketProcessor.isDomainBlocked(query.domain, blocklist)) {
                    // Blocked: write fake 0.0.0.0 response
                    val response = DnsPacketProcessor.buildBlockedResponse(packet, length, query)
                    output.write(response)
                    adsBlockedCounter.incrementAndGet()
                    updateNotification()
                } else {
                    // Allowed: forward to upstream DNS
                    forwardDnsQuery(packet, length, query, output)
                }
            }
            } // withContext
        } catch (_: Exception) {
            // VPN fd closed or other error — service stopping
        } finally {
            try { input.close() } catch (_: Exception) {}
            try { output.close() } catch (_: Exception) {}
        }
    }

    private fun forwardDnsQuery(
        packet: ByteArray,
        length: Int,
        query: DnsPacketProcessor.DnsQueryInfo,
        tunOutput: FileOutputStream
    ) {
        try {
            val dnsPayload = DnsPacketProcessor.extractDnsPayload(packet, length, query)
            val socket = DatagramSocket()
            protect(socket) // Prevent VPN loop

            try {
                val address = InetAddress.getByName("8.8.8.8")
                val sendPacket = DatagramPacket(dnsPayload, dnsPayload.size, address, 53)
                socket.soTimeout = 5000
                socket.send(sendPacket)

                val responseBuffer = ByteArray(4096)
                val receivePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.receive(receivePacket)

                // Wrap response in IP+UDP headers and write to TUN
                val responseData = responseBuffer.copyOfRange(0, receivePacket.length)
                val wrappedResponse = DnsPacketProcessor.wrapDnsResponse(query, responseData)
                tunOutput.write(wrappedResponse)
            } finally {
                socket.close()
            }
        } catch (_: Exception) {
            // DNS forwarding failed — query will timeout naturally
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ad Blocker",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when the ad blocker VPN is active"
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, AdBlockVpnService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gamepad)
            .setContentTitle(getString(R.string.ad_block_active))
            .setContentText(getString(R.string.ads_blocked_count, adsBlocked))
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_gamepad, getString(R.string.stop), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        if (!isRunning) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, createNotification())
    }
}
