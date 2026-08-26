package com.viami.aamirror.core

data class GesturePoint(val x: Float, val y: Float)

/** One step of a drag: where the finger goes, and whether it must lift first. */
data class ScrollStep(val point: GesturePoint, val restart: Boolean)

/**
 * Turns the car host's scroll distances into the path of a finger dragging
 * on the car surface. The host reports many small distances, so the finger
 * walks from the centre of the display and lifts when it reaches an edge.
 */
object ScrollGesture {

    /** Kept away from the very edge, where a drag reads as a system gesture. */
    const val MARGIN: Float = 24f

    fun origin(width: Int, height: Int): GesturePoint =
        GesturePoint(width / 2f, height / 2f)

    fun advance(
        from: GesturePoint,
        distanceX: Float,
        distanceY: Float,
        width: Int,
        height: Int,
    ): ScrollStep {
        // GestureDetector convention: a positive distance means the finger
        // moved up/left, so the finger goes the opposite way of the content.
        val rawX = from.x - distanceX
        val rawY = from.y - distanceY
        val x = clamp(rawX, width)
        val y = clamp(rawY, height)
        return ScrollStep(GesturePoint(x, y), restart = x != rawX || y != rawY)
    }

    private fun clamp(value: Float, size: Int): Float {
        val max = size - MARGIN
        if (max <= MARGIN) return size / 2f
        return value.coerceIn(MARGIN, max)
    }
}
