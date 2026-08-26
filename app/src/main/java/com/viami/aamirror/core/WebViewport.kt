package com.viami.aamirror.core

/**
 * How far a WebView must zoom out to give a page the viewport it expects.
 *
 * The car surface is small (typically 800px wide) and the host shows the
 * virtual display pixel for pixel, so rendering on a bigger virtual display
 * only crops. Zooming the WebView out is what actually widens the viewport.
 */
object WebViewport {

    fun initialScalePercent(surfaceWidth: Int, targetWidth: Int): Int {
        if (surfaceWidth <= 0 || targetWidth <= 0 || surfaceWidth >= targetWidth) return 100
        // Rounded down, so the viewport always reaches at least targetWidth.
        return surfaceWidth * 100 / targetWidth
    }
}
