package com.viami.aamirror.car

import androidx.car.app.CarContext
import androidx.car.app.SurfaceContainer
import com.viami.aamirror.browser.WebDisplay

/**
 * Surface owner for one WebDisplay; cached per display so moving between
 * screens of the same mode never rebuilds the WebView.
 */
class WebSink private constructor(
    private val carContext: CarContext,
    private val display: WebDisplay,
) : SurfaceSink {

    override fun onAttach(container: SurfaceContainer) {
        val surface = container.surface ?: return
        display.attach(carContext, surface, container.width, container.height, container.dpi)
    }

    override fun onDetach() {
        display.detach()
    }

    override fun onTap(x: Float, y: Float) {
        display.tap(x, y)
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        display.scroll(distanceX, distanceY)
    }

    companion object {
        private val cache = mutableMapOf<WebDisplay, WebSink>()

        fun of(carContext: CarContext, display: WebDisplay): WebSink =
            cache.getOrPut(display) { WebSink(carContext, display) }
    }
}
