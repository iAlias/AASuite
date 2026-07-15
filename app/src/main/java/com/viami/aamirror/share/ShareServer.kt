package com.viami.aamirror.share

import com.viami.aamirror.core.SharedTextParser
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

/**
 * One-endpoint HTTP server: POST /open with shared text in the body opens
 * the first URL found on the car browser. Runs while the car session lives.
 */
class ShareServer(
    private val port: Int = DEFAULT_PORT,
    private val onUrl: (String) -> Unit,
) {

    @Volatile
    private var serverSocket: ServerSocket? = null
    private var thread: Thread? = null

    val boundPort: Int
        get() = serverSocket?.localPort ?: -1

    fun start() {
        if (thread != null) return
        val socket = try {
            ServerSocket(port)
        } catch (e: IOException) {
            return
        }
        serverSocket = socket
        thread = Thread({ acceptLoop(socket) }, "share-server").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            // already closed
        }
        serverSocket = null
        thread = null
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            val client = try {
                socket.accept()
            } catch (e: IOException) {
                return
            }
            try {
                handle(client)
            } catch (e: IOException) {
                // client dropped; keep serving
            }
        }
    }

    private fun handle(client: Socket) {
        client.use { c ->
            c.soTimeout = 3000
            val reader = c.getInputStream().bufferedReader()
            val headLines = mutableListOf<String>()
            while (true) {
                val line = reader.readLine() ?: return
                if (line.isEmpty()) break
                headLines.add(line)
            }
            val head = ShareHttp.parseHead(headLines) ?: return respond(c, 400)
            val body = readBody(reader, head.contentLength)
            if (head.method != "POST" || head.path != "/open") return respond(c, 404)
            val url = SharedTextParser.firstUrl(body) ?: return respond(c, 400)
            onUrl(url)
            respond(c, 200)
        }
    }

    private fun readBody(reader: java.io.BufferedReader, length: Int): String {
        if (length <= 0) return ""
        val buffer = CharArray(length)
        var read = 0
        while (read < length) {
            val n = reader.read(buffer, read, length - read)
            if (n < 0) break
            read += n
        }
        return String(buffer, 0, read)
    }

    private fun respond(client: Socket, code: Int) {
        val reason = when (code) {
            200 -> "OK"
            400 -> "Bad Request"
            else -> "Not Found"
        }
        client.getOutputStream().write(
            "HTTP/1.1 $code $reason\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                .toByteArray()
        )
        client.getOutputStream().flush()
    }

    companion object {
        const val DEFAULT_PORT = 8977
    }
}
