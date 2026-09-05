package com.viami.aamirror.car

import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.car.app.SurfaceContainer
import com.viami.aamirror.core.MirrorSettings
import com.viami.aamirror.mirror.SurfaceMode

/**
 * Draws a single status frame so the car screen is never plain black.
 * Locking can race with the VirtualDisplay taking over the surface —
 * that is expected and swallowed.
 *
 * Fill mode is the exception: it drives the surface through hwui, and a
 * software canvas here would take the surface for the CPU and abort the
 * process the moment the first mirrored frame arrived. There the screen
 * stays black until frames flow, and the toast on the phone carries the
 * message instead.
 */
object StatusRenderer {
    fun draw(container: SurfaceContainer, message: String) {
        val surface = container.surface ?: return
        if (!surface.isValid) return
        if (SurfaceMode.useHardware(surface, MirrorSettings.fillScreen)) return
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
