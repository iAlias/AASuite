package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedTextParserTest {

    @Test
    fun `extracts the first http url from free text`() {
        assertEquals(
            "https://youtu.be/abc123",
            SharedTextParser.firstUrl("Guarda questo! https://youtu.be/abc123 che ridere"),
        )
    }

    @Test
    fun `strips trailing punctuation`() {
        assertEquals(
            "https://example.com/p",
            SharedTextParser.firstUrl("vai su https://example.com/p."),
        )
    }

    @Test
    fun `falls back to www hosts`() {
        assertEquals("www.sito.it/pagina", SharedTextParser.firstUrl("apri www.sito.it/pagina ora"))
    }

    @Test
    fun `returns null when there is no url`() {
        assertNull(SharedTextParser.firstUrl("nessun indirizzo qui"))
    }
}
