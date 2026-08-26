package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollGestureTest {

    @Test
    fun `origin is the centre of the display`() {
        val origin = ScrollGesture.origin(800, 400)

        assertEquals(400f, origin.x, 0.01f)
        assertEquals(200f, origin.y, 0.01f)
    }

    @Test
    fun `a finger moving up scrolls the content down`() {
        // GestureDetector convention: positive distanceY = finger moved up.
        val step = ScrollGesture.advance(GesturePoint(400f, 200f), 0f, 50f, 800, 400)

        assertEquals(150f, step.point.y, 0.01f)
        assertFalse(step.restart)
    }

    @Test
    fun `a finger moving down keeps the horizontal position`() {
        val step = ScrollGesture.advance(GesturePoint(400f, 200f), 0f, -30f, 800, 400)

        assertEquals(230f, step.point.y, 0.01f)
        assertEquals(400f, step.point.x, 0.01f)
    }

    @Test
    fun `horizontal distance moves the finger the opposite way`() {
        val step = ScrollGesture.advance(GesturePoint(400f, 200f), 40f, 0f, 800, 400)

        assertEquals(360f, step.point.x, 0.01f)
    }

    @Test
    fun `reaching the top edge clamps the point and asks for a restart`() {
        val step = ScrollGesture.advance(GesturePoint(400f, 30f), 0f, 200f, 800, 400)

        assertTrue(step.restart)
        assertTrue("stays inside the display", step.point.y >= 0f)
        assertEquals(ScrollGesture.MARGIN, step.point.y, 0.01f)
    }

    @Test
    fun `reaching the bottom edge clamps the point and asks for a restart`() {
        val step = ScrollGesture.advance(GesturePoint(400f, 370f), 0f, -200f, 800, 400)

        assertTrue(step.restart)
        assertEquals(400f - ScrollGesture.MARGIN, step.point.y, 0.01f)
    }

    @Test
    fun `a display smaller than the margins keeps the finger at its centre`() {
        val step = ScrollGesture.advance(GesturePoint(10f, 10f), 0f, 5f, 20, 20)

        assertEquals(10f, step.point.x, 0.01f)
        assertEquals(10f, step.point.y, 0.01f)
    }

    @Test
    fun `a step that stays well inside never restarts`() {
        val step = ScrollGesture.advance(GesturePoint(400f, 200f), 5f, 5f, 800, 400)

        assertFalse(step.restart)
    }
}
