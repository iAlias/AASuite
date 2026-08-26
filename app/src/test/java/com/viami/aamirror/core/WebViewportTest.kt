package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewportTest {

    @Test
    fun `zooming out lets a narrow car display show a wide page`() {
        // 800 physical pixels at 62% zoom is ~1290 CSS pixels of viewport.
        assertEquals(62, WebViewport.initialScalePercent(800, 1280))
    }

    @Test
    fun `the zoom never leaves the page narrower than the target`() {
        val percent = WebViewport.initialScalePercent(800, 1280)
        val viewport = 800 * 100.0 / percent

        assertTrue("viewport $viewport should reach 1280", viewport >= 1280)
    }

    @Test
    fun `a display already wider than the target is not zoomed`() {
        assertEquals(100, WebViewport.initialScalePercent(1920, 1280))
    }

    @Test
    fun `a display exactly as wide as the target is not zoomed`() {
        assertEquals(100, WebViewport.initialScalePercent(1280, 1280))
    }

    @Test
    fun `an unknown display width means no zoom`() {
        assertEquals(100, WebViewport.initialScalePercent(0, 1280))
    }

    @Test
    fun `an unset target means no zoom`() {
        assertEquals(100, WebViewport.initialScalePercent(800, 0))
    }
}
