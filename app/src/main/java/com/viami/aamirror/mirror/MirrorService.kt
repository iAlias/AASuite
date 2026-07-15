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
import com.viami.aamirror.input.BrightnessSaver
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
        BrightnessSaver.dim(this)
        MirrorGateway.dispatch(MirrorEvent.ProjectionAcquired)
        // The collector in onCreate saw source == null for the current value;
        // deliver the already-attached surface (if any) by hand.
        MirrorGateway.surfaceTarget.value?.let { source?.attach(it) }
    }

    override fun onDestroy() {
        BrightnessSaver.restore(this)
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
