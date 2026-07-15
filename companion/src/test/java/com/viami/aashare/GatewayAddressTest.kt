package com.viami.aashare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GatewayAddressTest {

    @Test
    fun `formats the little-endian dhcp gateway int`() {
        // 192.168.43.1 little-endian: 0x012BA8C0
        assertEquals("192.168.43.1", GatewayAddress.format(0x012BA8C0))
        // 192.168.1.1 little-endian: 0x0101A8C0
        assertEquals("192.168.1.1", GatewayAddress.format(0x0101A8C0))
    }

    @Test
    fun `zero means no gateway`() {
        assertNull(GatewayAddress.format(0))
    }
}
