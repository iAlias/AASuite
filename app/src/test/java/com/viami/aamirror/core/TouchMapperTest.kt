package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TouchMapperTest {

    // Portrait phone 1080x2400 pillarboxed into car 800x480:
    // content = FitRect(292, 0, 216, 480)
    private val content = FitRect(x = 292, y = 0, width = 216, height = 480)

    @Test
    fun `tap at content center maps to phone center`() {
        val point = TouchMapper.mapTap(400f, 240f, content, 1080, 2400)!!
        assertEquals(540f, point.x, 0.5f)
        assertEquals(1200f, point.y, 0.5f)
    }

    @Test
    fun `tap at content origin maps to phone origin`() {
        val point = TouchMapper.mapTap(292f, 0f, content, 1080, 2400)!!
        assertEquals(0f, point.x, 0.001f)
        assertEquals(0f, point.y, 0.001f)
    }

    @Test
    fun `tap on left black bar is ignored`() {
        assertNull(TouchMapper.mapTap(100f, 240f, content, 1080, 2400))
    }

    @Test
    fun `tap on right black bar is ignored`() {
        assertNull(TouchMapper.mapTap(700f, 240f, content, 1080, 2400))
    }

    @Test
    fun `tap outside surface is ignored`() {
        assertNull(TouchMapper.mapTap(-5f, 240f, content, 1080, 2400))
        assertNull(TouchMapper.mapTap(400f, 500f, content, 1080, 2400))
    }
}
