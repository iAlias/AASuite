package com.viami.aamirror.car

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.Surface
import androidx.car.app.SurfaceContainer
import com.viami.aamirror.core.MirrorSettings
import com.viami.aamirror.mirror.SurfaceMode

/**
 * Draws a single status frame so the car screen is never plain black.
 * Locking can race with the VirtualDisplay taking over the surface —
 * that is expected and swallowed.
 *
 * The frame follows whichever API owns the surface (see [SurfaceMode]):
 * a software canvas here would lock fill mode out of hwui and abort the
 * process the moment the first mirrored frame arrived.
 */
object StatusRenderer {
    fun draw(container: SurfaceContainer, message: String) {
        val surface = container.surface ?: return
        if (!surface.isValid) return
        val hardware = SurfaceMode.useHardware(surface, MirrorSettings.fillScreen)
        try {
            val canvas =
                (if (hardware) surface.lockHardwareCanvas() else surface.lockCanvas(null))
                    ?: return
            try {
                paint(canvas, container, message)
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "surface busy, skipping status frame", e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "surface busy, skipping status frame", e)
        } catch (e: Surface.OutOfResourcesException) {
            Log.w(TAG, "surface out of resources, skipping status frame", e)
        }
    }

    private fun paint(canvas: Canvas, container: SurfaceContainer, message: String) {
        canvas.drawColor(Color.BLACK)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = container.height / 14f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(message, container.width / 2f, container.height / 2f, paint)
    }

    private const val TAG = "StatusRenderer"
}
