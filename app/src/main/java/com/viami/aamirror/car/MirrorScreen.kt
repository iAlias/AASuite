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
