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
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
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

    @Test
    fun `body with accents and emoji is decoded and the url received`() {
        val received = CopyOnWriteArrayList<String>()
        val server = ShareServer(port = 0, onUrl = received::add)
        server.start()
        try {
            val code = post(server.boundPort, "Guarda què 🎬 perché è bello https://example.com/città")
            assertEquals(200, code)
            assertEquals(listOf("https://example.com/città"), received)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `oversized content length is rejected with 400 without allocating`() {
        val server = ShareServer(port = 0, onUrl = {})
        server.start()
        try {
            java.net.Socket("127.0.0.1", server.boundPort).use { socket ->
                socket.soTimeout = 3000
                socket.getOutputStream().write(
                    "POST /open HTTP/1.1\r\nContent-Length: 2000000000\r\n\r\n"
                        .toByteArray()
                )
                val line = socket.getInputStream().bufferedReader().readLine()
                assertEquals("HTTP/1.1 400 Bad Request", line)
            }
        } finally {
            server.stop()
        }
    }
}
