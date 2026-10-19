package com.toolsboox.fi

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Plain HTTP only to servers on Tailscale or the home network; everything else must use HTTPS.
 *
 * The network security config allows cleartext in general, because Android reads it at build time and the
 * WebDAV servers are entered in the app settings. This interceptor enforces the actual rule for every
 * OkHttp client (WebDAV sync, Ultrabridge backup), so no server address has to be part of the source.
 */
object CleartextGuard : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url
        if (!url.isHttps && !isPrivateHost(url.host)) {
            throw IOException("Unverschlüsseltes HTTP ist nur für Tailscale- oder Heimnetz-Adressen erlaubt, nicht für ${url.host}. Bitte HTTPS verwenden.")
        }
        return chain.proceed(chain.request())
    }

    /**
     * Tailscale (100.64.0.0/10, fd7a:115c:a1e0::/48, `*.ts.net`, MagicDNS short names), private LAN ranges,
     * link-local, loopback and mDNS (`*.local`). Hostnames are not resolved.
     */
    fun isPrivateHost(host: String): Boolean {
        val name = host.lowercase().trim('[', ']').trimEnd('.')
        if (name == "localhost" || name.endsWith(".ts.net") || name.endsWith(".local")) return true
        ipv4(name)?.let { (a, b) ->
            return a == 10 || a == 127 ||
                    (a == 100 && b in 64..127) ||
                    (a == 172 && b in 16..31) ||
                    (a == 192 && b == 168) ||
                    (a == 169 && b == 254)
        }
        if (':' in name) {
            return name.startsWith("fd7a:115c:a1e0:") || name.startsWith("fc") || name.startsWith("fd") ||
                    name.startsWith("fe80:") || name == "::1"
        }
        // A single label (e.g. "nas") is a MagicDNS or LAN name, never a public internet host.
        return '.' !in name
    }

    /** First two octets of an IPv4 literal, null if [name] is not one. */
    private fun ipv4(name: String): Pair<Int, Int>? {
        val parts = name.split('.')
        if (parts.size != 4) return null
        val octets = parts.map { it.toIntOrNull()?.takeIf { octet -> octet in 0..255 } ?: return null }
        return octets[0] to octets[1]
    }
}
