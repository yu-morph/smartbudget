package com.yu_morph.smartbudget.ui.auth

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.browser.customtabs.CustomTabsIntent
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val LICENSE_ASSET = "open_source_licenses.html"
private const val LICENSE_PATH = "/open_source_licenses.html"
private const val SERVER_LIFETIME_MILLIS = 120_000L
private const val ACCEPT_TIMEOUT_MILLIS = 1_000
private const val CLIENT_TIMEOUT_MILLIS = 5_000
private val EmptyBody = ByteArray(0)

internal suspend fun openOpenSourceLicensesCustomTab(context: Context): Boolean = runCatching {
    val licenseUri =
        withContext(Dispatchers.IO) {
            val html = context.assets.open(LICENSE_ASSET).use { it.readBytes() }
            LicenseAssetServer.start(html)
        }
    withContext(Dispatchers.Main) {
        CustomTabsIntent
            .Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, licenseUri)
    }
}.isSuccess

private object LicenseAssetServer {
    private var activeServer: ServerSocket? = null

    @Synchronized
    fun start(html: ByteArray): Uri {
        activeServer?.close()
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        server.soTimeout = ACCEPT_TIMEOUT_MILLIS
        activeServer = server
        thread(isDaemon = true, name = "open-source-license-server") {
            serve(server, html)
        }
        return Uri.parse("http://127.0.0.1:${server.localPort}$LICENSE_PATH")
    }

    private fun serve(server: ServerSocket, html: ByteArray) {
        val expiresAt = SystemClock.elapsedRealtime() + SERVER_LIFETIME_MILLIS
        try {
            while (!server.isClosed && SystemClock.elapsedRealtime() < expiresAt) {
                val client = accept(server) ?: continue
                client.use { respond(it, html) }
            }
        } catch (_: SocketException) {
            Unit
        } finally {
            close(server)
        }
    }

    private fun accept(server: ServerSocket): Socket? = try {
        server.accept()
    } catch (_: SocketTimeoutException) {
        null
    }

    private fun respond(client: Socket, html: ByteArray) {
        client.soTimeout = CLIENT_TIMEOUT_MILLIS
        val reader = client.getInputStream().bufferedReader()
        val requestPath = reader.readLine().orEmpty().split(' ').getOrNull(1)
        while (!reader.readLine().isNullOrEmpty()) {
            Unit
        }
        when (requestPath) {
            LICENSE_PATH -> writeResponse(client.getOutputStream(), "200 OK", "text/html; charset=utf-8", html)
            "/favicon.ico" -> writeResponse(client.getOutputStream(), "204 No Content", null, EmptyBody)
            else -> writeResponse(client.getOutputStream(), "404 Not Found", "text/plain; charset=utf-8", EmptyBody)
        }
    }

    private fun writeResponse(output: OutputStream, status: String, contentType: String?, body: ByteArray) {
        val headers =
            buildString {
                append("HTTP/1.1 $status\r\n")
                contentType?.let { append("Content-Type: $it\r\n") }
                append("Content-Length: ${body.size}\r\n")
                append("Cache-Control: no-store\r\n")
                append("Connection: close\r\n\r\n")
            }
        output.write(headers.toByteArray(StandardCharsets.US_ASCII))
        output.write(body)
        output.flush()
    }

    @Synchronized
    private fun close(server: ServerSocket) {
        server.close()
        if (activeServer === server) {
            activeServer = null
        }
    }
}
