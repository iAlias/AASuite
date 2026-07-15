package com.viami.aamirror.browser

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.Surface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.viami.aamirror.core.UrlResolver

/**
 * Renders a WebView straight onto the car screen: the car Surface backs a
 * private VirtualDisplay, a Presentation shows the WebView on it, and car
 * taps/scrolls are injected into the view hierarchy as MotionEvents.
 */
object BrowserDisplay {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var webView: WebView? = null

    /** Survives detach/attach cycles so the page comes back after HUN focus loss. */
    private var currentUrl: String = UrlResolver.HOME

    val isAttached: Boolean
        get() = presentation != null

    @SuppressLint("SetJavaScriptEnabled")
    fun attach(context: Context, surface: Surface, width: Int, height: Int, densityDpi: Int) {
        mainHandler.post {
            detachNow()
            val manager = context.getSystemService(DisplayManager::class.java)
            val display = manager.createVirtualDisplay(
                "AABrowser",
                width,
                height,
                densityDpi,
                surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
            )
            val shown = Presentation(context, display.display)
            val web = WebView(shown.context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        currentUrl = url
                    }
                }
            }
            shown.setContentView(web)
            shown.show()
            web.loadUrl(currentUrl)
            virtualDisplay = display
            presentation = shown
            webView = web
        }
    }

    fun detach() {
        mainHandler.post { detachNow() }
    }

    private fun detachNow() {
        presentation?.dismiss()
        webView?.destroy()
        virtualDisplay?.release()
        presentation = null
        webView = null
        virtualDisplay = null
    }

    fun loadUrl(url: String) {
        currentUrl = url
        mainHandler.post { webView?.loadUrl(url) }
    }

    fun goBack() {
        mainHandler.post { webView?.let { if (it.canGoBack()) it.goBack() } }
    }

    fun goHome() = loadUrl(UrlResolver.HOME)

    fun reload() {
        mainHandler.post { webView?.reload() }
    }

    fun tap(x: Float, y: Float) {
        mainHandler.post {
            val target = presentation?.window?.decorView ?: return@post
            val downTime = SystemClock.uptimeMillis()
            val down = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0
            ).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
            val up = MotionEvent.obtain(
                downTime, downTime + 60, MotionEvent.ACTION_UP, x, y, 0
            ).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
            target.dispatchTouchEvent(down)
            target.dispatchTouchEvent(up)
            down.recycle()
            up.recycle()
        }
    }

    fun scroll(distanceX: Float, distanceY: Float) {
        mainHandler.post {
            // GestureDetector convention: positive distance = finger moved
            // up/left = content scrolls down/right, which matches scrollBy.
            webView?.scrollBy(distanceX.toInt(), distanceY.toInt())
        }
    }
}
