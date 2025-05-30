package com.example.callerscreen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        val roomId = intent?.getStringExtra("room_id") ?: return
        val callerName = intent.getStringExtra("caller_name") ?: "Unknown"
        val notificationId = 1001


        when (action) {
            "com.example.ACTION_ANSWER_CALL" -> {
                Log.d("NotificationReceiver", "Answer clicked")

                // Open app and navigate to Answer screen
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("navigateTo", "call")
                    putExtra("room_id", roomId)
                    putExtra("caller_name", callerName)
                    SignalingManager.sendCallAccept(roomId)
                    //navController.navigate("answeredScreen")
                }
                ContextCompat.startActivity(context, launchIntent, null)
                // Dismiss the notification
                NotificationManagerCompat.from(context).cancel(notificationId)
            }

            "com.example.ACTION_DECLINE_CALL" -> {
                Log.d("NotificationReceiver", "Decline clicked")

                // Dismiss the notification
                NotificationManagerCompat.from(context).cancel(notificationId)

                // Optional: send 'declined' to Firestore
                CallManager.declineCall(roomId)
            }
        }
    }
}


object CallManager {
    fun declineCall(roomId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val callRef = firestore.collection("calls").document(roomId)
        callRef.update("status", "declined")
            .addOnSuccessListener { Log.d("CallManager", "Call declined in Firestore") }
            .addOnFailureListener { Log.e("CallManager", "Failed to decline call", it) }
    }
}
