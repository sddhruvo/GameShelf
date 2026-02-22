package com.gamevault.app.service

import java.nio.ByteBuffer

/**
 * Stateless DNS packet parser and response builder for the ad-blocking VPN.
 * Handles IPv4 + UDP + DNS headers only (no TCP, no IPv6).
 */
object DnsPacketProcessor {

    private const val IP_HEADER_MIN_SIZE = 20
    private const val UDP_HEADER_SIZE = 8
    private const val DNS_HEADER_SIZE = 12
    private const val UDP_PROTOCOL = 17
    private const val DNS_PORT = 53

    data class DnsQueryInfo(
        val ipHeaderLength: Int,
        val sourceIp: ByteArray,
        val destIp: ByteArray,
        val sourcePort: Int,
        val destPort: Int,
        val transactionId: Int,
        val domain: String,
        val queryStart: Int, // offset of DNS payload within packet
        val queryLength: Int // length of DNS payload
    )

    /**
     * Parse an IPv4+UDP+DNS query packet.
     * Returns null if not a valid DNS query on port 53.
     */
    fun extractDnsQuery(packet: ByteArray, length: Int): DnsQueryInfo? {
        if (length < IP_HEADER_MIN_SIZE) return null

        // Check IPv4
        val version = (packet[0].toInt() shr 4) and 0x0F
        if (version != 4) return null

        val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
        if (ipHeaderLength < IP_HEADER_MIN_SIZE) return null

        // Check UDP protocol
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != UDP_PROTOCOL) return null

        if (length < ipHeaderLength + UDP_HEADER_SIZE) return null

        // Extract source/dest IP
        val sourceIp = packet.copyOfRange(12, 16)
        val destIp = packet.copyOfRange(16, 20)

        // UDP header
        val udpOffset = ipHeaderLength
        val sourcePort = ((packet[udpOffset].toInt() and 0xFF) shl 8) or (packet[udpOffset + 1].toInt() and 0xFF)
        val destPort = ((packet[udpOffset + 2].toInt() and 0xFF) shl 8) or (packet[udpOffset + 3].toInt() and 0xFF)

        // Only handle DNS (dest port 53)
        if (destPort != DNS_PORT) return null

        val dnsOffset = ipHeaderLength + UDP_HEADER_SIZE
        if (length < dnsOffset + DNS_HEADER_SIZE) return null

        // DNS transaction ID
        val transactionId = ((packet[dnsOffset].toInt() and 0xFF) shl 8) or (packet[dnsOffset + 1].toInt() and 0xFF)

        // Parse domain name from DNS question section
        val domain = parseDomainName(packet, dnsOffset + DNS_HEADER_SIZE, length)
            ?: return null

        val dnsLength = length - dnsOffset

        return DnsQueryInfo(
            ipHeaderLength = ipHeaderLength,
            sourceIp = sourceIp,
            destIp = destIp,
            sourcePort = sourcePort,
            destPort = destPort,
            transactionId = transactionId,
            domain = domain,
            queryStart = dnsOffset,
            queryLength = dnsLength
        )
    }

    /**
     * Check if a domain (or any parent domain) is in the blocklist.
     */
    fun isDomainBlocked(domain: String, blocklist: Set<String>): Boolean {
        val lower = domain.lowercase()
        if (lower in blocklist) return true

        // Check parent domains: e.g., for "ads.example.com", also check "example.com"
        var d = lower
        while (true) {
            val dot = d.indexOf('.')
            if (dot < 0 || dot == d.length - 1) break
            d = d.substring(dot + 1)
            if (d in blocklist) return true
        }
        return false
    }

    /**
     * Build a fake DNS response with A record pointing to 0.0.0.0.
     * Swaps source/dest IP and ports, sets DNS response flags.
     */
    fun buildBlockedResponse(packet: ByteArray, length: Int, query: DnsQueryInfo): ByteArray {
        val dnsPayload = buildBlockedDnsResponse(packet, query)
        return wrapInIpUdp(
            sourceIp = query.destIp,
            destIp = query.sourceIp,
            sourcePort = query.destPort,
            destPort = query.sourcePort,
            dnsPayload = dnsPayload
        )
    }

    /**
     * Extract the DNS payload (strip IP+UDP headers) for forwarding to upstream.
     */
    fun extractDnsPayload(packet: ByteArray, length: Int, query: DnsQueryInfo): ByteArray {
        return packet.copyOfRange(query.queryStart, query.queryStart + query.queryLength)
    }

    /**
     * Wrap an upstream DNS response back into an IP+UDP packet for the TUN interface.
     * Uses the original query's addressing info to construct proper headers.
     */
    fun wrapDnsResponse(original: DnsQueryInfo, response: ByteArray): ByteArray {
        return wrapInIpUdp(
            sourceIp = original.destIp,
            destIp = original.sourceIp,
            sourcePort = original.destPort,
            destPort = original.sourcePort,
            dnsPayload = response
        )
    }

    // ---- Private helpers ----

    private fun parseDomainName(packet: ByteArray, offset: Int, length: Int): String? {
        val parts = mutableListOf<String>()
        var pos = offset
        while (pos < length) {
            val labelLen = packet[pos].toInt() and 0xFF
            if (labelLen == 0) break
            // Compression pointer — not expected in queries, but handle gracefully
            if ((labelLen and 0xC0) == 0xC0) break
            pos++
            if (pos + labelLen > length) return null
            parts.add(String(packet, pos, labelLen, Charsets.US_ASCII))
            pos += labelLen
        }
        return if (parts.isNotEmpty()) parts.joinToString(".") else null
    }

    private fun buildBlockedDnsResponse(packet: ByteArray, query: DnsQueryInfo): ByteArray {
        val dnsOffset = query.queryStart

        // Copy the entire DNS question section
        val questionEnd = findQuestionEnd(packet, dnsOffset + DNS_HEADER_SIZE, dnsOffset + query.queryLength)

        // Build DNS response
        val buf = ByteBuffer.allocate(512)

        // Transaction ID
        buf.putShort(query.transactionId.toShort())
        // Flags: standard response, no error, recursion available
        buf.putShort(0x8180.toShort())
        // Questions: 1
        buf.putShort(1.toShort())
        // Answers: 1
        buf.putShort(1.toShort())
        // Authority RRs: 0
        buf.putShort(0.toShort())
        // Additional RRs: 0
        buf.putShort(0.toShort())

        // Copy question section from original packet
        val questionStart = dnsOffset + DNS_HEADER_SIZE
        val questionLen = questionEnd - questionStart
        buf.put(packet, questionStart, questionLen)

        // Answer section: pointer to domain name in question
        buf.putShort(0xC00C.toInt().toShort()) // name pointer to offset 12
        buf.putShort(1.toShort()) // Type A
        buf.putShort(1.toShort()) // Class IN
        buf.putInt(300) // TTL 5 minutes
        buf.putShort(4.toShort()) // Data length
        buf.put(0) // 0.0.0.0
        buf.put(0)
        buf.put(0)
        buf.put(0)

        val result = ByteArray(buf.position())
        buf.flip()
        buf.get(result)
        return result
    }

    private fun findQuestionEnd(packet: ByteArray, offset: Int, limit: Int): Int {
        var pos = offset
        // Skip domain name labels
        while (pos < limit) {
            val labelLen = packet[pos].toInt() and 0xFF
            if (labelLen == 0) {
                pos++ // skip the null terminator
                break
            }
            if ((labelLen and 0xC0) == 0xC0) {
                pos += 2 // compression pointer is 2 bytes
                break
            }
            pos += 1 + labelLen
        }
        // Skip QTYPE (2 bytes) + QCLASS (2 bytes)
        pos += 4
        return pos.coerceAtMost(limit)
    }

    private fun wrapInIpUdp(
        sourceIp: ByteArray,
        destIp: ByteArray,
        sourcePort: Int,
        destPort: Int,
        dnsPayload: ByteArray
    ): ByteArray {
        val udpLength = UDP_HEADER_SIZE + dnsPayload.size
        val totalLength = IP_HEADER_MIN_SIZE + udpLength
        val packet = ByteArray(totalLength)
        val buf = ByteBuffer.wrap(packet)

        // === IPv4 Header ===
        buf.put((0x45).toByte()) // Version 4, IHL 5 (20 bytes)
        buf.put(0) // DSCP/ECN
        buf.putShort(totalLength.toShort()) // Total length
        buf.putShort(0) // Identification
        buf.putShort(0x4000.toShort()) // Flags: Don't Fragment
        buf.put(64) // TTL
        buf.put(UDP_PROTOCOL.toByte()) // Protocol: UDP
        buf.putShort(0) // Header checksum (will be computed)
        buf.put(sourceIp)
        buf.put(destIp)

        // Compute IP header checksum
        val checksum = computeIpChecksum(packet, 0, IP_HEADER_MIN_SIZE)
        packet[10] = (checksum shr 8).toByte()
        packet[11] = (checksum and 0xFF).toByte()

        // === UDP Header ===
        buf.position(IP_HEADER_MIN_SIZE)
        buf.putShort(sourcePort.toShort())
        buf.putShort(destPort.toShort())
        buf.putShort(udpLength.toShort())
        buf.putShort(0) // UDP checksum (optional for IPv4)

        // === DNS Payload ===
        buf.put(dnsPayload)

        return packet
    }

    private fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length
        while (i < end - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.toInt().inv()) and 0xFFFF
    }
}
