package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuLayoutTest {

    @Test
    fun `the stored key round-trips`() {
        for (layout in MenuLayout.entries) {
            assertEquals(layout, MenuLayout.fromKey(layout.key))
        }
    }

    @Test
    fun `a missing preference means the grid`() {
        assertEquals(MenuLayout.GRID, MenuLayout.fromKey(null))
    }

    @Test
    fun `an unknown key falls back to the grid`() {
        assertEquals(MenuLayout.GRID, MenuLayout.fromKey("carousel"))
    }

    @Test
    fun `toggling swaps grid and list`() {
        assertEquals(MenuLayout.LIST, MenuLayout.GRID.toggled())
        assertEquals(MenuLayout.GRID, MenuLayout.LIST.toggled())
    }
}
