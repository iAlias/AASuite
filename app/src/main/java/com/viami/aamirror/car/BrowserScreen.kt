package com.viami.aamirror.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.viami.aamirror.R
import com.viami.aamirror.browser.BrowserDisplay
import com.viami.aamirror.core.UrlResolver

class BrowserScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private val sink = object : SurfaceSink {
        override fun onAttach(container: SurfaceContainer) {
            val surface = container.surface ?: return
            BrowserDisplay.attach(
                carContext, surface, container.width, container.height, container.dpi
            )
        }

        override fun onDetach() {
            BrowserDisplay.detach()
        }

        override fun onTap(x: Float, y: Float) {
            BrowserDisplay.tap(x, y)
        }

        override fun onScroll(distanceX: Float, distanceY: Float) {
            BrowserDisplay.scroll(distanceX, distanceY)
        }
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        SurfaceRouter.setSink(sink)
    }

    override fun onGetTemplate(): Template {
        val strip = ActionStrip.Builder()
            .addAction(action(R.drawable.ic_menu) { screenManager.pop() })
            .addAction(action(R.drawable.ic_back) { BrowserDisplay.goBack() })
            .addAction(action(R.drawable.ic_reload) { BrowserDisplay.reload() })
            .addAction(action(R.drawable.ic_search) { openSearch() })
            .build()
        return NavigationTemplate.Builder().setActionStrip(strip).build()
    }

    private fun openSearch() {
        screenManager.push(
            SearchScreen(carContext) { query ->
                BrowserDisplay.loadUrl(UrlResolver.resolve(query))
            }
        )
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
