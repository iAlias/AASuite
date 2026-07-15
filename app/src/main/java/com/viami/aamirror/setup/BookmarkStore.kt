package com.viami.aamirror.setup

import android.content.Context
import com.viami.aamirror.core.Bookmark
import com.viami.aamirror.core.BookmarkCodec

/** SharedPreferences-backed favorites list; order is the list order. */
object BookmarkStore {

    private const val PREFS = "bookmarks"
    private const val KEY = "list"

    val defaults = listOf(
        Bookmark("Google", "www.google.com"),
        Bookmark("YouTube", "www.youtube.com"),
    )

    fun load(context: Context): List<Bookmark> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return defaults
        return BookmarkCodec.decode(raw)
    }

    fun save(context: Context, bookmarks: List<Bookmark>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, BookmarkCodec.encode(bookmarks))
            .apply()
    }
}
