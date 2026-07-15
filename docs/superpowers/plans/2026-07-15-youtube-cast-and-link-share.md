# YouTube Cast + Link Share Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Terza modalità "YouTube Cast" in AA Suite (interfaccia TV di YouTube comandata dal secondo telefono col tasto trasmetti) + condivisione link dal secondo telefono verso il browser dell'auto via hotspot.

**Architecture:** `BrowserDisplay` (object) si generalizza nella classe `WebDisplay(homeUrl, userAgent)` con due istanze top-level (`BrowserDisplay`, `YouTubeDisplay`); un `WebSink` parametrico le collega al `SurfaceRouter` esistente. Un mini server HTTP (`ShareServer`, socket puri, porta 8977) gira finché la sessione car è viva e pubblica gli URL ricevuti su `ShareInbox`; `MirrorSession` li osserva e apre il browser. Un micro-APK separato (`companion`, `com.viami.aashare`) fa da destinazione Condividi sul secondo telefono e POSTa il testo al gateway dell'hotspot.

**Tech Stack:** Kotlin 2.1.0, AGP 8.7.3, car-app 1.4.0, WebView/Presentation (esistenti), java.net ServerSocket/HttpURLConnection, JUnit 4.

## Global Constraints

- Build sempre con: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\AI Code\AndroidAutoApp\gradlew.bat" -p "C:\AI Code\AndroidAutoApp" <task> --console=plain` (PowerShell).
- Package car app: `com.viami.aamirror` (modulo `:app`); package companion: `com.viami.aashare` (modulo `:companion`).
- Porta del server: **8977**. User-agent TV: costante `TV_USER_AGENT` in `WebDisplay.kt`.
- Le classi coperte da unit test JVM (`SharedTextParser`, `ShareHttp`, `ShareServer`, `GatewayAddress`) NON devono usare `android.util.Log` né altre API Android.
- Stringhe UI in italiano, in `res/values/strings.xml` del modulo giusto.
- A fine piano: `versionCode = 5`, `versionName = "0.5"` in `app/build.gradle.kts`.
- Commit frequenti; messaggio finale con `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.

---

### Task 1: SharedTextParser (TDD)

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/core/SharedTextParser.kt`
- Test: `app/src/test/java/com/viami/aamirror/core/SharedTextParserTest.kt`

**Interfaces:**
- Consumes: —
- Produces: `object SharedTextParser { fun firstUrl(text: String): String? }` (usato da Task 3).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedTextParserTest {

    @Test
    fun `extracts the first http url from free text`() {
        assertEquals(
            "https://youtu.be/abc123",
            SharedTextParser.firstUrl("Guarda questo! https://youtu.be/abc123 che ridere"),
        )
    }

    @Test
    fun `strips trailing punctuation`() {
        assertEquals(
            "https://example.com/p",
            SharedTextParser.firstUrl("vai su https://example.com/p."),
        )
    }

    @Test
    fun `falls back to www hosts`() {
        assertEquals("www.sito.it/pagina", SharedTextParser.firstUrl("apri www.sito.it/pagina ora"))
    }

    @Test
    fun `returns null when there is no url`() {
        assertNull(SharedTextParser.firstUrl("nessun indirizzo qui"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\AI Code\AndroidAutoApp\gradlew.bat" -p "C:\AI Code\AndroidAutoApp" :app:testDebugUnitTest --tests "com.viami.aamirror.core.SharedTextParserTest" --console=plain`
Expected: FAIL (unresolved reference `SharedTextParser`).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.viami.aamirror.core

/** Pulls the first shareable URL out of free-form shared text. */
object SharedTextParser {

    private val httpUrl = Regex("""https?://\S+""")
    private val wwwHost = Regex("""\bwww\.\S+""")

    fun firstUrl(text: String): String? {
        val match = httpUrl.find(text)?.value ?: wwwHost.find(text)?.value ?: return null
        return match.trimEnd('.', ',', ';', ')', ']', '!', '?')
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same command as Step 2. Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```powershell
git -C "C:\AI Code\AndroidAutoApp" add app/src/main/java/com/viami/aamirror/core/SharedTextParser.kt app/src/test/java/com/viami/aamirror/core/SharedTextParserTest.kt
git -C "C:\AI Code\AndroidAutoApp" commit -m "feat: extract the first URL from shared text"
```

---

### Task 2: ShareHttp, parser della richiesta (TDD)

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/share/ShareHttp.kt`
- Test: `app/src/test/java/com/viami/aamirror/share/ShareHttpTest.kt`

**Interfaces:**
- Consumes: —
- Produces: `data class ShareHead(val method: String, val path: String, val contentLength: Int)`; `object ShareHttp { fun parseHead(lines: List<String>): ShareHead? }` (usati da Task 3).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.viami.aamirror.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareHttpTest {

    @Test
    fun `parses method path and content length`() {
        val head = ShareHttp.parseHead(
            listOf("POST /open HTTP/1.1", "Host: 192.168.43.1", "Content-Length: 27")
        )
        assertEquals(ShareHead("POST", "/open", 27), head)
    }

    @Test
    fun `content length defaults to zero and header name is case-insensitive`() {
        assertEquals(0, ShareHttp.parseHead(listOf("GET / HTTP/1.1"))!!.contentLength)
        assertEquals(
            9,
            ShareHttp.parseHead(listOf("POST /open HTTP/1.1", "content-length: 9"))!!.contentLength,
        )
    }

    @Test
    fun `malformed request line is rejected`() {
        assertNull(ShareHttp.parseHead(listOf("garbage")))
        assertNull(ShareHttp.parseHead(emptyList()))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\AI Code\AndroidAutoApp\gradlew.bat" -p "C:\AI Code\AndroidAutoApp" :app:testDebugUnitTest --tests "com.viami.aamirror.share.ShareHttpTest" --console=plain`
Expected: FAIL (unresolved reference `ShareHttp`).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.viami.aamirror.share

data class ShareHead(val method: String, val path: String, val contentLength: Int)

/** Just enough HTTP parsing for the one-endpoint share server. */
object ShareHttp {

    fun parseHead(lines: List<String>): ShareHead? {
        val requestLine = lines.firstOrNull() ?: return null
        val parts = requestLine.split(' ')
        if (parts.size < 3) return null
        val contentLength = lines.drop(1)
            .firstOrNull { it.startsWith("content-length:", ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.toIntOrNull() ?: 0
        return ShareHead(parts[0], parts[1], contentLength)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same command as Step 2. Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```powershell
git -C "C:\AI Code\AndroidAutoApp" add app/src/main/java/com/viami/aamirror/share/ShareHttp.kt app/src/test/java/com/viami/aamirror/share/ShareHttpTest.kt
git -C "C:\AI Code\AndroidAutoApp" commit -m "feat: minimal HTTP head parser for the share server"
```

---

### Task 3: ShareInbox + ShareServer (TDD su socket reali JVM)

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/share/ShareInbox.kt`
- Create: `app/src/main/java/com/viami/aamirror/share/ShareServer.kt`
- Test: `app/src/test/java/com/viami/aamirror/share/ShareServerTest.kt`

**Interfaces:**
- Consumes: `ShareHttp.parseHead(List<String>): ShareHead?` (Task 2), `SharedTextParser.firstUrl(String): String?` (Task 1).
- Produces: `object ShareInbox { val links: MutableSharedFlow<String>; fun publish(url: String) }`; `class ShareServer(port: Int = 8977, onUrl: (String) -> Unit) { fun start(); fun stop(); val boundPort: Int }` (usati da Task 6).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.viami.aamirror.share

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareServerTest {

    private fun post(port: Int, body: String): Int {
        val conn = URL("http://127.0.0.1:$port/open").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 2000
        conn.readTimeout = 2000
        conn.outputStream.use { it.write(body.toByteArray()) }
        return conn.responseCode.also { conn.disconnect() }
    }

    @Test
    fun `posted text reaches the callback and answers 200`() {
        val received = CopyOnWriteArrayList<String>()
        val server = ShareServer(port = 0, onUrl = received::add)
        server.start()
        try {
            val code = post(server.boundPort, "guarda https://example.com/video subito")
            assertEquals(200, code)
            assertEquals(listOf("https://example.com/video"), received)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `text without url answers 400`() {
        val server = ShareServer(port = 0, onUrl = {})
        server.start()
        try {
            assertEquals(400, post(server.boundPort, "niente indirizzi"))
        } finally {
            server.stop()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\AI Code\AndroidAutoApp\gradlew.bat" -p "C:\AI Code\AndroidAutoApp" :app:testDebugUnitTest --tests "com.viami.aamirror.share.ShareServerTest" --console=plain`
Expected: FAIL (unresolved reference `ShareServer`).

- [ ] **Step 3: Write minimal implementation**

`ShareInbox.kt`:

```kotlin
package com.viami.aamirror.share

import kotlinx.coroutines.flow.MutableSharedFlow

/** Bridge between the share server thread and the car session. */
object ShareInbox {
    val links = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun publish(url: String) {
        links.tryEmit(url)
    }
}
```

`ShareServer.kt` (nessun uso di API Android, gira nei test JVM):

```kotlin
package com.viami.aamirror.share

import com.viami.aamirror.core.SharedTextParser
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

/**
 * One-endpoint HTTP server: POST /open with shared text in the body opens
 * the first URL found on the car browser. Runs while the car session lives.
 */
class ShareServer(
    private val port: Int = DEFAULT_PORT,
    private val onUrl: (String) -> Unit,
) {

    @Volatile
    private var serverSocket: ServerSocket? = null
    private var thread: Thread? = null

    val boundPort: Int
        get() = serverSocket?.localPort ?: -1

    fun start() {
        if (thread != null) return
        val socket = try {
            ServerSocket(port)
        } catch (e: IOException) {
            return
        }
        serverSocket = socket
        thread = Thread({ acceptLoop(socket) }, "share-server").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            // already closed
        }
        serverSocket = null
        thread = null
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            val client = try {
                socket.accept()
            } catch (e: IOException) {
                return
            }
            try {
                handle(client)
            } catch (e: IOException) {
                // client dropped; keep serving
            }
        }
    }

    private fun handle(client: Socket) {
        client.use { c ->
            c.soTimeout = 3000
            val reader = c.getInputStream().bufferedReader()
            val headLines = mutableListOf<String>()
            while (true) {
                val line = reader.readLine() ?: return
                if (line.isEmpty()) break
                headLines.add(line)
            }
            val head = ShareHttp.parseHead(headLines) ?: return respond(c, 400)
            val body = readBody(reader, head.contentLength)
            if (head.method != "POST" || head.path != "/open") return respond(c, 404)
            val url = SharedTextParser.firstUrl(body) ?: return respond(c, 400)
            onUrl(url)
            respond(c, 200)
        }
    }

    private fun readBody(reader: java.io.BufferedReader, length: Int): String {
        if (length <= 0) return ""
        val buffer = CharArray(length)
        var read = 0
        while (read < length) {
            val n = reader.read(buffer, read, length - read)
            if (n < 0) break
            read += n
        }
        return String(buffer, 0, read)
    }

    private fun respond(client: Socket, code: Int) {
        val reason = when (code) {
            200 -> "OK"
            400 -> "Bad Request"
            else -> "Not Found"
        }
        client.getOutputStream().write(
            "HTTP/1.1 $code $reason\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                .toByteArray()
        )
        client.getOutputStream().flush()
    }

    companion object {
        const val DEFAULT_PORT = 8977
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same command as Step 2. Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```powershell
git -C "C:\AI Code\AndroidAutoApp" add app/src/main/java/com/viami/aamirror/share app/src/test/java/com/viami/aamirror/share
git -C "C:\AI Code\AndroidAutoApp" commit -m "feat: share server publishing received URLs"
```

---

### Task 4: WebDisplay generalizzato (Browser + YouTube)

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/browser/WebDisplay.kt`
- Delete: `app/src/main/java/com/viami/aamirror/browser/BrowserDisplay.kt`
- Create: `app/src/main/java/com/viami/aamirror/car/WebSink.kt`
- Delete: `app/src/main/java/com/viami/aamirror/car/BrowserSink.kt`
- Modify: `app/src/main/java/com/viami/aamirror/car/BrowserScreen.kt` (riga `SurfaceRouter.setSink(BrowserSink.of(carContext))`)

**Interfaces:**
- Consumes: `SurfaceSink`, `SurfaceRouter` (esistenti).
- Produces: `class WebDisplay(homeUrl: String, userAgent: String?)` con gli stessi metodi dell'attuale `BrowserDisplay` (`attach(context, surface, width, height, densityDpi)`, `detach()`, `loadUrl(url)`, `goBack()`, `goHome()`, `reload()`, `tap(x,y)`, `scroll(dx,dy)`, `isAttached`); top-level `val BrowserDisplay: WebDisplay`, `val YouTubeDisplay: WebDisplay`, `const val TV_USER_AGENT`; `class WebSink` con `companion fun of(carContext: CarContext, display: WebDisplay): WebSink` (usati da Task 5 e 6). Gli import esistenti `com.viami.aamirror.browser.BrowserDisplay` restano validi (proprietà top-level nello stesso package).

- [ ] **Step 1: Create `WebDisplay.kt`**

Contenuto = l'attuale `BrowserDisplay.kt` trasformato in classe. Differenze rispetto all'object attuale: intestazione, costruttore, UA opzionale, `currentUrl` inizializzato a `homeUrl`, `goHome()` usa `homeUrl`, e in fondo le due istanze + costante:

```kotlin
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

/** Smart-TV user agent so youtube.com/tv serves the leanback interface. */
const val TV_USER_AGENT =
    "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.5) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Version/5.5 TV Safari/537.36"

/** The plain web browser mode. */
val BrowserDisplay: WebDisplay = WebDisplay(homeUrl = UrlResolver.HOME)

/** The YouTube "TV" mode, cast target for the second phone. */
val YouTubeDisplay: WebDisplay = WebDisplay(
    homeUrl = "https://www.youtube.com/tv",
    userAgent = TV_USER_AGENT,
)

/**
 * Renders a WebView straight onto the car screen: the car Surface backs a
 * private VirtualDisplay, a Presentation shows the WebView on it, and car
 * taps/scrolls are injected into the view hierarchy as MotionEvents.
 */
class WebDisplay(
    private val homeUrl: String,
    private val userAgent: String? = null,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var webView: WebView? = null

    /** Survives detach/attach cycles so the page comes back after focus loss. */
    private var currentUrl: String = homeUrl

    val isAttached: Boolean
        get() = presentation != null

    @SuppressLint("SetJavaScriptEnabled")
    fun attach(context: Context, surface: Surface, width: Int, height: Int, densityDpi: Int) {
        mainHandler.post {
            detachNow()
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

    fun goHome() = loadUrl(homeUrl)

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
```

- [ ] **Step 2: Delete `BrowserDisplay.kt`**

```powershell
git -C "C:\AI Code\AndroidAutoApp" rm app/src/main/java/com/viami/aamirror/browser/BrowserDisplay.kt
```

- [ ] **Step 3: Create `WebSink.kt` and delete `BrowserSink.kt`**

`app/src/main/java/com/viami/aamirror/car/WebSink.kt`:

```kotlin
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
```

```powershell
git -C "C:\AI Code\AndroidAutoApp" rm app/src/main/java/com/viami/aamirror/car/BrowserSink.kt
```

- [ ] **Step 4: Update `BrowserScreen.kt`**

Nella `onStart` sostituire `SurfaceRouter.setSink(BrowserSink.of(carContext))` con:

```kotlin
    override fun onStart(owner: LifecycleOwner) {
        SurfaceRouter.setSink(WebSink.of(carContext, BrowserDisplay))
    }
```

(nessun nuovo import per `BrowserDisplay`, era già importato; rimuovere l'import di `BrowserSink` se presente).

- [ ] **Step 5: Build + run all tests**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\AI Code\AndroidAutoApp\gradlew.bat" -p "C:\AI Code\AndroidAutoApp" :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: BUILD SUCCESSFUL, tutti i test verdi.

- [ ] **Step 6: Commit**

```powershell
git -C "C:\AI Code\AndroidAutoApp" add -A
git -C "C:\AI Code\AndroidAutoApp" commit -m "refactor: WebDisplay class with browser and YouTube TV instances"
```

---

### Task 5: YouTubeScreen + voce di menu + icona cast

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/car/YouTubeScreen.kt`
- Create: `app/src/main/res/drawable/ic_cast.xml`
- Modify: `app/src/main/java/com/viami/aamirror/car/HomeScreen.kt` (nuova riga menu dopo quella del browser)
- Modify: `app/src/main/res/values/strings.xml` (stringa `menu_youtube`)

**Interfaces:**
- Consumes: `WebSink.of(carContext, YouTubeDisplay)` e `YouTubeDisplay` (Task 4), `SurfaceRouter` esistente.
- Produces: `class YouTubeScreen(carContext: CarContext)` (usata solo da HomeScreen).

- [ ] **Step 1: Create `ic_cast.xml`**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="#FFFFFF">
    <path android:fillColor="#FF000000"
        android:pathData="M1,18v3h3C4,19.34 2.66,18 1,18zM1,14v2c2.76,0 5,2.24 5,5h2C8,17.13 4.87,14 1,14zM1,10v2c4.97,0 9,4.03 9,9h2C12,14.92 7.08,10 1,10zM21,3L3,3c-1.1,0 -2,0.9 -2,2v3h2L3,5h18v14h-7v2h7c1.1,0 2,-0.9 2,-2L23,5c0,-1.1 -0.9,-2 -2,-2z"/>
</vector>
```

- [ ] **Step 2: Create `YouTubeScreen.kt`**

```kotlin
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
```

- [ ] **Step 3: Add the menu row in `HomeScreen.kt`**

Dopo la riga del browser, dentro il builder della lista:

```kotlin
            .addItem(
                row(R.string.menu_youtube, R.drawable.ic_cast) {
                    screenManager.push(YouTubeScreen(carContext))
                }
            )
```

- [ ] **Step 4: Add the string**

In `strings.xml`, sotto `menu_browser`:

```xml
    <string name="menu_youtube">YouTube Cast</string>
```

- [ ] **Step 5: Build**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\AI Code\AndroidAutoApp\gradlew.bat" -p "C:\AI Code\AndroidAutoApp" :app:assembleDebug --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```powershell
git -C "C:\AI Code\AndroidAutoApp" add -A
git -C "C:\AI Code\AndroidAutoApp" commit -m "feat: YouTube Cast mode on the car menu"
```

---

### Task 6: ShareServer nella sessione car

**Files:**
- Modify: `app/src/main/java/com/viami/aamirror/car/MirrorSession.kt` (riscrittura completa, è di 9 righe)

**Interfaces:**
- Consumes: `ShareServer(port, onUrl)`, `ShareInbox.links`, `ShareInbox.publish` (Task 3); `BrowserDisplay.loadUrl` (Task 4); `UrlResolver.resolve`; `BrowserScreen`, `HomeScreen` esistenti.
- Produces: — (comportamento: link ricevuto → browser in primo piano).

- [ ] **Step 1: Rewrite `MirrorSession.kt`**

```kotlin
package com.viami.aamirror.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.viami.aamirror.browser.BrowserDisplay
import com.viami.aamirror.core.UrlResolver
import com.viami.aamirror.share.ShareInbox
import com.viami.aamirror.share.ShareServer
import kotlinx.coroutines.launch

class MirrorSession : Session(), DefaultLifecycleObserver {

    private val shareServer = ShareServer(onUrl = ShareInbox::publish)

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        shareServer.start()
        owner.lifecycle.coroutineScope.launch {
            ShareInbox.links.collect { url -> openInBrowser(url) }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        shareServer.stop()
    }

    private fun openInBrowser(url: String) {
        BrowserDisplay.loadUrl(UrlResolver.resolve(url))
        val manager = carContext.getCarService(ScreenManager::class.java)
        manager.popToRoot()
        manager.push(BrowserScreen(carContext))
    }

    override fun onCreateScreen(intent: Intent): Screen = HomeScreen(carContext)
}
```

- [ ] **Step 2: Build + tests**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\AI Code\AndroidAutoApp\gradlew.bat" -p "C:\AI Code\AndroidAutoApp" :app:testDebugUnitTest :app:assembleDebug --console=plain`
Expected: BUILD SUCCESSFUL, test verdi.

- [ ] **Step 3: Commit**

```powershell
git -C "C:\AI Code\AndroidAutoApp" add app/src/main/java/com/viami/aamirror/car/MirrorSession.kt
git -C "C:\AI Code\AndroidAutoApp" commit -m "feat: session-scoped share server opening links in the car browser"
```

---

### Task 7: Modulo companion "AA Share" (secondo telefono)

**Files:**
- Modify: `settings.gradle.kts` (aggiungere `include(":companion")`)
- Create: `companion/build.gradle.kts`
- Create: `companion/src/main/AndroidManifest.xml`
- Create: `companion/src/main/java/com/viami/aashare/GatewayAddress.kt`
- Create: `companion/src/main/java/com/viami/aashare/ShareActivity.kt`
- Create: `companion/src/main/res/values/strings.xml`
- Create: `companion/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `companion/src/main/res/values/ic_launcher_background.xml`
- Create: `companion/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Test: `companion/src/test/java/com/viami/aashare/GatewayAddressTest.kt`

**Interfaces:**
- Consumes: il server di Task 3 via HTTP (`POST http://<gateway>:8977/open`, body = testo condiviso).
- Produces: APK `com.viami.aashare` da installare sul secondo telefono.

- [ ] **Step 1: Register the module**

In `settings.gradle.kts` dopo `include(":app")`:

```kotlin
include(":companion")
```

- [ ] **Step 2: Create `companion/build.gradle.kts`**

```kotlin
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore/keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.viami.aashare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.viami.aashare"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    testImplementation(libs.junit)
}
```

- [ ] **Step 3: Write the failing test for `GatewayAddress`**

`companion/src/test/java/com/viami/aashare/GatewayAddressTest.kt`:

```kotlin
package com.viami.aashare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GatewayAddressTest {

    @Test
    fun `formats the little-endian dhcp gateway int`() {
        // 192.168.43.1 little-endian: 0x012BA8C0
        assertEquals("192.168.43.1", GatewayAddress.format(0x012BA8C0))
        // 192.168.1.1 little-endian: 0x0101A8C0
        assertEquals("192.168.1.1", GatewayAddress.format(0x0101A8C0))
    }

    @Test
    fun `zero means no gateway`() {
        assertNull(GatewayAddress.format(0))
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\AI Code\AndroidAutoApp\gradlew.bat" -p "C:\AI Code\AndroidAutoApp" :companion:testDebugUnitTest --console=plain`
Expected: FAIL (manca il manifest o `GatewayAddress`; se fallisce per manifest mancante, completare prima gli step 5-8 e rieseguire).

- [ ] **Step 5: Implement `GatewayAddress.kt`**

```kotlin
package com.viami.aashare

/** WifiManager.dhcpInfo.gateway is a little-endian IPv4 int. */
object GatewayAddress {

    fun format(gateway: Int): String? {
        if (gateway == 0) return null
        return listOf(0, 8, 16, 24)
            .joinToString(".") { shift -> ((gateway shr shift) and 0xff).toString() }
    }
}
```

- [ ] **Step 6: Create `AndroidManifest.xml`**

`android:usesCleartextTraffic="true"` è obbligatorio: la POST verso il gateway è HTTP in chiaro e Android 9+ la bloccherebbe.

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <application
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:usesCleartextTraffic="true">

        <activity
            android:name=".ShareActivity"
            android:exported="true"
            android:excludeFromRecents="true"
            android:theme="@android:style/Theme.Translucent.NoTitleBar">
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 7: Create `strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">AA Share</string>
    <string name="sent_ok">Inviato all\'auto ✓</string>
    <string name="send_failed">Non raggiungo il telefono dell\'auto: collegati al suo hotspot e apri AA Suite sull\'auto</string>
    <string name="no_text">Niente da condividere</string>
</resources>
```

- [ ] **Step 8: Create `ShareActivity.kt`**

```kotlin
package com.viami.aashare

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL

/** Share-sheet target: POSTs the shared text to the car phone (hotspot gateway). */
class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (text.isNullOrBlank()) {
            Toast.makeText(this, R.string.no_text, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Thread {
            val ok = send(text)
            runOnUiThread {
                Toast.makeText(
                    this,
                    if (ok) R.string.sent_ok else R.string.send_failed,
                    Toast.LENGTH_LONG,
                ).show()
                finish()
            }
        }.start()
    }

    private fun send(text: String): Boolean = try {
        val gateway = gatewayAddress() ?: return false
        val connection =
            URL("http://$gateway:8977/open").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        connection.outputStream.use { it.write(text.toByteArray()) }
        val ok = connection.responseCode == 200
        connection.disconnect()
        ok
    } catch (e: Exception) {
        false
    }

    private fun gatewayAddress(): String? {
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        return GatewayAddress.format(wifi.dhcpInfo?.gateway ?: 0)
    }
}
```

- [ ] **Step 9: Create the launcher icon**

`companion/src/main/res/values/ic_launcher_background.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#4A2B7A</color>
</resources>
```

`companion/src/main/res/drawable/ic_launcher_foreground.xml` (freccia "condividi" bianca):

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="2.2" android:scaleY="2.2" android:pivotX="12" android:pivotY="12"
        android:translateX="27.6" android:translateY="27.6">
        <path
            android:pathData="M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3 -3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92s2.92,-1.31 2.92,-2.92 -1.31,-2.92 -2.92,-2.92z"
            android:fillColor="#FFFFFF" />
    </group>
</vector>
```

`companion/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

- [ ] **Step 10: Run companion tests + build both APKs**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\AI Code\AndroidAutoApp\gradlew.bat" -p "C:\AI Code\AndroidAutoApp" :companion:testDebugUnitTest :companion:assembleRelease --console=plain`
Expected: BUILD SUCCESSFUL, 2 test verdi; APK in `companion/build/outputs/apk/release/companion-release.apk`.

- [ ] **Step 11: Commit**

```powershell
git -C "C:\AI Code\AndroidAutoApp" add -A
git -C "C:\AI Code\AndroidAutoApp" commit -m "feat: AA Share companion app posting shared links to the car phone"
```

---

### Task 8: Version bump + build completa

**Files:**
- Modify: `app/build.gradle.kts` (versionCode/versionName)

**Interfaces:**
- Consumes: tutto quanto sopra.
- Produces: AAB v0.5 pronto per il canale interno + APK companion.

- [ ] **Step 1: Bump version**

In `app/build.gradle.kts`:

```kotlin
        versionCode = 5
        versionName = "0.5"
```

- [ ] **Step 2: Full build + all tests**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; & "C:\AI Code\AndroidAutoApp\gradlew.bat" -p "C:\AI Code\AndroidAutoApp" test :app:bundleRelease :companion:assembleRelease --console=plain`
Expected: BUILD SUCCESSFUL; test totali attesi: 29 esistenti + 9 nuovi (4 SharedTextParser, 3 ShareHttp, 2 ShareServer) nel modulo app + 2 GatewayAddress nel companion.

- [ ] **Step 3: Commit**

```powershell
git -C "C:\AI Code\AndroidAutoApp" add app/build.gradle.kts
git -C "C:\AI Code\AndroidAutoApp" commit -m "chore: bump AA Suite to 0.5"
```

---

## Collaudo manuale (dopo i task)

1. **DHU**: menu → YouTube Cast → si carica l'interfaccia TV di YouTube; touch per navigare; Impostazioni → "Collega con codice TV" mostra il codice.
2. **Secondo telefono**: app YouTube → Impostazioni → "Guarda sulla TV" → inserisci codice → il tasto trasmetti elenca il dispositivo; avvia un video e comanda pausa/salta.
3. **Condivisione**: telefono auto in hotspot, secondo telefono agganciato con AA Share installata; condividi un link da Chrome → sul display si apre il browser con la pagina; senza hotspot → toast d'errore dedicato.
4. **Regressione**: mirroring, browser, preferiti, riempi schermo, risparmio energetico invariati.
