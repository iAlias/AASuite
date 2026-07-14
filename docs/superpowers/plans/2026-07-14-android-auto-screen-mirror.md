# Android Auto Screen Mirror — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Personal-use Android app that mirrors the phone screen onto the Android Auto car display via MediaProjection → VirtualDisplay → car Surface, with three action-strip buttons (play/pause, back, home).

**Architecture:** The app declares itself an Android Auto *navigation* app (the only category that gets a free-draw `Surface`). A foreground service owns the MediaProjection session and renders the phone screen onto the car Surface through a `FrameSource` abstraction (today: local `VirtualDisplay`; phase 2: WiFi stream from a second phone). Car side and phone side communicate in-process through a singleton `MirrorGateway` holding `StateFlow`s.

**Tech Stack:** Kotlin 2.1, AGP 8.7.3, single Gradle module, `androidx.car.app:app:1.4.0`, coroutines, JUnit 4 for pure unit tests. No other libraries.

**Spec:** `docs/superpowers/specs/2026-07-14-android-auto-screen-mirror-design.md`

## Global Constraints

- `applicationId` / package: `com.viami.aamirror` — app label: **"AA Mirror"**
- `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`, JVM target 17
- Single Gradle module `:app`. No AppCompat, no Compose, no DI framework, no third-party libs.
- Personal use only: never add Play Store publishing config. Works only with Android Auto developer mode + "Unknown sources" enabled on the phone.
- Car category: `androidx.car.app.category.NAVIGATION` (required to obtain the Surface).
- Aspect-fit always (black bars), never stretch.
- Never show a plain black car screen: draw a status message when not mirroring.
- UI strings in Italian (the user's language).
- Android 14+ MediaProjection rules apply: FGS type `mediaProjection` started **before** `getMediaProjection()`; `registerCallback` **before** `createVirtualDisplay`; **never** call `createVirtualDisplay` twice on one projection — reuse via `resize()` + `setSurface()`.
- All Gradle commands run from `C:\AI Code\AndroidAutoApp` in PowerShell as `.\gradlew.bat <task>`. If `gradlew.bat` fails with a JVM error, set `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` first.
- Commit after every task; message format `feat: ...` / `chore: ...` / `docs: ...`.

---

### Task 1: Project scaffold

**Files:**
- Create: `.gitignore`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/drawable/ic_app.xml`

**Interfaces:**
- Consumes: nothing (first task).
- Produces: a compiling empty Android app, package `com.viami.aamirror`, resource `R.drawable.ic_app`, all strings used by later tasks (exact names in strings.xml below).

- [ ] **Step 1: Create `.gitignore`**

```gitignore
.gradle/
build/
local.properties
.idea/
*.iml
.kotlin/
captures/
```

- [ ] **Step 2: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "AndroidAutoMirror"
include(":app")
```

- [ ] **Step 3: Create `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
coreKtx = "1.15.0"
carApp = "1.4.0"
activity = "1.9.3"
lifecycle = "2.8.7"
coroutines = "1.9.0"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-car-app = { group = "androidx.car.app", name = "app", version.ref = "carApp" }
androidx-activity-ktx = { group = "androidx.activity", name = "activity-ktx", version.ref = "activity" }
androidx-lifecycle-service = { group = "androidx.lifecycle", name = "lifecycle-service", version.ref = "lifecycle" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
junit = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

- [ ] **Step 4: Create root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
```

- [ ] **Step 5: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
```

- [ ] **Step 6: Create `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.viami.aamirror"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.viami.aamirror"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.car.app)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
```

- [ ] **Step 7: Create `app/src/main/AndroidManifest.xml`** (skeleton — later tasks add services/activity)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="false"
        android:icon="@drawable/ic_app"
        android:label="@string/app_name"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
    </application>

</manifest>
```

- [ ] **Step 8: Create `app/src/main/res/values/strings.xml`** (complete set — later tasks reference these names verbatim)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">AA Mirror</string>

    <!-- Notifiche -->
    <string name="notification_channel">Mirroring attivo</string>
    <string name="notification_mirroring">Mirroring dello schermo in corso</string>
    <string name="notification_stop">Interrompi</string>
    <string name="notification_channel_start">Richiesta di avvio</string>
    <string name="start_request_title">AA Mirror</string>
    <string name="start_request_text">Tocca per autorizzare la condivisione dello schermo</string>

    <!-- Display auto -->
    <string name="car_status_idle">Apri AA Mirror sul telefono e autorizza la cattura</string>
    <string name="car_status_confirm">Conferma sul telefono</string>
    <string name="car_status_error">Permesso negato: apri l\'app sul telefono e riprova</string>
    <string name="car_confirm_on_phone">Conferma la cattura schermo sul telefono</string>
    <string name="car_accessibility_missing">Attiva il servizio Accessibility nelle impostazioni del telefono</string>

    <!-- Activity di setup -->
    <string name="status_idle">Non attivo. Autorizza la cattura schermo, poi apri AA Mirror sull\'auto.</string>
    <string name="status_waiting">In attesa della conferma…</string>
    <string name="status_ready">Pronto: apri AA Mirror sul display dell\'auto</string>
    <string name="status_mirroring">Mirroring attivo sul display dell\'auto</string>
    <string name="accessibility_on">Servizio Accessibility: attivo ✓</string>
    <string name="accessibility_off">Servizio Accessibility: non attivo</string>
    <string name="btn_accessibility">Apri impostazioni Accessibility</string>
    <string name="btn_capture">Autorizza cattura schermo</string>
    <string name="accessibility_description">Consente ad AA Mirror di inviare Indietro e Home dal display dell\'auto</string>
    <string name="setup_instructions">Setup Android Auto (una tantum):\n1. App Android Auto → Impostazioni → tocca 10 volte "Versione" per attivare la modalità sviluppatore\n2. Menu ⋮ → Impostazioni sviluppatore → attiva "Fonti sconosciute"\n3. Impostazioni → Personalizza launcher → attiva AA Mirror</string>
</resources>
```

- [ ] **Step 9: Create `app/src/main/res/drawable/ic_app.xml`** (screen/monitor glyph — also used as notification icon)

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="#FFFFFF">
    <path android:fillColor="#FF000000"
        android:pathData="M21,3H3C1.9,3 1,3.9 1,5v12c0,1.1 0.9,2 2,2h5v2h8v-2h5c1.1,0 2,-0.9 2,-2V5C23,3.9 22.1,3 21,3zM21,17H3V5h18V17z"/>
</vector>
```

- [ ] **Step 10: Generate the Gradle wrapper**

Run in PowerShell from `C:\AI Code\AndroidAutoApp`:

```powershell
gradle wrapper --gradle-version 8.11.1
```

If `gradle` is not on PATH: open the project once in Android Studio (File → Open → `C:\AI Code\AndroidAutoApp`) and let the first sync generate/download the wrapper; alternatively copy `gradlew.bat`, `gradlew`, and `gradle/wrapper/*` from any existing Android Studio project and set `distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip` in `gradle/wrapper/gradle-wrapper.properties`.

- [ ] **Step 11: Verify the build**

```powershell
.\gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL`. (First run downloads Gradle + dependencies; may take minutes.)

- [ ] **Step 12: Commit**

```powershell
git add -A
git commit -m "chore: scaffold Android project (Kotlin, car-app library, single module)"
```

---

### Task 2: Mirror state machine (pure, TDD)

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/core/MirrorState.kt`
- Test: `app/src/test/java/com/viami/aamirror/core/MirrorStateTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `enum class ProjectionStatus { NONE, REQUESTED, ACTIVE }`
  - `data class MirrorState(val projection: ProjectionStatus = NONE, val surfaceAttached: Boolean = false, val lastError: String? = null)` with `val isMirroring: Boolean`
  - `sealed interface MirrorEvent` with data objects: `PermissionRequested`, `ProjectionAcquired`, `ProjectionStopped`, `PermissionDenied`, `SurfaceAvailable`, `SurfaceDestroyed`
  - `fun MirrorState.reduce(event: MirrorEvent): MirrorState`

- [ ] **Step 1: Write the failing tests** — `app/src/test/java/com/viami/aamirror/core/MirrorStateTest.kt`

```kotlin
package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MirrorStateTest {

    @Test
    fun `initial state is not mirroring`() {
        val state = MirrorState()
        assertFalse(state.isMirroring)
        assertEquals(ProjectionStatus.NONE, state.projection)
        assertFalse(state.surfaceAttached)
        assertNull(state.lastError)
    }

    @Test
    fun `projection plus surface means mirroring`() {
        val state = MirrorState()
            .reduce(MirrorEvent.PermissionRequested)
            .reduce(MirrorEvent.ProjectionAcquired)
            .reduce(MirrorEvent.SurfaceAvailable)
        assertTrue(state.isMirroring)
    }

    @Test
    fun `surface alone does not mirror`() {
        val state = MirrorState().reduce(MirrorEvent.SurfaceAvailable)
        assertFalse(state.isMirroring)
        assertTrue(state.surfaceAttached)
    }

    @Test
    fun `losing surface stops mirroring but keeps projection`() {
        val state = MirrorState()
            .reduce(MirrorEvent.ProjectionAcquired)
            .reduce(MirrorEvent.SurfaceAvailable)
            .reduce(MirrorEvent.SurfaceDestroyed)
        assertFalse(state.isMirroring)
        assertEquals(ProjectionStatus.ACTIVE, state.projection)
    }

    @Test
    fun `projection stop keeps surface attached`() {
        val state = MirrorState()
            .reduce(MirrorEvent.ProjectionAcquired)
            .reduce(MirrorEvent.SurfaceAvailable)
            .reduce(MirrorEvent.ProjectionStopped)
        assertFalse(state.isMirroring)
        assertTrue(state.surfaceAttached)
        assertEquals(ProjectionStatus.NONE, state.projection)
    }

    @Test
    fun `denied permission records error and new request clears it`() {
        val denied = MirrorState()
            .reduce(MirrorEvent.PermissionRequested)
            .reduce(MirrorEvent.PermissionDenied)
        assertEquals(ProjectionStatus.NONE, denied.projection)
        assertTrue(denied.lastError != null)

        val retried = denied.reduce(MirrorEvent.PermissionRequested)
        assertNull(retried.lastError)
        assertEquals(ProjectionStatus.REQUESTED, retried.projection)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.viami.aamirror.core.MirrorStateTest"
```

Expected: FAIL (compilation error: `MirrorState` unresolved).

- [ ] **Step 3: Write the implementation** — `app/src/main/java/com/viami/aamirror/core/MirrorState.kt`

```kotlin
package com.viami.aamirror.core

enum class ProjectionStatus { NONE, REQUESTED, ACTIVE }

data class MirrorState(
    val projection: ProjectionStatus = ProjectionStatus.NONE,
    val surfaceAttached: Boolean = false,
    val lastError: String? = null,
) {
    val isMirroring: Boolean
        get() = projection == ProjectionStatus.ACTIVE && surfaceAttached
}

sealed interface MirrorEvent {
    data object PermissionRequested : MirrorEvent
    data object ProjectionAcquired : MirrorEvent
    data object ProjectionStopped : MirrorEvent
    data object PermissionDenied : MirrorEvent
    data object SurfaceAvailable : MirrorEvent
    data object SurfaceDestroyed : MirrorEvent
}

fun MirrorState.reduce(event: MirrorEvent): MirrorState = when (event) {
    MirrorEvent.PermissionRequested ->
        copy(projection = ProjectionStatus.REQUESTED, lastError = null)
    MirrorEvent.ProjectionAcquired ->
        copy(projection = ProjectionStatus.ACTIVE, lastError = null)
    MirrorEvent.ProjectionStopped ->
        copy(projection = ProjectionStatus.NONE)
    MirrorEvent.PermissionDenied ->
        copy(projection = ProjectionStatus.NONE, lastError = "Permesso di cattura negato")
    MirrorEvent.SurfaceAvailable ->
        copy(surfaceAttached = true)
    MirrorEvent.SurfaceDestroyed ->
        copy(surfaceAttached = false)
}
```

- [ ] **Step 4: Run tests to verify they pass**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.viami.aamirror.core.MirrorStateTest"
```

Expected: `BUILD SUCCESSFUL`, 6 tests passed.

- [ ] **Step 5: Commit**

```powershell
git add app/src
git commit -m "feat: mirror state machine with pure reducer"
```

---

### Task 3: AspectFit (pure, TDD)

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/core/AspectFit.kt`
- Test: `app/src/test/java/com/viami/aamirror/core/AspectFitTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `data class FitRect(val x: Int, val y: Int, val width: Int, val height: Int)` (deliberately not `android.graphics.Rect` so tests stay JVM-pure)
  - `object AspectFit { fun fit(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): FitRect }` — throws `IllegalArgumentException` on non-positive input.
- Note: the system letterboxes MediaProjection output on its own; `AspectFit` documents/logs where the content lands on the car screen and is the mapping function phase 2 (touch/remote source) will need.

- [ ] **Step 1: Write the failing tests** — `app/src/test/java/com/viami/aamirror/core/AspectFitTest.kt`

```kotlin
package com.viami.aamirror.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AspectFitTest {

    @Test
    fun `portrait phone on landscape car screen is pillarboxed`() {
        // Phone 1080x2400 into car 800x480: scale = min(800/1080, 480/2400) = 0.2
        val rect = AspectFit.fit(1080, 2400, 800, 480)
        assertEquals(FitRect(x = 292, y = 0, width = 216, height = 480), rect)
    }

    @Test
    fun `landscape phone on landscape car screen is letterboxed`() {
        // Phone 2400x1080 into car 800x480: scale = min(800/2400, 480/1080) = 1/3
        val rect = AspectFit.fit(2400, 1080, 800, 480)
        assertEquals(FitRect(x = 0, y = 60, width = 800, height = 360), rect)
    }

    @Test
    fun `same aspect ratio fills the destination`() {
        val rect = AspectFit.fit(1600, 960, 800, 480)
        assertEquals(FitRect(x = 0, y = 0, width = 800, height = 480), rect)
    }

    @Test
    fun `non positive dimensions are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { AspectFit.fit(0, 100, 800, 480) }
        assertThrows(IllegalArgumentException::class.java) { AspectFit.fit(100, 100, 800, -1) }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.viami.aamirror.core.AspectFitTest"
```

Expected: FAIL (compilation error: `AspectFit` unresolved).

- [ ] **Step 3: Write the implementation** — `app/src/main/java/com/viami/aamirror/core/AspectFit.kt`

```kotlin
package com.viami.aamirror.core

data class FitRect(val x: Int, val y: Int, val width: Int, val height: Int)

object AspectFit {
    fun fit(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): FitRect {
        require(srcWidth > 0 && srcHeight > 0 && dstWidth > 0 && dstHeight > 0) {
            "dimensions must be positive: src=${srcWidth}x$srcHeight dst=${dstWidth}x$dstHeight"
        }
        val scale = minOf(
            dstWidth.toFloat() / srcWidth,
            dstHeight.toFloat() / srcHeight,
        )
        val width = (srcWidth * scale).toInt()
        val height = (srcHeight * scale).toInt()
        return FitRect(
            x = (dstWidth - width) / 2,
            y = (dstHeight - height) / 2,
            width = width,
            height = height,
        )
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.viami.aamirror.core.AspectFitTest"
```

Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 5: Commit**

```powershell
git add app/src
git commit -m "feat: aspect-fit calculation for letterboxed mirroring"
```

---

### Task 4: Frame plumbing — SurfaceTarget, FrameSource, LocalScreenSource, MirrorGateway

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/mirror/FrameSource.kt`
- Create: `app/src/main/java/com/viami/aamirror/mirror/LocalScreenSource.kt`
- Create: `app/src/main/java/com/viami/aamirror/core/MirrorGateway.kt`

**Interfaces:**
- Consumes: `MirrorState`/`MirrorEvent`/`reduce` (Task 2), `AspectFit` (Task 3).
- Produces:
  - `data class SurfaceTarget(val surface: android.view.Surface, val width: Int, val height: Int, val densityDpi: Int)`
  - `interface FrameSource { fun attach(target: SurfaceTarget); fun detach(); fun release() }`
  - `class LocalScreenSource(context: Context, projection: MediaProjection) : FrameSource`
  - `object MirrorGateway` with: `val state: StateFlow<MirrorState>`, `val surfaceTarget: StateFlow<SurfaceTarget?>`, `fun attachSurface(target: SurfaceTarget)`, `fun detachSurface()`, `fun dispatch(event: MirrorEvent)`
- No unit tests here (thin Android-framework wrappers); verified by compilation now and end-to-end in Task 9.

- [ ] **Step 1: Create `app/src/main/java/com/viami/aamirror/mirror/FrameSource.kt`**

```kotlin
package com.viami.aamirror.mirror

import android.view.Surface

data class SurfaceTarget(
    val surface: Surface,
    val width: Int,
    val height: Int,
    val densityDpi: Int,
)

/**
 * Produces frames onto a car-display surface. Today the only implementation
 * captures the local phone screen; phase 2 adds a WiFi stream from a second
 * phone as another implementation, without touching the car side.
 */
interface FrameSource {
    /** Start (or move) output onto [target]. Safe to call again with a new target. */
    fun attach(target: SurfaceTarget)

    /** Stop producing frames but keep the source alive for a later [attach]. */
    fun detach()

    /** Tear down permanently. The instance must not be reused afterwards. */
    fun release()
}
```

- [ ] **Step 2: Create `app/src/main/java/com/viami/aamirror/mirror/LocalScreenSource.kt`**

Android 14 forbids calling `createVirtualDisplay` twice on one `MediaProjection`, so the `VirtualDisplay` is created once and then re-targeted with `resize()` + `setSurface()`. Phone rotation is re-letterboxed automatically by the system compositor — no rotation code needed.

```kotlin
package com.viami.aamirror.mirror

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import com.viami.aamirror.core.AspectFit

class LocalScreenSource(
    context: Context,
    private val projection: MediaProjection,
) : FrameSource {

    private val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val handler = Handler(Looper.getMainLooper())
    private var virtualDisplay: VirtualDisplay? = null

    override fun attach(target: SurfaceTarget) {
        val mode = displayManager.getDisplay(Display.DEFAULT_DISPLAY).mode
        val content = AspectFit.fit(
            mode.physicalWidth, mode.physicalHeight, target.width, target.height,
        )
        Log.i(TAG, "phone ${mode.physicalWidth}x${mode.physicalHeight} -> " +
            "car ${target.width}x${target.height}, content=$content")

        val existing = virtualDisplay
        if (existing == null) {
            virtualDisplay = projection.createVirtualDisplay(
                "aa-mirror",
                target.width, target.height, target.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                target.surface, null, handler,
            )
        } else {
            existing.resize(target.width, target.height, target.densityDpi)
            existing.setSurface(target.surface)
        }
    }

    override fun detach() {
        virtualDisplay?.surface = null
    }

    override fun release() {
        virtualDisplay?.release()
        virtualDisplay = null
    }

    private companion object {
        const val TAG = "LocalScreenSource"
    }
}
```

- [ ] **Step 3: Create `app/src/main/java/com/viami/aamirror/core/MirrorGateway.kt`**

```kotlin
package com.viami.aamirror.core

import com.viami.aamirror.mirror.SurfaceTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-process bridge between the car side (Surface + buttons), the phone-side
 * MirrorService (projection + frames), and the setup activity (status UI).
 */
object MirrorGateway {
    private val _state = MutableStateFlow(MirrorState())
    val state: StateFlow<MirrorState> = _state.asStateFlow()

    private val _surfaceTarget = MutableStateFlow<SurfaceTarget?>(null)
    val surfaceTarget: StateFlow<SurfaceTarget?> = _surfaceTarget.asStateFlow()

    fun attachSurface(target: SurfaceTarget) {
        _surfaceTarget.value = target
        dispatch(MirrorEvent.SurfaceAvailable)
    }

    fun detachSurface() {
        _surfaceTarget.value = null
        dispatch(MirrorEvent.SurfaceDestroyed)
    }

    fun dispatch(event: MirrorEvent) {
        _state.update { it.reduce(event) }
    }
}
```

- [ ] **Step 4: Verify compilation and existing tests**

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, 10 tests passed.

- [ ] **Step 5: Commit**

```powershell
git add app/src
git commit -m "feat: frame source abstraction, local screen capture, in-process gateway"
```

---

### Task 5: MirrorService (foreground service + MediaProjection)

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/mirror/MirrorService.kt`
- Modify: `app/src/main/AndroidManifest.xml` (permissions + service declaration)

**Interfaces:**
- Consumes: `MirrorGateway`, `MirrorEvent`, `LocalScreenSource`, `SurfaceTarget` (Tasks 2, 4); strings `notification_channel`, `notification_mirroring`, `notification_stop`; drawable `ic_app` (Task 1).
- Produces: `class MirrorService : LifecycleService` with `companion object { fun start(context: Context, resultCode: Int, data: Intent); const val ACTION_START; const val ACTION_STOP; const val EXTRA_RESULT_CODE; const val EXTRA_RESULT_DATA }`. Task 7's SetupActivity calls `MirrorService.start(...)` after the capture consent result.

- [ ] **Step 1: Create `app/src/main/java/com/viami/aamirror/mirror/MirrorService.kt`**

Android-14 rules honored here: `startForeground` (type `mediaProjection`) happens **before** `getMediaProjection()`; the callback is registered **before** the first `createVirtualDisplay`.

```kotlin
package com.viami.aamirror.mirror

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.viami.aamirror.R
import com.viami.aamirror.core.MirrorEvent
import com.viami.aamirror.core.MirrorGateway
import kotlinx.coroutines.launch

class MirrorService : LifecycleService() {

    private var projection: MediaProjection? = null
    private var source: LocalScreenSource? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            MirrorGateway.dispatch(MirrorEvent.ProjectionStopped)
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch {
            MirrorGateway.surfaceTarget.collect { target ->
                val src = source ?: return@collect
                if (target != null) src.attach(target) else src.detach()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                startAsForeground()
                acquireProjection(intent)
            }
            ACTION_STOP -> projection?.stop() ?: stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, MirrorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app)
            .setContentTitle(getString(R.string.notification_mirroring))
            .setOngoing(true)
            .addAction(0, getString(R.string.notification_stop), stopIntent)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun acquireProjection(intent: Intent) {
        if (projection != null) return
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (data == null) {
            stopSelf()
            return
        }
        val manager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val acquired = manager.getMediaProjection(resultCode, data) ?: run {
            MirrorGateway.dispatch(MirrorEvent.PermissionDenied)
            stopSelf()
            return
        }
        acquired.registerCallback(projectionCallback, null)
        projection = acquired
        source = LocalScreenSource(this, acquired)
        MirrorGateway.dispatch(MirrorEvent.ProjectionAcquired)
        // The collector in onCreate saw source == null for the current value;
        // deliver the already-attached surface (if any) by hand.
        MirrorGateway.surfaceTarget.value?.let { source?.attach(it) }
    }

    override fun onDestroy() {
        source?.release()
        source = null
        projection?.unregisterCallback(projectionCallback)
        projection = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.viami.aamirror.START"
        const val ACTION_STOP = "com.viami.aamirror.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "mirror"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, MirrorService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, data)
            context.startForegroundService(intent)
        }
    }
}
```

- [ ] **Step 2: Add permissions and service to the manifest**

In `app/src/main/AndroidManifest.xml`, insert **before** `<application>`:

```xml
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Insert **inside** `<application>`:

```xml
        <service
            android:name=".mirror.MirrorService"
            android:exported="false"
            android:foregroundServiceType="mediaProjection" />
```

- [ ] **Step 3: Verify compilation and tests**

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, 10 tests passed.

- [ ] **Step 4: Commit**

```powershell
git add app/src
git commit -m "feat: foreground mirror service owning the MediaProjection session"
```

---

### Task 6: Phone input — accessibility service + media key

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/input/MirrorAccessibilityService.kt`
- Create: `app/src/main/java/com/viami/aamirror/input/PhoneKeys.kt`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Modify: `app/src/main/AndroidManifest.xml` (accessibility service declaration)

**Interfaces:**
- Consumes: string `accessibility_description` (Task 1).
- Produces:
  - `MirrorAccessibilityService.pressBack(): Boolean` and `MirrorAccessibilityService.pressHome(): Boolean` (companion functions; return `false` when the service is not enabled)
  - `MirrorAccessibilityService.instance: MirrorAccessibilityService?` (null when not enabled — SetupActivity uses it as the "is enabled" check)
  - `PhoneKeys.playPause(context: Context)`

- [ ] **Step 1: Create `app/src/main/java/com/viami/aamirror/input/MirrorAccessibilityService.kt`**

```kotlin
package com.viami.aamirror.input

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class MirrorAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        var instance: MirrorAccessibilityService? = null
            private set

        fun pressBack(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false

        fun pressHome(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
    }
}
```

- [ ] **Step 2: Create `app/src/main/java/com/viami/aamirror/input/PhoneKeys.kt`**

```kotlin
package com.viami.aamirror.input

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

object PhoneKeys {
    /** Toggles play/pause on the active media session. No accessibility needed. */
    fun playPause(context: Context) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.dispatchMediaKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        )
        audio.dispatchMediaKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        )
    }
}
```

- [ ] **Step 3: Create `app/src/main/res/xml/accessibility_service_config.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:description="@string/accessibility_description" />
```

- [ ] **Step 4: Declare the service in the manifest** — insert inside `<application>`:

```xml
        <service
            android:name=".input.MirrorAccessibilityService"
            android:exported="false"
            android:label="@string/app_name"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
```

- [ ] **Step 5: Verify compilation**

```powershell
.\gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```powershell
git add app/src
git commit -m "feat: back/home via accessibility, play-pause via media key"
```

---

### Task 7: Setup activity + capture-permission request path

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/setup/StartRequests.kt`
- Create: `app/src/main/java/com/viami/aamirror/setup/SetupActivity.kt`
- Create: `app/src/main/res/layout/activity_setup.xml`
- Modify: `app/src/main/AndroidManifest.xml` (activity declaration)

**Interfaces:**
- Consumes: `MirrorGateway`, `MirrorState`, `MirrorEvent`, `ProjectionStatus` (Tasks 2, 4); `MirrorService.start(...)` (Task 5); `MirrorAccessibilityService.instance` (Task 6); strings from Task 1.
- Produces:
  - `class SetupActivity : ComponentActivity` with `companion object { const val EXTRA_REQUEST_CAPTURE = "request_capture" }` — when launched with that boolean extra `true`, it immediately fires the system screen-capture consent dialog.
  - `object StartRequests { fun requestCapturePermission(context: Context) }` — dispatches `PermissionRequested`, tries a direct activity start (may be blocked in background), and always posts a high-priority notification that opens SetupActivity with `EXTRA_REQUEST_CAPTURE`. Task 8's car screen calls this.

- [ ] **Step 1: Create `app/src/main/res/layout/activity_setup.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:id="@+id/txt_status"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="18sp"
        android:text="@string/status_idle" />

    <Button
        android:id="@+id/btn_capture"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:text="@string/btn_capture" />

    <TextView
        android:id="@+id/txt_accessibility"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:text="@string/accessibility_off" />

    <Button
        android:id="@+id/btn_accessibility"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/btn_accessibility" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="32dp"
        android:textSize="14sp"
        android:text="@string/setup_instructions" />

</LinearLayout>
```

- [ ] **Step 2: Create `app/src/main/java/com/viami/aamirror/setup/StartRequests.kt`**

```kotlin
package com.viami.aamirror.setup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.viami.aamirror.R
import com.viami.aamirror.core.MirrorEvent
import com.viami.aamirror.core.MirrorGateway

object StartRequests {
    private const val CHANNEL_ID = "start_request"
    private const val NOTIFICATION_ID = 2
    private const val TAG = "StartRequests"

    /**
     * Called from the car screen when mirroring is requested but no projection
     * permission is held. The direct activity start works only when Android
     * allows background launches; the notification is the reliable path.
     */
    fun requestCapturePermission(context: Context) {
        MirrorGateway.dispatch(MirrorEvent.PermissionRequested)

        val activityIntent = Intent(context, SetupActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(SetupActivity.EXTRA_REQUEST_CAPTURE, true)
        try {
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            Log.i(TAG, "direct activity start blocked, notification only", e)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_start),
                NotificationManager.IMPORTANCE_HIGH,
            )
        )
        val pending = PendingIntent.getActivity(
            context, 0, activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app)
            .setContentTitle(context.getString(R.string.start_request_title))
            .setContentText(context.getString(R.string.start_request_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
```

- [ ] **Step 3: Create `app/src/main/java/com/viami/aamirror/setup/SetupActivity.kt`**

```kotlin
package com.viami.aamirror.setup

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.viami.aamirror.R
import com.viami.aamirror.core.MirrorEvent
import com.viami.aamirror.core.MirrorGateway
import com.viami.aamirror.core.MirrorState
import com.viami.aamirror.core.ProjectionStatus
import com.viami.aamirror.input.MirrorAccessibilityService
import com.viami.aamirror.mirror.MirrorService
import kotlinx.coroutines.launch

class SetupActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val capture =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                MirrorService.start(this, result.resultCode, data)
            } else {
                MirrorGateway.dispatch(MirrorEvent.PermissionDenied)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        findViewById<Button>(R.id.btn_capture).setOnClickListener { requestCapture() }
        findViewById<Button>(R.id.btn_accessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        lifecycleScope.launch {
            MirrorGateway.state.collect { render(it) }
        }

        if (intent.getBooleanExtra(EXTRA_REQUEST_CAPTURE, false)) requestCapture()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_REQUEST_CAPTURE, false)) requestCapture()
    }

    override fun onResume() {
        super.onResume()
        render(MirrorGateway.state.value)
    }

    private fun requestCapture() {
        if (MirrorGateway.state.value.projection == ProjectionStatus.ACTIVE) return
        val manager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        capture.launch(manager.createScreenCaptureIntent())
    }

    private fun render(state: MirrorState) {
        findViewById<TextView>(R.id.txt_status).text = when {
            state.isMirroring -> getString(R.string.status_mirroring)
            state.projection == ProjectionStatus.ACTIVE -> getString(R.string.status_ready)
            state.projection == ProjectionStatus.REQUESTED -> getString(R.string.status_waiting)
            state.lastError != null -> state.lastError
            else -> getString(R.string.status_idle)
        }
        findViewById<TextView>(R.id.txt_accessibility).text =
            if (MirrorAccessibilityService.instance != null) {
                getString(R.string.accessibility_on)
            } else {
                getString(R.string.accessibility_off)
            }
    }

    companion object {
        const val EXTRA_REQUEST_CAPTURE = "request_capture"
    }
}
```

- [ ] **Step 4: Declare the activity in the manifest** — insert inside `<application>`:

```xml
        <activity
            android:name=".setup.SetupActivity"
            android:exported="true"
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
```

- [ ] **Step 5: Verify compilation and tests**

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, 10 tests passed.

- [ ] **Step 6: Commit**

```powershell
git add app/src
git commit -m "feat: setup activity with capture consent and start-request notification"
```

---

### Task 8: Car side — CarAppService, session, screen, status renderer, icons

**Files:**
- Create: `app/src/main/java/com/viami/aamirror/car/MirrorCarAppService.kt`
- Create: `app/src/main/java/com/viami/aamirror/car/MirrorSession.kt`
- Create: `app/src/main/java/com/viami/aamirror/car/MirrorScreen.kt`
- Create: `app/src/main/java/com/viami/aamirror/car/StatusRenderer.kt`
- Create: `app/src/main/res/drawable/ic_play_pause.xml`
- Create: `app/src/main/res/drawable/ic_back.xml`
- Create: `app/src/main/res/drawable/ic_home.xml`
- Create: `app/src/main/res/xml/automotive_app_desc.xml`
- Modify: `app/src/main/AndroidManifest.xml` (car permissions, meta-data, car app service)

**Interfaces:**
- Consumes: `MirrorGateway`, `ProjectionStatus` (Tasks 2, 4), `SurfaceTarget` (Task 4), `StartRequests.requestCapturePermission` (Task 7), `MirrorAccessibilityService.pressBack/pressHome`, `PhoneKeys.playPause` (Task 6), car strings (Task 1).
- Produces: the Android Auto entry point. Nothing else consumes it.

- [ ] **Step 1: Create the three action icons**

`app/src/main/res/drawable/ic_play_pause.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="#FFFFFF">
    <path android:fillColor="#FF000000"
        android:pathData="M3,5v14l8,-7L3,5z M14,5v14h3V5h-3z M19,5v14h3V5h-3z"/>
</vector>
```

`app/src/main/res/drawable/ic_back.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="#FFFFFF">
    <path android:fillColor="#FF000000"
        android:pathData="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"/>
</vector>
```

`app/src/main/res/drawable/ic_home.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24"
    android:tint="#FFFFFF">
    <path android:fillColor="#FF000000"
        android:pathData="M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z"/>
</vector>
```

- [ ] **Step 2: Create `app/src/main/res/xml/automotive_app_desc.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<automotiveApp>
    <uses name="template" />
</automotiveApp>
```

- [ ] **Step 3: Create `app/src/main/java/com/viami/aamirror/car/StatusRenderer.kt`**

Draws a single status frame so the car screen is never plain black. Locking can race with the VirtualDisplay taking over the surface — that is expected and swallowed.

```kotlin
package com.viami.aamirror.car

import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.car.app.SurfaceContainer

object StatusRenderer {
    fun draw(container: SurfaceContainer, message: String) {
        val surface = container.surface ?: return
        if (!surface.isValid) return
        try {
            val canvas = surface.lockCanvas(null) ?: return
            try {
                canvas.drawColor(Color.BLACK)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = container.height / 14f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    message,
                    container.width / 2f,
                    container.height / 2f,
                    paint,
                )
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "surface busy, skipping status frame", e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "surface busy, skipping status frame", e)
        }
    }

    private const val TAG = "StatusRenderer"
}
```

- [ ] **Step 4: Create `app/src/main/java/com/viami/aamirror/car/MirrorScreen.kt`**

```kotlin
package com.viami.aamirror.car

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.viami.aamirror.R
import com.viami.aamirror.core.MirrorGateway
import com.viami.aamirror.core.ProjectionStatus
import com.viami.aamirror.input.MirrorAccessibilityService
import com.viami.aamirror.input.PhoneKeys
import com.viami.aamirror.mirror.SurfaceTarget
import com.viami.aamirror.setup.StartRequests
import kotlinx.coroutines.launch

class MirrorScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    private var surfaceContainer: SurfaceContainer? = null

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(container: SurfaceContainer) {
            surfaceContainer = container
            val surface = container.surface ?: return
            MirrorGateway.attachSurface(
                SurfaceTarget(surface, container.width, container.height, container.dpi)
            )
            ensureProjection()
            renderStatusIfIdle()
        }

        override fun onSurfaceDestroyed(container: SurfaceContainer) {
            surfaceContainer = null
            MirrorGateway.detachSurface()
        }
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
        lifecycleScope.launch {
            MirrorGateway.state.collect { renderStatusIfIdle() }
        }
    }

    override fun onGetTemplate(): Template {
        val strip = ActionStrip.Builder()
            .addAction(action(R.drawable.ic_play_pause) { PhoneKeys.playPause(carContext) })
            .addAction(action(R.drawable.ic_back) {
                requireAccessibility { MirrorAccessibilityService.pressBack() }
            })
            .addAction(action(R.drawable.ic_home) {
                requireAccessibility { MirrorAccessibilityService.pressHome() }
            })
            .build()
        return NavigationTemplate.Builder().setActionStrip(strip).build()
    }

    private fun ensureProjection() {
        if (MirrorGateway.state.value.projection == ProjectionStatus.NONE) {
            StartRequests.requestCapturePermission(carContext)
            CarToast.makeText(
                carContext,
                carContext.getString(R.string.car_confirm_on_phone),
                CarToast.LENGTH_LONG,
            ).show()
        }
    }

    private fun renderStatusIfIdle() {
        val container = surfaceContainer ?: return
        val state = MirrorGateway.state.value
        if (state.isMirroring) return
        val message = when {
            state.lastError != null ->
                carContext.getString(R.string.car_status_error)
            state.projection == ProjectionStatus.REQUESTED ->
                carContext.getString(R.string.car_status_confirm)
            else ->
                carContext.getString(R.string.car_status_idle)
        }
        StatusRenderer.draw(container, message)
    }

    private fun requireAccessibility(send: () -> Boolean) {
        if (!send()) {
            CarToast.makeText(
                carContext,
                carContext.getString(R.string.car_accessibility_missing),
                CarToast.LENGTH_LONG,
            ).show()
        }
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

- [ ] **Step 5: Create `app/src/main/java/com/viami/aamirror/car/MirrorSession.kt`**

```kotlin
package com.viami.aamirror.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class MirrorSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = MirrorScreen(carContext)
}
```

- [ ] **Step 6: Create `app/src/main/java/com/viami/aamirror/car/MirrorCarAppService.kt`**

`ALLOW_ALL_HOSTS_VALIDATOR` is acceptable for a personal sideloaded app; a Play-Store app would pin the official host signatures.

```kotlin
package com.viami.aamirror.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class MirrorCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = MirrorSession()
}
```

- [ ] **Step 7: Add car declarations to the manifest**

Insert **before** `<application>` (next to the existing `<uses-permission>` lines):

```xml
    <uses-permission android:name="androidx.car.app.NAVIGATION_TEMPLATES" />
    <uses-permission android:name="androidx.car.app.ACCESS_SURFACE" />
```

Insert **inside** `<application>`:

```xml
        <meta-data
            android:name="androidx.car.app.minCarApiLevel"
            android:value="1" />
        <meta-data
            android:name="com.google.android.gms.car.application"
            android:resource="@xml/automotive_app_desc" />

        <service
            android:name=".car.MirrorCarAppService"
            android:exported="true">
            <intent-filter>
                <action android:name="androidx.car.app.CarAppService" />
                <category android:name="androidx.car.app.category.NAVIGATION" />
            </intent-filter>
        </service>
```

- [ ] **Step 8: Verify compilation and tests**

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, 10 tests passed.

- [ ] **Step 9: Commit**

```powershell
git add app/src
git commit -m "feat: Android Auto navigation-category car app rendering the mirror surface"
```

---

### Task 9: README + end-to-end verification (DHU, then car)

**Files:**
- Create: `README.md`

**Interfaces:**
- Consumes: the complete app (Tasks 1–8).
- Produces: documentation and a verified build. The DHU checklist is the integration test for everything not unit-testable.

- [ ] **Step 1: Create `README.md`**

```markdown
# AA Mirror

App personale che mostra lo schermo del telefono sul display Android Auto
(categoria navigazione + MediaProjection). **Solo uso personale:** non
pubblicabile sul Play Store.

## Requisiti

- Telefono Android 8+ (consigliato 10+), app Android Auto installata
- Auto o unità con Android Auto via USB (testata: Nissan Qashqai J12)

## Installazione

1. Compila e installa: `.\gradlew.bat installDebug` (telefono via USB, debug USB attivo)
2. Sul telefono, app **Android Auto** → Impostazioni → tocca 10 volte **Versione**
   per sbloccare la modalità sviluppatore
3. Menu ⋮ → **Impostazioni sviluppatore** → attiva **Fonti sconosciute**
4. Impostazioni → **Personalizza launcher** → attiva **AA Mirror**
5. Apri AA Mirror sul telefono:
   - tocca "Apri impostazioni Accessibility" e attiva **AA Mirror**
     (serve solo per i tasti Indietro/Home dal display auto)
   - concedi il permesso notifiche se richiesto

## Uso

1. Collega il telefono all'auto con il cavo USB-C
2. Sul display auto apri **AA Mirror**
3. Alla richiesta, conferma la cattura schermo sul telefono
   (tocca la notifica "AA Mirror" se il dialog non appare da solo)
4. Barra azioni sul display auto: ⏯ play/pausa · ◀ indietro · ⌂ home

L'audio multimediale arriva alle casse dell'auto tramite Android Auto stesso.

## Test senza auto (Desktop Head Unit)

1. In Android Studio: SDK Manager → SDK Tools → installa **Android Auto Desktop Head Unit Emulator**
2. Sul telefono: Android Auto → Impostazioni sviluppatore → **Avvia server unità principale**
3. Sul PC: `adb forward tcp:5277 tcp:5277`
4. Avvia `%LOCALAPPDATA%\Android\Sdk\extras\google\auto\desktop-head-unit.exe`

## Architettura

- `core/` — stato (reducer puro), gateway in-process, calcolo aspect-fit
- `mirror/` — foreground service + MediaProjection → VirtualDisplay (`FrameSource`)
- `car/` — CarAppService categoria navigazione: Surface, action strip, frame di stato
- `input/` — Indietro/Home via Accessibility, play/pausa via media key
- `setup/` — activity di configurazione e richiesta permessi

Fase 2 prevista: sorgente remota via WiFi da un secondo telefono
(nuova implementazione di `FrameSource`, lato auto invariato).
```

- [ ] **Step 2: Full build and unit tests**

```powershell
.\gradlew.bat clean assembleDebug testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, 10 tests passed.

- [ ] **Step 3: Install on the phone**

Phone connected via USB with USB debugging enabled:

```powershell
.\gradlew.bat installDebug
```

Expected: `Installed on 1 device.`

- [ ] **Step 4: Manual DHU checklist** (requires the DHU setup from the README; needs the human's phone — coordinate with the user)

1. Enable AA developer mode + Unknown sources + launcher customization (README steps 2–4).
2. Start head unit server on phone, `adb forward tcp:5277 tcp:5277`, launch DHU.
3. **AA Mirror appears** in the DHU launcher.
4. Tap it → status frame "Conferma sul telefono" (not a black screen) → consent dialog or notification appears on the phone.
5. Confirm → phone screen appears on DHU, aspect-fit with black bars.
6. Rotate the phone to landscape → mirrored image reflows (brief flicker acceptable).
7. ⏯ with a media app playing → toggles play/pause.
8. ◀ and ⌂ → act on the phone (with Accessibility enabled); with it disabled → CarToast message appears.
9. Pull down phone notification shade → "Mirroring dello schermo in corso" with a working **Interrompi** action; after stopping, DHU shows the idle status frame.
10. Exit and re-enter the app on DHU → mirroring resumes without a new consent (same service session).

- [ ] **Step 5: Commit**

```powershell
git add README.md
git commit -m "docs: install, usage and DHU testing guide"
```

- [ ] **Step 6: Final car test (Nissan Qashqai J12)** — same checklist as Step 4 but on the real head unit, plus: unplug the USB cable mid-mirroring → notification disappears (projection released); replug → app reappears in the AA launcher and mirroring restarts after re-consent if the service was killed.

---

## Addendum 2026-07-14 — Touch + blocco landscape (Task 10–13)

Approvato dopo il collaudo DHU della v0.1 (vedi addendum nella spec). Eseguiti
inline nella stessa sessione, stesso branch `feature/screen-mirror`.

### Task 10: TouchMapper (pure, TDD)

- Create: `core/TouchMapper.kt` — `data class PhonePoint(val x: Float, val y: Float)`;
  `TouchMapper.mapTap(carX: Float, carY: Float, content: FitRect, phoneWidth: Int, phoneHeight: Int): PhonePoint?`
  (null sulle bande nere; mapping lineare dentro il content rect).
- Test: `core/TouchMapperTest.kt` — centro→centro, angolo→angolo, banda→null.
- Verify: `.\gradlew.bat testDebugUnitTest` → verde. Commit.

### Task 11: Gesti Accessibility

- Modify: `input/MirrorAccessibilityService.kt` — aggiunge
  `tap(x: Float, y: Float): Boolean` e
  `swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long): Boolean`
  via `dispatchGesture`.
- Modify: `res/xml/accessibility_service_config.xml` — `android:canPerformGestures="true"`.
- Verify: build. Commit.

### Task 12: Cablaggio touch nel MirrorScreen

- Create: `mirror/PhoneDisplay.kt` — `currentSize(context): Pair<Int, Int>`
  (dimensioni logiche correnti: physicalWidth/Height del Display.Mode, scambiate
  se rotation è 90/270).
- Modify: `car/MirrorScreen.kt` — `onClick` → TouchMapper → tap;
  `onScroll` → swipe ancorato al centro (end = start − distanza, scalata
  content→telefono, clampata ai bordi). Ignora tutto se non in mirroring.
- Verify: build + test. Commit.

### Task 13: RotationLock + toggle + setup

- Create: `input/RotationLock.kt` — overlay 0×0 `TYPE_APPLICATION_OVERLAY` con
  `screenOrientation = SENSOR_LANDSCAPE`; `isLocked`, `canLock(context)`,
  `lock(context)`, `unlock()`, `toggle(context): Boolean`.
- Create: `res/drawable/ic_rotate.xml` (screen_rotation material).
- Modify: manifest (`SYSTEM_ALERT_WINDOW`), strings (toast + setup overlay),
  `car/MirrorScreen.kt` (4ª azione toggle con CarToast),
  `setup/SetupActivity.kt` + `res/layout/activity_setup.xml` (stato + bottone
  "Mostra sopra altre app" → `ACTION_MANAGE_OVERLAY_PERMISSION`).
- Verify: build + test. Commit.

### Task 14: Reinstallazione e ricollaudo DHU

- `.\gradlew.bat installDebug` sul telefono collegato; riattivare il servizio
  Accessibility se Android lo ha disattivato dopo l'aggiornamento.
- Checklist: tap su icone/app nel DHU col mouse, scroll di una lista, tap sulle
  bande nere (nessun effetto), toggle 🔄 con e senza permesso overlay,
  pulsanti esistenti ancora funzionanti.

## Self-Review Notes

- **Spec coverage:** navigation category + Surface (Task 8), MediaProjection/VirtualDisplay aspect-fit (Tasks 4–5), three buttons (Tasks 6, 8), setup activity + guided steps (Task 7), never-black status frames (Task 8), foreground service + notification (Task 5), permission-denied/reconnect handling (Tasks 5, 7, 8), unit tests for pure parts (Tasks 2–3), DHU + car checklist (Task 9), phase-2 seam documented (`FrameSource`, Task 4). Rotation is handled by the system compositor instead of recreating the VirtualDisplay — deviation from the spec's letter, forced by Android 14's single-`createVirtualDisplay` rule; the spec's observable behavior (brief flicker, correct reflow) is unchanged.
- **Placeholders:** none; every code step contains the full file.
- **Type consistency:** `SurfaceTarget(surface, width, height, densityDpi)`, `FrameSource.attach/detach/release`, `MirrorGateway.attachSurface/detachSurface/dispatch`, `MirrorService.start(context, resultCode, data)`, `StartRequests.requestCapturePermission(context)`, `MirrorAccessibilityService.pressBack()/pressHome()`, `PhoneKeys.playPause(context)` — used identically across Tasks 4–8.
