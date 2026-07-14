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
