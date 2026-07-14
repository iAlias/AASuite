package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MirrorStateTest {

    @Test
    fun `initial state is not mirroring`() {
        val state = MirrorState()
        assertFalse(state.isMirroring)
        assertEquals(ProjectionStatus.NONE, state.projection)
        assertFalse(state.surfaceAttached)
        assertNull(state.lastError)
    }

    @Test
    fun `projection plus surface means mirroring`() {
        val state = MirrorState()
            .reduce(MirrorEvent.PermissionRequested)
            .reduce(MirrorEvent.ProjectionAcquired)
            .reduce(MirrorEvent.SurfaceAvailable)
        assertTrue(state.isMirroring)
    }

    @Test
    fun `surface alone does not mirror`() {
        val state = MirrorState().reduce(MirrorEvent.SurfaceAvailable)
        assertFalse(state.isMirroring)
        assertTrue(state.surfaceAttached)
    }

    @Test
    fun `losing surface stops mirroring but keeps projection`() {
        val state = MirrorState()
            .reduce(MirrorEvent.ProjectionAcquired)
            .reduce(MirrorEvent.SurfaceAvailable)
            .reduce(MirrorEvent.SurfaceDestroyed)
        assertFalse(state.isMirroring)
        assertEquals(ProjectionStatus.ACTIVE, state.projection)
    }

    @Test
    fun `projection stop keeps surface attached`() {
        val state = MirrorState()
            .reduce(MirrorEvent.ProjectionAcquired)
            .reduce(MirrorEvent.SurfaceAvailable)
            .reduce(MirrorEvent.ProjectionStopped)
        assertFalse(state.isMirroring)
        assertTrue(state.surfaceAttached)
        assertEquals(ProjectionStatus.NONE, state.projection)
    }

    @Test
    fun `denied permission records error and new request clears it`() {
        val denied = MirrorState()
            .reduce(MirrorEvent.PermissionRequested)
            .reduce(MirrorEvent.PermissionDenied)
        assertEquals(ProjectionStatus.NONE, denied.projection)
        assertTrue(denied.lastError != null)

        val retried = denied.reduce(MirrorEvent.PermissionRequested)
        assertNull(retried.lastError)
        assertEquals(ProjectionStatus.REQUESTED, retried.projection)
    }
}
