package com.viami.aamirror.setup

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
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
import com.viami.aamirror.input.RotationLock
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
        findViewById<Button>(R.id.btn_overlay).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"),
                )
            )
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
        findViewById<TextView>(R.id.txt_overlay).text =
            if (RotationLock.canLock(this)) {
                getString(R.string.overlay_on)
            } else {
                getString(R.string.overlay_off)
            }
    }

    companion object {
        const val EXTRA_REQUEST_CAPTURE = "request_capture"
    }
}
