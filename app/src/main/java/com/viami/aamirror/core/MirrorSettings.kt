package com.viami.aamirror.core

/** In-memory mode flags, toggled from the car menu. */
object MirrorSettings {
    /** true = center-crop the phone screen so it fills the whole car display. */
    @Volatile
    var fillScreen: Boolean = false
}
