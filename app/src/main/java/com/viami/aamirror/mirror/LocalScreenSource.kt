package com.viami.aamirror.mirror

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import com.viami.aamirror.core.AspectFit

/**
 * Mirrors the phone screen onto the car surface through a VirtualDisplay.
 * Android 14 forbids a second createVirtualDisplay on the same projection,
 * so the display is created once and re-targeted with resize() + setSurface().
 * Phone rotation is re-letterboxed by the system compositor automatically.
 */
class LocalScreenSource(
    context: Context,
    private val projection: MediaProjection,
) : FrameSource {

    private val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val handler = Handler(Looper.getMainLooper())
    private var virtualDisplay: VirtualDisplay? = null

    override fun attach(target: SurfaceTarget) {
        val mode = displayManager.getDisplay(Display.DEFAULT_DISPLAY).mode
        val content = AspectFit.fit(
            mode.physicalWidth, mode.physicalHeight, target.width, target.height,
        )
        Log.i(TAG, "phone ${mode.physicalWidth}x${mode.physicalHeight} -> " +
            "car ${target.width}x${target.height}, content=$content")

        val existing = virtualDisplay
        if (existing == null) {
            virtualDisplay = projection.createVirtualDisplay(
                "aa-mirror",
                target.width, target.height, target.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                target.surface, null, handler,
            )
        } else {
            existing.resize(target.width, target.height, target.densityDpi)
            existing.setSurface(target.surface)
        }
    }

    override fun detach() {
        virtualDisplay?.surface = null
    }

    override fun release() {
        virtualDisplay?.release()
        virtualDisplay = null
    }

    private companion object {
        const val TAG = "LocalScreenSource"
    }
}
