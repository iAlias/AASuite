package com.viami.aamirror.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.viami.aamirror.R
import com.viami.aamirror.browser.YouTubeDisplay

/**
 * YouTube's TV interface on the car display. Pair once from the second
 * phone (YouTube app -> Watch on TV -> enter the code shown under the TV
 * interface settings) and the cast button works like a real TV.
 */
class YouTubeScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        SurfaceRouter.setSink(WebSink.of(carContext, YouTubeDisplay))
    }

    override fun onGetTemplate(): Template {
        val strip = ActionStrip.Builder()
            .addAction(action(R.drawable.ic_menu) { screenManager.pop() })
            .addAction(action(R.drawable.ic_reload) { YouTubeDisplay.reload() })
            .build()
        return NavigationTemplate.Builder().setActionStrip(strip).build()
    }

    private fun action(iconRes: Int, onClick: () -> Unit): Action =
        Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(carContext, iconRes)
                ).build()
            )
            .setOnClickListener(onClick)
            .build()
}
