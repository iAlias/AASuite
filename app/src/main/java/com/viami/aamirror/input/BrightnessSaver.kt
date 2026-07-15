package com.viami.aamirror.input

import android.content.Context
import android.provider.Settings

/**
 * Drops the phone screen brightness to the minimum while mirroring (the
 * screen cannot be turned off — MediaProjection mirrors what it shows — but
 * dim it and the heat/battery drain fall sharply). Needs WRITE_SETTINGS.
 */
object BrightnessSaver {

    private var saved: Pair<Int, Int>? = null

    fun canDim(context: Context): Boolean = Settings.System.canWrite(context)

    fun dim(context: Context) {
        if (!canDim(context) || saved != null) return
        val resolver = context.contentResolver
        saved = Pair(
            Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128),
            Settings.System.getInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            ),
        )
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 1)
    }

    fun restore(context: Context) {
        val (brightness, mode) = saved ?: return
        saved = null
        if (!canDim(context)) return
        val resolver = context.contentResolver
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
    }
}
