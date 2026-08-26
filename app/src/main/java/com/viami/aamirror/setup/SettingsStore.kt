package com.viami.aamirror.setup

import android.content.Context
import com.viami.aamirror.core.MenuLayout
import com.viami.aamirror.core.MirrorSettings

/**
 * SharedPreferences behind [MirrorSettings], which stays a plain in-memory
 * holder so the renderers can read it without a Context.
 */
object SettingsStore {

    private const val PREFS = "settings"
    private const val KEY_FILL = "fillScreen"
    private const val KEY_LAYOUT = "menuLayout"

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        MirrorSettings.fillScreen = prefs.getBoolean(KEY_FILL, false)
        MirrorSettings.menuLayout = MenuLayout.fromKey(prefs.getString(KEY_LAYOUT, null))
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FILL, MirrorSettings.fillScreen)
            .putString(KEY_LAYOUT, MirrorSettings.menuLayout.key)
            .apply()
    }
}
