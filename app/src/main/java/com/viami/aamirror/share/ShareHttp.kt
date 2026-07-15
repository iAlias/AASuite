package com.viami.aamirror.share

data class ShareHead(val method: String, val path: String, val contentLength: Int)

/** Just enough HTTP parsing for the one-endpoint share server. */
object ShareHttp {

    fun parseHead(lines: List<String>): ShareHead? {
        val requestLine = lines.firstOrNull() ?: return null
        val parts = requestLine.split(' ')
        if (parts.size < 3) return null
        val contentLength = lines.drop(1)
            .firstOrNull { it.startsWith("content-length:", ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.toIntOrNull() ?: 0
        return ShareHead(parts[0], parts[1], contentLength)
    }
}
