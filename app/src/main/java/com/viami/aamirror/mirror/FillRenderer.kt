package com.viami.aamirror.mirror

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import com.viami.aamirror.core.AspectFit

/**
 * Fill ("center crop") pipeline: the projection renders the phone screen at
 * native size into an ImageReader, and every frame is blitted onto the car
 * surface with a hardware canvas, scaled so it covers the whole display.
 */
@RequiresApi(29)
class FillRenderer(
    private val target: SurfaceTarget,
    phoneWidth: Int,
    phoneHeight: Int,
) {

    private val thread = HandlerThread("fill-renderer").apply { start() }
    private val handler = Handler(thread.looper)
    @Volatile
    private var released = false

    private val reader: ImageReader = ImageReader.newInstance(
        phoneWidth,
        phoneHeight,
        PixelFormat.RGBA_8888,
        3,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
    )

    /** Where the VirtualDisplay must render. */
    val surface: Surface
        get() = reader.surface

    init {
        val crop = AspectFit.sourceCrop(phoneWidth, phoneHeight, target.width, target.height)
        val src = Rect(crop.x, crop.y, crop.x + crop.width, crop.y + crop.height)
        val dst = Rect(0, 0, target.width, target.height)
        reader.setOnImageAvailableListener({ r ->
            val image = try {
                r.acquireLatestImage()
            } catch (e: IllegalStateException) {
                null
            } ?: return@setOnImageAvailableListener
            try {
                if (released) return@setOnImageAvailableListener
                val buffer = image.hardwareBuffer ?: return@setOnImageAvailableListener
                buffer.use { hb ->
                    val bitmap = Bitmap.wrapHardwareBuffer(hb, null) ?: return@use
                    drawFrame(bitmap, src, dst)
                    bitmap.recycle()
                }
            } catch (e: Exception) {
                Log.w(TAG, "frame blit failed", e)
            } finally {
                image.close()
            }
        }, handler)
    }

    private fun drawFrame(bitmap: Bitmap, src: Rect, dst: Rect) {
        val canvas = try {
            target.surface.lockHardwareCanvas()
        } catch (e: IllegalArgumentException) {
            return
        } catch (e: IllegalStateException) {
            return
        }
        try {
            canvas.drawBitmap(bitmap, src, dst, null)
        } finally {
            target.surface.unlockCanvasAndPost(canvas)
        }
    }

    fun release() {
        released = true
        reader.setOnImageAvailableListener(null, null)
        handler.post {
            reader.close()
            thread.quitSafely()
        }
    }

    private companion object {
        const val TAG = "FillRenderer"
    }
}
