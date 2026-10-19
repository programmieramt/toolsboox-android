package com.toolsboox.fi

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleartextGuardTest {

    @Test
    fun tailscaleAndHomeNetworkMayUsePlainHttp() {
        listOf(
            "100.64.0.1", "100.127.255.254", "nas.tail1234.ts.net", "nas",
            "192.168.1.20", "10.0.0.5", "172.16.0.1", "172.31.255.255",
            "localhost", "127.0.0.1", "printer.local", "169.254.1.1",
            "[fd7a:115c:a1e0::1]", "fe80::1", "::1",
        ).forEach { assertTrue(it, CleartextGuard.isPrivateHost(it)) }
    }

    @Test
    fun publicHostsMustUseHttps() {
        listOf(
            "cloud.example.com", "8.8.8.8", "100.63.255.255", "100.128.0.1",
            "172.15.0.1", "172.32.0.1", "192.169.0.1", "11.0.0.1",
            "ts.net.example.com", "local.example.com", "2001:4860:4860::8888",
        ).forEach { assertFalse(it, CleartextGuard.isPrivateHost(it)) }
    }
}
