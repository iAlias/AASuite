package com.viami.aamirror.core

/** Arrow keys, with the DOM key names and codes a TV web app listens for. */
enum class ArrowKey(val key: String, val keyCode: Int) {
    LEFT("ArrowLeft", 37),
    UP("ArrowUp", 38),
    RIGHT("ArrowRight", 39),
    DOWN("ArrowDown", 40),
}

/**
 * Turns the car host's scroll distances into arrow-key presses, for pages
 * such as YouTube's TV interface that move focus with a remote and ignore
 * touch drags. Distances are summed per axis and each [step] pixels of
 * travel on the dominant axis becomes one press.
 */
class KeyScroll(private val step: Float = DEFAULT_STEP) {

    private var pendingX = 0f
    private var pendingY = 0f

    fun reset() {
        pendingX = 0f
        pendingY = 0f
    }

    /** Presses owed after this scroll event, at most [MAX_PRESSES]. */
    fun advance(distanceX: Float, distanceY: Float): List<ArrowKey> {
        // GestureDetector convention: a positive distance means the finger
        // moved up/left, i.e. the content should move down/right.
        if (kotlin.math.abs(distanceY) >= kotlin.math.abs(distanceX)) {
            pendingX = 0f
            pendingY += distanceY
            return drain(pendingY, ArrowKey.DOWN, ArrowKey.UP) { pendingY = it }
        }
        pendingY = 0f
        pendingX += distanceX
        return drain(pendingX, ArrowKey.RIGHT, ArrowKey.LEFT) { pendingX = it }
    }

    private inline fun drain(
        pending: Float,
        positive: ArrowKey,
        negative: ArrowKey,
        store: (Float) -> Unit,
    ): List<ArrowKey> {
        val count = (pending / step).toInt()
        if (count == 0) return emptyList()
        store(pending - count * step)
        val presses = kotlin.math.abs(count).coerceAtMost(MAX_PRESSES)
        return List(presses) { if (count > 0) positive else negative }
    }

    companion object {
        const val DEFAULT_STEP = 80f
        const val MAX_PRESSES = 3
    }
}
