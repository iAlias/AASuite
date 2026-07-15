package com.viami.aamirror.car

import androidx.car.app.CarContext
import androidx.car.app.SurfaceContainer
import com.viami.aamirror.browser.BrowserDisplay

/**
 * The one browser surface owner, shared by every browser screen so that
 * moving between the bookmarks list and the page never rebuilds the WebView.
 */
class BrowserSink(private val carContext: CarContext) : SurfaceSink {

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

    companion object {
        private var shared: BrowserSink? = null

        fun of(carContext: CarContext): BrowserSink =
            shared ?: BrowserSink(carContext).also { shared = it }
    }
}
