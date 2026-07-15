package com.viami.aamirror.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class MirrorSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = HomeScreen(carContext)
}
