package com.viami.aamirror.mirror

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.Surface
import com.viami.aamirror.core.MirrorSettings

/**
 * Mirrors the phone screen onto the car surface through a VirtualDisplay.
 * Android 14 forbids a second createVirtualDisplay on the same projection,
 * so the display is created once and re-targeted with resize() + setSurface().
 *
 * Fit mode targets the car surface directly (the system compositor
 * letterboxes). Fill mode targets a FillRenderer that center-crops every
 * frame onto the car surface; phone rotation re-builds that pipeline.
 */
class LocalScreenSource(
    private val context: Context,
    private val projection: MediaProjection,
) : FrameSource {

    private val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val handler = Handler(Looper.getMainLooper())
    private var virtualDisplay: VirtualDisplay? = null
    private var fillRenderer: FillRenderer? = null
    private var lastTarget: SurfaceTarget? = null
    private var lastPhoneSize: Pair<Int, Int>? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) return
            if (fillRenderer == null) return
            val target = lastTarget ?: return
            // Rotation changes the phone size: rebuild the crop pipeline.
            if (PhoneDisplay.currentSize(context) != lastPhoneSize) attach(target)
        }
    }

    init {
        displayManager.registerDisplayListener(displayListener, handler)
    }

    override fun attach(target: SurfaceTarget) {
        lastTarget = target
        fillRenderer?.release()
        fillRenderer = null
        if (MirrorSettings.fillScreen && Build.VERSION.SDK_INT >= 29) {
            val (phoneWidth, phoneHeight) = PhoneDisplay.currentSize(context)
            lastPhoneSize = phoneWidth to phoneHeight
            val renderer = FillRenderer(target, phoneWidth, phoneHeight)
            fillRenderer = renderer
            Log.i(TAG, "fill: phone ${phoneWidth}x$phoneHeight cropped onto " +
                "car ${target.width}x${target.height}")
            retarget(phoneWidth, phoneHeight, target.densityDpi, renderer.surface)
        } else {
            Log.i(TAG, "fit: car ${target.width}x${target.height}")
            retarget(target.width, target.height, target.densityDpi, target.surface)
        }
    }

    private fun retarget(width: Int, height: Int, densityDpi: Int, surface: Surface) {
        val existing = virtualDisplay
        if (existing == null) {
            virtualDisplay = projection.createVirtualDisplay(
                "aa-mirror",
                width, height, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, handler,
            )
        } else {
            existing.resize(width, height, densityDpi)
            existing.setSurface(surface)
        }
    }

    override fun detach() {
        virtualDisplay?.surface = null
        fillRenderer?.release()
        fillRenderer = null
    }

    override fun release() {
        detach()
        virtualDisplay?.release()
        virtualDisplay = null
        displayManager.unregisterDisplayListener(displayListener)
    }

    private companion object {
        const val TAG = "LocalScreenSource"
    }
}
