package com.viami.aamirror.car

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer

/** A mode that can own the car surface (screen mirroring, browser, ...). */
interface SurfaceSink {
    fun onAttach(container: SurfaceContainer)
    fun onDetach()
    fun onTap(x: Float, y: Float) {}
    fun onScroll(distanceX: Float, distanceY: Float) {}
}

/**
 * The single SurfaceCallback registered with the host. The host does not
 * re-deliver onSurfaceAvailable when the callback changes, so the router is
 * registered once per session and the active mode is swapped underneath it.
 */
object SurfaceRouter {

    var container: SurfaceContainer? = null
        private set

    private var sink: SurfaceSink? = null

    private val callback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            container = surfaceContainer
            sink?.onAttach(surfaceContainer)
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            sink?.onDetach()
            container = null
        }

        override fun onClick(x: Float, y: Float) {
            sink?.onTap(x, y)
        }

        override fun onScroll(distanceX: Float, distanceY: Float) {
            sink?.onScroll(distanceX, distanceY)
        }
    }

    fun register(carContext: CarContext) {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(callback)
    }

    fun isActive(candidate: SurfaceSink): Boolean = sink === candidate

    /** Swaps the surface owner; no-op when the sink is already active. */
    fun setSink(newSink: SurfaceSink?) {
        if (newSink === sink) return
        sink?.onDetach()
        sink = newSink
        val current = container ?: return
        newSink?.onAttach(current)
    }
}
