package com.example.pushnotification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.callerscreen.R
import com.example.callerscreen.callUi.CallActivity
import com.example.callerscreen.webRtc.CallActionReceiver
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {


    fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName
        return appProcesses.any {
            it.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    it.processName == packageName
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val context : Context = this

        val roomId = remoteMessage.data["room_id"]
        val callerName = remoteMessage.data["caller_id"] ?: "Unknown"

        if (!isAppInForeground(context)) {
            showIncomingCallNotification(context)
        } else {
            if (roomId != null) {
                Handler(Looper.getMainLooper()).post {
                    CallEventBus.triggerNavigateToCall(roomId, callerName)
                    Log.d("Call","Call has been triggered")
                }
            } else {
                Log.e("FCM", "roomId missing from payload!")
            }
        }
    }




    @SuppressLint("LaunchActivityFromNotification")
    private fun showIncomingCallNotification(context: Context) {
        val channelId = "call_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the channel
        val channel = NotificationChannel(
            channelId,
            "Incoming Call",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming call notifications"
            setSound(null, null)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        // Pending intents
        val answerIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = "ACTION_ANSWER"
        }
        val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = "ACTION_DECLINE"
        }

        val answerPendingIntent = PendingIntent.getBroadcast(
            context, 0, answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declinePendingIntent = PendingIntent.getBroadcast(
            context, 1, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming Call")
            .setContentText("John Doe is calling...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.drawable.call1, "Answer", answerPendingIntent)
            .addAction(R.drawable.call2, "Decline", declinePendingIntent)
            .setFullScreenIntent(answerPendingIntent, true)
            .build()

        notificationManager.notify(1001, notification)
    }
}

