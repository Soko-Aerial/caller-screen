package com.example.callerscreen.callUi

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.rememberNavController
import com.example.callerscreen.WebRTCManager
import org.webrtc.SurfaceViewRenderer

class CallActivity : AppCompatActivity() {

    private lateinit var localRenderer: SurfaceViewRenderer
    private lateinit var remoteRenderer: SurfaceViewRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set content view
        setContent {
            val roomId = "roomIdFromServer" // Retrieve roomId dynamically
            val callerName = ""
            AnswerScreen(navController = rememberNavController(), callerName = callerName,roomId = roomId)
        }
    }

    override fun onStart() {
        super.onStart()
        // Initialize and set up WebRTC
        WebRTCManager.init(applicationContext)
        WebRTCManager.setSurfaceViews(localRenderer, remoteRenderer)
        WebRTCManager.startLocalVideo()
    }

    override fun onPause() {
        super.onPause()
        // Stop video capture and close the peer connection when the activity is paused
        WebRTCManager.endCall()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to release resources like SurfaceViewRenderer
        localRenderer.release()
        remoteRenderer.release()
    }
}
