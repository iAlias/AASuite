package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AspectFillTest {

    @Test
    fun `portrait phone on wide screen crops a horizontal band`() {
        val crop = AspectFit.sourceCrop(1080, 2340, 1280, 720)
        assertEquals(FitRect(0, 866, 1080, 607), crop)
    }

    @Test
    fun `landscape phone on wide screen crops thin side bands`() {
        val crop = AspectFit.sourceCrop(2340, 1080, 1280, 720)
        assertEquals(FitRect(210, 0, 1920, 1080), crop)
    }

    @Test
    fun `same aspect keeps the full frame`() {
        val crop = AspectFit.sourceCrop(1920, 1080, 1280, 720)
        assertEquals(FitRect(0, 0, 1920, 1080), crop)
    }

    @Test
    fun `fill tap in the middle of the car lands in the middle of the phone`() {
        val point = TouchMapper.mapTapFill(640f, 360f, 1280, 720, 1080, 2340)
        assertEquals(540f, point.x, 0.5f)
        assertEquals(1169.5f, point.y, 0.5f)
    }

    @Test
    fun `fill tap in the top-left corner maps to the crop origin`() {
        val point = TouchMapper.mapTapFill(0f, 0f, 1280, 720, 1080, 2340)
        assertEquals(0f, point.x, 0.5f)
        assertEquals(866f, point.y, 0.5f)
    }
}
