package com.viami.aamirror.share

import com.viami.aamirror.core.SharedTextParser
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.logging.Logger

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
            Logger.getLogger("ShareServer").warning("port $port unavailable: $e")
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
            } catch (t: Throwable) {
                // Last-resort backstop: a hostile client (or an OOM from a bogus
                // Content-Length) must never kill the server thread or the app.
            }
        }
    }

    private fun handle(client: Socket) {
        client.use { c ->
            c.soTimeout = 3000
            val input = c.getInputStream()
            val head = readHead(input) ?: return respond(c, 400)
            val parsed = ShareHttp.parseHead(head) ?: return respond(c, 400)
            if (parsed.method != "POST" || parsed.path != "/open") return respond(c, 404)
            if (parsed.contentLength < 0 || parsed.contentLength > MAX_BODY_BYTES) {
                return respond(c, 400)
            }
            val body = readBody(input, parsed.contentLength)
            val url = SharedTextParser.firstUrl(body) ?: return respond(c, 400)
            onUrl(url)
            respond(c, 200)
        }
    }

    /** Reads header bytes until the blank line; null if malformed or over 8 KB. */
    private fun readHead(input: java.io.InputStream): List<String>? {
        val buffer = java.io.ByteArrayOutputStream()
        var trailing = 0 // consecutive bytes of the \r\n\r\n terminator seen
        while (buffer.size() < MAX_HEAD_BYTES) {
            val byte = input.read()
            if (byte < 0) return null
            buffer.write(byte)
            trailing = when {
                byte == '\r'.code && (trailing == 0 || trailing == 2) -> trailing + 1
                byte == '\n'.code && (trailing == 1 || trailing == 3) -> trailing + 1
                else -> 0
            }
            if (trailing == 4) {
                return buffer.toString(Charsets.UTF_8.name())
                    .split("\r\n")
                    .filter { it.isNotEmpty() }
            }
        }
        return null
    }

    private fun readBody(input: java.io.InputStream, length: Int): String {
        if (length <= 0) return ""
        val bytes = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(bytes, read, length - read)
            if (n < 0) break
            read += n
        }
        return String(bytes, 0, read, Charsets.UTF_8)
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
        private const val MAX_HEAD_BYTES = 8 * 1024
        private const val MAX_BODY_BYTES = 64 * 1024
    }
}
