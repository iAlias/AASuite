package com.viami.aamirror.mirror

import android.util.Log
import android.view.Surface

/**
 * Which drawing API owns a car surface.
 *
 * A Surface accepts a single producer API at a time. Lock it once with a
 * software canvas and hwui can no longer connect to it: the connect fails
 * with EINVAL and the next frame aborts the process from the RenderThread,
 * a native abort that no catch block can intercept. Fill mode drives the
 * surface through hwui (lockHardwareCanvas), the status frame and fit mode
 * drive it on the CPU, so the choice is made once, the first time we touch
 * a given Surface, and everything that draws on it afterwards follows.
 *
 * A setting toggled mid-session therefore only takes effect once the host
 * hands us a new surface, which is the price of never aborting.
 */
object SurfaceMode {

    private var surface: Surface? = null
    private var hardware = false

    /**
     * Claims [candidate] for hwui when [preferHardware] and the surface is
     * still untouched. Returns whether hwui may drive it.
     */
    @Synchronized
    fun useHardware(candidate: Surface, preferHardware: Boolean): Boolean {
        if (surface !== candidate) {
            surface = candidate
            hardware = preferHardware
            Log.i(TAG, "surface claimed for ${if (hardware) "hwui" else "cpu"}")
        } else if (hardware != preferHardware) {
            Log.i(TAG, "surface already bound to ${if (hardware) "hwui" else "cpu"}, " +
                "ignoring the request for ${if (preferHardware) "hwui" else "cpu"}")
        }
        return hardware
    }

    private const val TAG = "SurfaceMode"
}
