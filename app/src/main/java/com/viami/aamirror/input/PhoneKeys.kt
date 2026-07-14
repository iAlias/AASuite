package com.viami.aamirror.input

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

object PhoneKeys {
    /** Toggles play/pause on the active media session. No accessibility needed. */
    fun playPause(context: Context) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.dispatchMediaKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        )
        audio.dispatchMediaKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        )
    }
}
