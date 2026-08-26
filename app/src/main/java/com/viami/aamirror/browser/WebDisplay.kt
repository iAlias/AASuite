package com.viami.aamirror.browser

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.viami.aamirror.core.GesturePoint
import com.viami.aamirror.core.MirrorSettings
import com.viami.aamirror.core.ScrollGesture
import com.viami.aamirror.core.UrlResolver
import com.viami.aamirror.core.WebViewport

/** Smart-TV user agent so youtube.com/tv serves the leanback interface. */
const val TV_USER_AGENT =
    "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.5) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Version/5.5 TV Safari/537.36"

/** The plain web browser mode. */
val BrowserDisplay: WebDisplay = WebDisplay(homeUrl = UrlResolver.HOME)

/**
 * The YouTube "TV" mode, cast target for the second phone. The TV interface
 * is built for a 1280-wide screen; the car surface is half that, so the
 * WebView zooms out until the page has that much room to lay itself out in.
 */
val YouTubeDisplay: WebDisplay = WebDisplay(
    homeUrl = "https://www.youtube.com/tv",
    userAgent = TV_USER_AGENT,
    logicalWidth = 1280,
)

/**
 * Renders a WebView straight onto the car screen: the car Surface backs a
 * private VirtualDisplay, a Presentation shows the WebView on it, and car
 * taps/scrolls are injected into the view hierarchy as MotionEvents.
 */
class WebDisplay(
    private val homeUrl: String,
    private val userAgent: String? = null,
    private val logicalWidth: Int? = null,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var webView: WebView? = null

    /** Survives detach/attach cycles so the page comes back after focus loss. */
    private var currentUrl: String = homeUrl

    /** State of the drag the car scroll events are being folded into. */
    private var gestureActive = false
    private var fingerPoint = GesturePoint(0f, 0f)
    private var gestureDownTime = 0L
    private val liftFinger = Runnable { finishGesture() }

    val isAttached: Boolean
        get() = presentation != null

    @SuppressLint("SetJavaScriptEnabled")
    fun attach(context: Context, surface: Surface, width: Int, height: Int, densityDpi: Int) {
        mainHandler.post {
            detachNow()
            // The host paints the virtual display pixel for pixel, so its
            // resolution must match the surface: a bigger one only crops.
            val scalePercent = logicalWidth
                ?.let { WebViewport.initialScalePercent(width, it) }
                ?: 100
            Log.i(TAG, "attach surface=${width}x$height dpi=$densityDpi scale=$scalePercent%")
            val manager = context.getSystemService(DisplayManager::class.java)
            val display = manager.createVirtualDisplay(
                "AAWeb",
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
                userAgent?.let { settings.userAgentString = it }
                if (scalePercent != 100) {
                    // Zoom out so the page lays itself out for a wide screen
                    // while still being drawn on the small car surface.
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    setInitialScale(scalePercent)
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        currentUrl = url
                        applyVideoFit(view)
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
        cancelGesture()
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

    /**
     * Back for both kinds of page. An ordinary site is walked back through
     * the history; a TV app keeps its own routing and leaves the history
     * empty, so it gets the remote's back key as a keyboard event injected
     * into the page — a native key event would need the WebView to hold the
     * focus, which it does not inside a Presentation.
     */
    fun goBack() {
        mainHandler.post {
            val web = webView ?: return@post
            if (web.canGoBack()) {
                web.goBack()
                return@post
            }
            web.evaluateJavascript(BACK_KEY_JS, null)
        }
    }

    fun goHome() = loadUrl(homeUrl)

    fun reload() {
        mainHandler.post { webView?.reload() }
    }

    /** Re-applies the fill/fit choice to a page that is already loaded. */
    fun refreshVideoFit() {
        mainHandler.post { webView?.let { applyVideoFit(it) } }
    }

    fun tap(x: Float, y: Float) {
        mainHandler.post {
            val target = decorView() ?: return@post
            finishGesture()
            val downTime = SystemClock.uptimeMillis()
            dispatch(target, MotionEvent.ACTION_DOWN, GesturePoint(x, y), downTime)
            dispatch(target, MotionEvent.ACTION_UP, GesturePoint(x, y), downTime)
        }
    }

    /**
     * The host reports scrolling as a stream of small distances. Replaying
     * each one with WebView.scrollBy only moves the root document, which
     * modern pages never scroll; folding them into one real drag lets the
     * page scroll whichever container is under the finger.
     */
    fun scroll(distanceX: Float, distanceY: Float) {
        mainHandler.post {
            val target = decorView() ?: return@post
            if (!gestureActive) beginGesture(target)
            val step = ScrollGesture.advance(
                fingerPoint, distanceX, distanceY, target.width, target.height
            )
            fingerPoint = step.point
            dispatch(target, MotionEvent.ACTION_MOVE, fingerPoint, gestureDownTime)
            mainHandler.removeCallbacks(liftFinger)
            if (step.restart) {
                // The finger reached an edge: lift it so the next event
                // starts a fresh drag from the centre.
                finishGesture()
            } else {
                mainHandler.postDelayed(liftFinger, GESTURE_IDLE_MS)
            }
        }
    }

    private fun beginGesture(target: View) {
        gestureDownTime = SystemClock.uptimeMillis()
        fingerPoint = ScrollGesture.origin(target.width, target.height)
        dispatch(target, MotionEvent.ACTION_DOWN, fingerPoint, gestureDownTime)
        gestureActive = true
    }

    private fun finishGesture() {
        if (!gestureActive) return
        gestureActive = false
        mainHandler.removeCallbacks(liftFinger)
        val target = decorView() ?: return
        dispatch(target, MotionEvent.ACTION_UP, fingerPoint, gestureDownTime)
    }

    private fun cancelGesture() {
        mainHandler.removeCallbacks(liftFinger)
        gestureActive = false
    }

    private fun decorView(): View? = presentation?.window?.decorView

    private fun dispatch(target: View, action: Int, point: GesturePoint, downTime: Long) {
        val event = MotionEvent.obtain(
            downTime, SystemClock.uptimeMillis(), action, point.x, point.y, 0
        ).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        target.dispatchTouchEvent(event)
        event.recycle()
    }

    /**
     * The car display is wider than 16:9, so a video letterboxes by default.
     * With "fill screen" on it is cropped to cover the display instead.
     */
    private fun applyVideoFit(view: WebView) {
        val css = if (MirrorSettings.fillScreen) {
            "video{object-fit:cover!important;width:100%!important;height:100%!important;}"
        } else {
            "video{object-fit:contain!important;}"
        }
        val js = "(function(){var s=document.getElementById('$FIT_STYLE_ID');" +
            "if(!s){s=document.createElement('style');s.id='$FIT_STYLE_ID';" +
            "document.head.appendChild(s);}s.textContent=\"$css\";})();"
        view.evaluateJavascript(js, null)
    }

    private companion object {
        const val TAG = "WebDisplay"
        const val FIT_STYLE_ID = "aa-video-fit"

        /**
         * The back key as the TV platforms report it: 27 is Escape (what a
         * browser-hosted leanback UI listens for), 461 is webOS and 10009 is
         * Tizen. Returns the route before and after, so the log says whether
         * the page actually moved.
         */
        val BACK_KEY_JS = """
            (function () {
              var before = location.hash;
              [27, 461, 10009].forEach(function (code) {
                ['keydown', 'keyup'].forEach(function (type) {
                  var e = new KeyboardEvent(type, {
                    bubbles: true, cancelable: true, key: 'Escape', code: 'Escape'
                  });
                  Object.defineProperty(e, 'keyCode', { get: function () { return code; } });
                  Object.defineProperty(e, 'which', { get: function () { return code; } });
                  document.dispatchEvent(e);
                });
              });
              return before + ' -> ' + location.hash;
            })();
        """.trimIndent()

        /** How long after the last scroll event the finger lifts. */
        const val GESTURE_IDLE_MS = 140L
    }
}
