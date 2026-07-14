package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AspectFitTest {

    @Test
    fun `portrait phone on landscape car screen is pillarboxed`() {
        // Phone 1080x2400 into car 800x480: scale = min(800/1080, 480/2400) = 0.2
        val rect = AspectFit.fit(1080, 2400, 800, 480)
        assertEquals(FitRect(x = 292, y = 0, width = 216, height = 480), rect)
    }

    @Test
    fun `landscape phone on landscape car screen is letterboxed`() {
        // Phone 2400x1080 into car 800x480: scale = min(800/2400, 480/1080) = 1/3
        val rect = AspectFit.fit(2400, 1080, 800, 480)
        assertEquals(FitRect(x = 0, y = 60, width = 800, height = 360), rect)
    }

    @Test
    fun `same aspect ratio fills the destination`() {
        val rect = AspectFit.fit(1600, 960, 800, 480)
        assertEquals(FitRect(x = 0, y = 0, width = 800, height = 480), rect)
    }

    @Test
    fun `non positive dimensions are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { AspectFit.fit(0, 100, 800, 480) }
        assertThrows(IllegalArgumentException::class.java) { AspectFit.fit(100, 100, 800, -1) }
    }
}
