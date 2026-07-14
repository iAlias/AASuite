package com.viami.aamirror.core

enum class ProjectionStatus { NONE, REQUESTED, ACTIVE }

data class MirrorState(
    val projection: ProjectionStatus = ProjectionStatus.NONE,
    val surfaceAttached: Boolean = false,
    val lastError: String? = null,
) {
    val isMirroring: Boolean
        get() = projection == ProjectionStatus.ACTIVE && surfaceAttached
}

sealed interface MirrorEvent {
    data object PermissionRequested : MirrorEvent
    data object ProjectionAcquired : MirrorEvent
    data object ProjectionStopped : MirrorEvent
    data object PermissionDenied : MirrorEvent
    data object SurfaceAvailable : MirrorEvent
    data object SurfaceDestroyed : MirrorEvent
}

fun MirrorState.reduce(event: MirrorEvent): MirrorState = when (event) {
    MirrorEvent.PermissionRequested ->
        copy(projection = ProjectionStatus.REQUESTED, lastError = null)
    MirrorEvent.ProjectionAcquired ->
        copy(projection = ProjectionStatus.ACTIVE, lastError = null)
    MirrorEvent.ProjectionStopped ->
        copy(projection = ProjectionStatus.NONE)
    MirrorEvent.PermissionDenied ->
        copy(projection = ProjectionStatus.NONE, lastError = "Permesso di cattura negato")
    MirrorEvent.SurfaceAvailable ->
        copy(surfaceAttached = true)
    MirrorEvent.SurfaceDestroyed ->
        copy(surfaceAttached = false)
}
