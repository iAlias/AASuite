package com.viami.aamirror.car

import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.car.app.SurfaceContainer

/**
 * Draws a single status frame so the car screen is never plain black.
 * Locking can race with the VirtualDisplay taking over the surface —
 * that is expected and swallowed.
 */
object StatusRenderer {
    fun draw(container: SurfaceContainer, message: String) {
        val surface = container.surface ?: return
        if (!surface.isValid) return
        try {
            val canvas = surface.lockCanvas(null) ?: return
            try {
                canvas.drawColor(Color.BLACK)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = container.height / 14f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    message,
                    container.width / 2f,
                    container.height / 2f,
                    paint,
                )
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "surface busy, skipping status frame", e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "surface busy, skipping status frame", e)
        }
    }

    private const val TAG = "StatusRenderer"
}
