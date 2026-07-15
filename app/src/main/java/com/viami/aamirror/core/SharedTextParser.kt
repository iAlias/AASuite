package com.viami.aamirror.core

/** Pulls the first shareable URL out of free-form shared text. */
object SharedTextParser {

    private val httpUrl = Regex("""https?://\S+""")
    private val wwwHost = Regex("""\bwww\.\S+""")

    fun firstUrl(text: String): String? {
        val match = httpUrl.find(text)?.value ?: wwwHost.find(text)?.value ?: return null
        return match.trimEnd('.', ',', ';', ')', ']', '!', '?')
    }
}
