package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkCodecTest {

    @Test
    fun `roundtrip preserves items and order`() {
        val list = listOf(
            Bookmark("YouTube", "www.youtube.com"),
            Bookmark("Meteo", "www.ilmeteo.it"),
            Bookmark("Google", "www.google.com"),
        )
        assertEquals(list, BookmarkCodec.decode(BookmarkCodec.encode(list)))
    }

    @Test
    fun `empty input decodes to empty list`() {
        assertEquals(emptyList<Bookmark>(), BookmarkCodec.decode(""))
    }

    @Test
    fun `tabs and newlines in titles are flattened`() {
        val encoded = BookmarkCodec.encode(listOf(Bookmark("A\tB\nC", "example.com")))
        assertEquals(listOf(Bookmark("A B C", "example.com")), BookmarkCodec.decode(encoded))
    }

    @Test
    fun `malformed lines are skipped`() {
        val decoded = BookmarkCodec.decode("solo-una-colonna\nTitolo\twww.ok.it\n\t\n")
        assertEquals(listOf(Bookmark("Titolo", "www.ok.it")), decoded)
    }
}
