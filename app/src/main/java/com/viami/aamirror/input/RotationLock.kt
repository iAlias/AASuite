package com.viami.aamirror.input

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager

/**
 * Forces the whole phone into landscape through an invisible 0x0 overlay
 * window whose layout params carry a screenOrientation request (the
 * "Rotation Control" technique). Needs the SYSTEM_ALERT_WINDOW permission.
 */
object RotationLock {

    private var overlay: View? = null
    private var windowManager: WindowManager? = null

    val isLocked: Boolean
        get() = overlay != null

    fun canLock(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Flips the lock. Returns the new state (true = locked). */
    fun toggle(context: Context): Boolean {
        if (isLocked) unlock() else lock(context)
        return isLocked
    }

    fun lock(context: Context) {
        if (overlay != null) return
        val overlayContext = overlayContext(context.applicationContext)
        val manager = overlayContext.getSystemService(WindowManager::class.java)
        val view = View(overlayContext)
        val params = WindowManager.LayoutParams(
            0, 0,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        manager.addView(view, params)
        overlay = view
        windowManager = manager
    }

    fun unlock() {
        overlay?.let { view -> windowManager?.removeView(view) }
        overlay = null
        windowManager = null
    }

    private fun overlayContext(appContext: Context): Context =
        if (Build.VERSION.SDK_INT >= 30) {
            appContext.createWindowContext(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null,
            )
        } else {
            appContext
        }
}
