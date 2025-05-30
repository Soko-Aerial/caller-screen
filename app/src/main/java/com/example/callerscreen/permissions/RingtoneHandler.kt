package com.example.callerscreen

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun RingtoneHandler() {
    val context = LocalContext.current
    var ringtone: Ringtone? by remember { mutableStateOf(null) }
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val canVibrate = vibrator?.hasVibrator() == true

    // Listen for Volume Changes
    val volumeReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                        ringtone?.stop()
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val ringtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(context, ringtoneUri)

        // Register Volume Change Receiver
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(volumeReceiver, filter)

        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                ringtone?.play()
                if (canVibrate) vibratePhone(vibrator)
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                if (canVibrate) vibratePhone(vibrator)
            }
            AudioManager.RINGER_MODE_SILENT -> {
                // Do nothing
            }
        }

        onDispose {
            ringtone?.stop()
            vibrator?.cancel()
            context.unregisterReceiver(volumeReceiver)
        }
    }
}

// ✅ No need to check permissions, just vibrate if possible
@SuppressLint("ObsoleteSdkInt")
private fun vibratePhone(vibrator: Vibrator?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0)
        vibrator?.vibrate(vibrationEffect)
    } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
    }
}
