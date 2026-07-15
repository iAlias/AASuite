package com.viami.aamirror.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareHttpTest {

    @Test
    fun `parses method path and content length`() {
        val head = ShareHttp.parseHead(
            listOf("POST /open HTTP/1.1", "Host: 192.168.43.1", "Content-Length: 27")
        )
        assertEquals(ShareHead("POST", "/open", 27), head)
    }

    @Test
    fun `content length defaults to zero and header name is case-insensitive`() {
        assertEquals(0, ShareHttp.parseHead(listOf("GET / HTTP/1.1"))!!.contentLength)
        assertEquals(
            9,
            ShareHttp.parseHead(listOf("POST /open HTTP/1.1", "content-length: 9"))!!.contentLength,
        )
    }

    @Test
    fun `malformed request line is rejected`() {
        assertNull(ShareHttp.parseHead(listOf("garbage")))
        assertNull(ShareHttp.parseHead(emptyList()))
    }
}
