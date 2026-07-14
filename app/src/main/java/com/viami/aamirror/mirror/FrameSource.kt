package com.viami.aamirror.mirror

import android.view.Surface

data class SurfaceTarget(
    val surface: Surface,
    val width: Int,
    val height: Int,
    val densityDpi: Int,
)

/**
 * Produces frames onto a car-display surface. Today the only implementation
 * captures the local phone screen; phase 2 adds a WiFi stream from a second
 * phone as another implementation, without touching the car side.
 */
interface FrameSource {
    /** Start (or move) output onto [target]. Safe to call again with a new target. */
    fun attach(target: SurfaceTarget)

    /** Stop producing frames but keep the source alive for a later [attach]. */
    fun detach()

    /** Tear down permanently. The instance must not be reused afterwards. */
    fun release()
}
