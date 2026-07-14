package com.viami.aamirror.core

import com.viami.aamirror.mirror.SurfaceTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-process bridge between the car side (Surface + buttons), the phone-side
 * MirrorService (projection + frames), and the setup activity (status UI).
 */
object MirrorGateway {
    private val _state = MutableStateFlow(MirrorState())
    val state: StateFlow<MirrorState> = _state.asStateFlow()

    private val _surfaceTarget = MutableStateFlow<SurfaceTarget?>(null)
    val surfaceTarget: StateFlow<SurfaceTarget?> = _surfaceTarget.asStateFlow()

    fun attachSurface(target: SurfaceTarget) {
        _surfaceTarget.value = target
        dispatch(MirrorEvent.SurfaceAvailable)
    }

    fun detachSurface() {
        _surfaceTarget.value = null
        dispatch(MirrorEvent.SurfaceDestroyed)
    }

    fun dispatch(event: MirrorEvent) {
        _state.update { it.reduce(event) }
    }
}
