package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyScrollTest {

    @Test
    fun `small distances add up to one press`() {
        val scroll = KeyScroll(step = 80f)

        assertEquals(emptyList<ArrowKey>(), scroll.advance(0f, 50f))
        assertEquals(listOf(ArrowKey.DOWN), scroll.advance(0f, 40f))
        assertEquals(emptyList<ArrowKey>(), scroll.advance(0f, 60f))
    }

    @Test
    fun `finger moving down presses up`() {
        val scroll = KeyScroll(step = 80f)

        assertEquals(listOf(ArrowKey.UP, ArrowKey.UP), scroll.advance(0f, -170f))
    }

    @Test
    fun `horizontal drag presses left and right`() {
        val scroll = KeyScroll(step = 80f)

        assertEquals(listOf(ArrowKey.RIGHT), scroll.advance(90f, 10f))
        assertEquals(listOf(ArrowKey.LEFT), scroll.advance(-90f, 0f))
    }

    @Test
    fun `switching axis drops the other axis remainder`() {
        val scroll = KeyScroll(step = 80f)

        scroll.advance(0f, 70f)
        scroll.advance(70f, 0f)
        assertEquals(emptyList<ArrowKey>(), scroll.advance(0f, 20f))
    }

    @Test
    fun `a fast fling is capped`() {
        val scroll = KeyScroll(step = 80f)

        assertEquals(KeyScroll.MAX_PRESSES, scroll.advance(0f, 2000f).size)
    }

    @Test
    fun `reset clears the remainder`() {
        val scroll = KeyScroll(step = 80f)

        scroll.advance(0f, 70f)
        scroll.reset()
        assertEquals(emptyList<ArrowKey>(), scroll.advance(0f, 20f))
    }
}
