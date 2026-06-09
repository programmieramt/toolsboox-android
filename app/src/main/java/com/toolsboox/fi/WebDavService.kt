package com.toolsboox.fi

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xml.sax.InputSource
import timber.log.Timber
import java.io.StringReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

class WebDavService @Inject constructor() {

    data class WebDavEntry(
        val href: String,
        val name: String,
        val lastModified: Long,
        val isDirectory: Boolean
    )

    companion object {

        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        private fun authHeader(username: String, password: String) =
            Credentials.basic(username, password)

        fun list(baseUrl: String, path: String, username: String, password: String): List<WebDavEntry> {
            val url = "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"
            val body = """<?xml version="1.0"?>
                <d:propfind xmlns:d="DAV:">
                  <d:prop><d:resourcetype/><d:getlastmodified/></d:prop>
                </d:propfind>""".trimIndent()

            val request = Request.Builder()
                .url(url)
                .header("Authorization", authHeader(username, password))
                .header("Depth", "1")
                .method("PROPFIND", body.toRequestBody("application/xml".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful && response.code != 207) {
                Timber.w("PROPFIND failed: ${response.code} $url")
                return emptyList()
            }

            val xml = response.body?.string() ?: return emptyList()
            return parseMultiStatus(xml, url)
        }

        fun get(baseUrl: String, path: String, username: String, password: String): ByteArray {
            val url = "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", authHeader(username, password))
                .get()
                .build()

            val response = client.newCall(request).execute()
            check(response.isSuccessful) { "GET failed: ${response.code} $url" }
            return response.body?.bytes() ?: ByteArray(0)
        }

        fun put(baseUrl: String, path: String, content: ByteArray, username: String, password: String) {
            val url = "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", authHeader(username, password))
                .put(content.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            check(response.isSuccessful) { "PUT failed: ${response.code} $url" }
        }

        fun mkdirs(baseUrl: String, path: String, username: String, password: String) {
            val base = baseUrl.trimEnd('/')
            val segments = path.trimStart('/').trimEnd('/').split("/").filter { it.isNotEmpty() }
            var current = ""
            segments.forEach { segment ->
                current += "/$segment"
                val url = "$base$current/"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader(username, password))
                    .method("MKCOL", null)
                    .build()
                val response = client.newCall(request).execute()
                // 201 = created, 405 = already exists — both are fine
                if (!response.isSuccessful && response.code != 405) {
                    Timber.w("MKCOL failed: ${response.code} $url")
                }
            }
        }

        private fun parseMultiStatus(xml: String, requestUrl: String): List<WebDavEntry> {
            val entries = mutableListOf<WebDavEntry>()
            try {
                val doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(InputSource(StringReader(xml)))
                val responses = doc.getElementsByTagNameNS("DAV:", "response")
                val requestPath = requestUrl.substringAfter("://").substringAfter("/")

                for (i in 0 until responses.length) {
                    val response = responses.item(i)
                    var href = ""
                    var lastModified = 0L
                    var isDirectory = false

                    val children = response.childNodes
                    for (j in 0 until children.length) {
                        val child = children.item(j)
                        when (child.localName) {
                            "href" -> href = child.textContent.trim()
                            "propstat" -> {
                                val props = child.childNodes
                                for (k in 0 until props.length) {
                                    val prop = props.item(k)
                                    if (prop.localName == "prop") {
                                        val propChildren = prop.childNodes
                                        for (l in 0 until propChildren.length) {
                                            val p = propChildren.item(l)
                                            when (p.localName) {
                                                "resourcetype" -> isDirectory = p.textContent.contains("collection") ||
                                                        p.childNodes.length > 0
                                                "getlastmodified" -> {
                                                    try {
                                                        lastModified = java.text.SimpleDateFormat(
                                                            "EEE, dd MMM yyyy HH:mm:ss zzz",
                                                            java.util.Locale.US
                                                        ).parse(p.textContent.trim())?.time ?: 0L
                                                    } catch (e: Exception) {
                                                        Timber.w("Could not parse date: ${p.textContent}")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val name = href.trimEnd('/').substringAfterLast('/')
                    if (name.isNotEmpty()) {
                        entries.add(WebDavEntry(href, name, lastModified, isDirectory))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse WebDAV response")
            }
            // Skip the parent directory entry itself
            return entries.filter { !requestUrl.endsWith(it.href) && it.href.isNotEmpty() }
        }
    }
}
