package com.example.callerscreen

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }


    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName
        return appProcesses.any {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    it.processName == packageName
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val context: Context = this

        val roomId = remoteMessage.data["room_id"]
        val callerName = remoteMessage.data["caller_id"] ?: "Unknown"
        Log.d("roomId", "$roomId")


        if (!isAppInForeground(context)) {
            showIncomingCallNotification(context, roomId, callerName = callerName)
        } else {
            // In foreground, you can trigger a navigation or UI event
            if (roomId != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    CallEventBus.triggerNavigateToCall(roomId, callerName)
                }
            } else {
                Log.e("FCM", "roomId missing from payload!")
            }
        }
    }


    @SuppressLint("LaunchActivityFromNotification")
    private fun showIncomingCallNotification(
        context: Context,
        roomId: String?,
        callerName: String
    ) {
        val channelId = "incoming_call_channel"
        val notificationId = 1001

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (for Android 8+)
        val channel = NotificationChannel(
            channelId,
            "Incoming Call",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for incoming call alerts"
        }
        notificationManager.createNotificationChannel(channel)

        // ACTION: ANSWER
        val answerIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.ACTION_ANSWER_CALL"
            putExtra("room_id", roomId)
            putExtra("caller_name", callerName)
        }
        val answerPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ACTION: DECLINE
        val declineIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.ACTION_DECLINE_CALL"
            putExtra("room_id", roomId)
            putExtra("caller_name", callerName)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming Call")
            .setContentText("$callerName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(true)
            .addAction(R.drawable.call1, "Answer", answerPendingIntent)
            .addAction(R.drawable.call2, "Decline", declinePendingIntent)
            .setFullScreenIntent(answerPendingIntent, true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

}

