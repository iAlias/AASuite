package com.viami.aashare

/** WifiManager.dhcpInfo.gateway is a little-endian IPv4 int. */
object GatewayAddress {

    fun format(gateway: Int): String? {
        if (gateway == 0) return null
        return listOf(0, 8, 16, 24)
            .joinToString(".") { shift -> ((gateway shr shift) and 0xff).toString() }
    }
}
