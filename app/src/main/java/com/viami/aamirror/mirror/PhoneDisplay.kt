package com.viami.aamirror.mirror

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface

object PhoneDisplay {
    /**
     * Current logical size of the phone screen: the panel resolution,
     * swapped when the display is rotated. Matches both the letterboxing
     * applied to the mirrored content and dispatchGesture coordinates.
     */
    fun currentSize(context: Context): Pair<Int, Int> {
        val display = (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
            .getDisplay(Display.DEFAULT_DISPLAY)
        val mode = display.mode
        val rotated = display.rotation == Surface.ROTATION_90 ||
            display.rotation == Surface.ROTATION_270
        return if (rotated) {
            mode.physicalHeight to mode.physicalWidth
        } else {
            mode.physicalWidth to mode.physicalHeight
        }
    }
}
