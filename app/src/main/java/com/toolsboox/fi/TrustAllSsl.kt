package com.toolsboox.fi

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Configures an [OkHttpClient.Builder] to accept any TLS certificate, including
 * self-signed ones. Opt-in per connection for private WebDAV servers reachable
 * only over a VPN (e.g. Tailscale) where the operator controls both endpoints.
 */
object TrustAllSsl {

    private val trustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(trustManager), SecureRandom())
    }

    private val hostnameVerifier = HostnameVerifier { _, _ -> true }

    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder =
        builder
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier(hostnameVerifier)
}
