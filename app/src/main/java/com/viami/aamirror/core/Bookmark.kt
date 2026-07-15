package com.viami.aamirror.core

data class Bookmark(val title: String, val url: String)

/**
 * Line-based persistence format: one bookmark per line, "title<TAB>url".
 * Tabs/newlines cannot appear in either field (flattened to spaces), so no
 * escaping is needed.
 */
object BookmarkCodec {

    fun encode(bookmarks: List<Bookmark>): String =
        bookmarks.joinToString("\n") { "${flatten(it.title)}\t${flatten(it.url)}" }

    fun decode(raw: String): List<Bookmark> =
        raw.lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('\t')
                if (separator <= 0) return@mapNotNull null
                val title = line.substring(0, separator).trim()
                val url = line.substring(separator + 1).trim()
                if (title.isEmpty() || url.isEmpty()) null else Bookmark(title, url)
            }
            .toList()

    private fun flatten(text: String): String =
        text.replace(Regex("[\\t\\n\\r]+"), " ").trim()
}
