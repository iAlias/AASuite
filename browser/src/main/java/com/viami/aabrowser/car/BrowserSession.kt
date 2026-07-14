package com.viami.aabrowser.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class BrowserSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen = BrowserScreen(carContext)
}
