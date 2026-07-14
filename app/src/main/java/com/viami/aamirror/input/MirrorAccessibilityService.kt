package com.viami.aamirror.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class MirrorAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        var instance: MirrorAccessibilityService? = null
            private set

        fun pressBack(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false

        fun pressHome(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false

        /** Taps the phone screen at ([x], [y]) in current display coordinates. */
        fun tap(x: Float, y: Float): Boolean = dispatch(
            Path().apply { moveTo(x, y) },
            durationMs = 50,
        )

        /** Swipes the phone screen from ([startX], [startY]) to ([endX], [endY]). */
        fun swipe(
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
            durationMs: Long,
        ): Boolean = dispatch(
            Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            },
            durationMs,
        )

        private fun dispatch(path: Path, durationMs: Long): Boolean {
            val service = instance ?: return false
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()
            return service.dispatchGesture(gesture, null, null)
        }
    }
}
