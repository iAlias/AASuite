package com.viami.aamirror.share

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareServerTest {

    private fun post(port: Int, body: String): Int {
        val conn = URL("http://127.0.0.1:$port/open").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 2000
        conn.readTimeout = 2000
        conn.outputStream.use { it.write(body.toByteArray()) }
        return conn.responseCode.also { conn.disconnect() }
    }

    @Test
    fun `posted text reaches the callback and answers 200`() {
        val received = CopyOnWriteArrayList<String>()
        val server = ShareServer(port = 0, onUrl = received::add)
        server.start()
        try {
            val code = post(server.boundPort, "guarda https://example.com/video subito")
            assertEquals(200, code)
            assertEquals(listOf("https://example.com/video"), received)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `text without url answers 400`() {
        val server = ShareServer(port = 0, onUrl = {})
        server.start()
        try {
            assertEquals(400, post(server.boundPort, "niente indirizzi"))
        } finally {
            server.stop()
        }
    }
}
