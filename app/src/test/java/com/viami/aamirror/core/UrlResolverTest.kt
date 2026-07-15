package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlResolverTest {

    @Test
    fun `blank input goes to the home page`() {
        assertEquals(UrlResolver.HOME, UrlResolver.resolve("   "))
    }

    @Test
    fun `full urls pass through untouched`() {
        assertEquals(
            "https://example.com/watch?v=1",
            UrlResolver.resolve("https://example.com/watch?v=1"),
        )
        assertEquals("http://example.com", UrlResolver.resolve("http://example.com"))
    }

    @Test
    fun `bare hosts get an https scheme`() {
        assertEquals("https://www.youtube.com", UrlResolver.resolve("www.youtube.com"))
        assertEquals("https://google.com/maps", UrlResolver.resolve(" google.com/maps "))
    }

    @Test
    fun `free text becomes a google search`() {
        assertEquals(
            "https://www.google.com/search?q=meteo+domani",
            UrlResolver.resolve("meteo domani"),
        )
    }

    @Test
    fun `text with a dot but with spaces is still a search`() {
        assertEquals(
            "https://www.google.com/search?q=orari+trenitalia+roma+t.no",
            UrlResolver.resolve("orari trenitalia roma t.no"),
        )
    }
}
