package com.viami.aamirror.core

import java.net.URLEncoder

/**
 * Turns whatever the user typed in the car keyboard into a loadable URL:
 * full URLs pass through, bare hosts get https://, anything else becomes
 * a Google search.
 */
object UrlResolver {

    const val HOME = "https://www.google.com"

    fun resolve(input: String): String {
        val text = input.trim()
        if (text.isEmpty()) return HOME
        if (text.startsWith("http://") || text.startsWith("https://")) return text
        val looksLikeHost = ' ' !in text && '.' in text
        if (looksLikeHost) return "https://$text"
        return "https://www.google.com/search?q=" + URLEncoder.encode(text, "UTF-8")
    }
}
