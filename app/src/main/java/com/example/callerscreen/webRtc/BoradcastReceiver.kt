package com.example.callerscreen.webRtc

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.callerscreen.callUi.CallActivity

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "ACTION_ANSWER" -> {
                val answerIntent = Intent(context, CallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val answerPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    answerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

            }
            "ACTION_DECLINE" -> {
                Toast.makeText(context, "Decline clicked", Toast.LENGTH_SHORT).show()
                // TODO: Add logic to decline the call (e.g., send signal to server)
            }
        }
    }
}
