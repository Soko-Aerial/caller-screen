package com.example.callerscreen.callUi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.callerscreen.R
import com.example.callerscreen.RequestPermissions
import com.example.callerscreen.WebRTCManager
import org.webrtc.SurfaceViewRenderer

@Composable
fun AnswerScreen(
    navController: NavHostController,
    roomId: String,
    callerName: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val localRenderer = remember { SurfaceViewRenderer(context) }
    val remoteRenderer = remember { SurfaceViewRenderer(context) }
    val isLocalVideoSmall = remember { mutableStateOf(true) }
    val pipOffset = remember { mutableStateOf(Offset(50f, 100f)) }

    RequestPermissions()

    LaunchedEffect(Unit) {
        WebRTCManager.init(context)
        WebRTCManager.setSurfaceViews(localRenderer, remoteRenderer)
        WebRTCManager.startLocalVideo()
        WebRTCManager.joinCall(roomId)
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFFEFEFEF))) {

        if (isLocalVideoSmall.value) {
            RemoteVideoPreview(modifier = Modifier.fillMaxSize(), remoteRenderer)
        } else {
            LocalVideoPreview(modifier = Modifier.fillMaxSize(), localRenderer)
        }

        // Draggable PiP window
        Box(
            modifier = Modifier
                .offset { IntOffset(pipOffset.value.x.toInt(), pipOffset.value.y.toInt()) }
                .size(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        pipOffset.value = pipOffset.value.copy(
                            x = (pipOffset.value.x + dragAmount.x).coerceIn(0f, 1000f),
                            y = (pipOffset.value.y + dragAmount.y).coerceIn(0f, 2000f)
                        )
                    }
                }
                .clickable { isLocalVideoSmall.value = !isLocalVideoSmall.value },
            contentAlignment = Alignment.Center
        ) {
            if (isLocalVideoSmall.value) {
                LocalVideoPreview(Modifier.fillMaxSize(), localRenderer)
            } else {
                RemoteVideoPreview(Modifier.fillMaxSize(), remoteRenderer)
            }
        }

        // Caller Name
        Text(
            callerName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9A9A00),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        )

        // Call control buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            EndCall {
                WebRTCManager.endCall()
                navController.navigate("home")
            }
            SwitchCamera()
            ToggleAudio()
            ToggleVideo()
        }
    }
}

@Composable
fun LocalVideoPreview(modifier: Modifier, localRenderer: SurfaceViewRenderer) {
    AndroidView(
        factory = { localRenderer },
        modifier = modifier
    )
}

@Composable
fun RemoteVideoPreview(modifier: Modifier, remoteRenderer: SurfaceViewRenderer) {
    AndroidView(
        factory = { remoteRenderer },
        modifier = modifier
    )
}

@Composable
fun CircularButton(icon: Int, backgroundColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun EndCall(onClick: () -> Unit) {
    CircularButton(
        icon = R.drawable.call2,
        backgroundColor = Color.Red,
        onClick = onClick
    )
}

@Composable
fun SwitchCamera(modifier: Modifier = Modifier) {
    CircularButton(
        icon = R.drawable.cameraswitch, // Add this icon
        backgroundColor = Color.Gray
    ) {
        WebRTCManager.switchCamera()
    }
}

@Composable
fun ToggleAudio(modifier: Modifier = Modifier) {
    val isMuteState by remember { mutableStateOf(false) }
    CircularButton(
        icon = if (isMuteState){
            R.drawable.canceled_mic
        } else{
            R.drawable.mic
        },
        backgroundColor = Color.Gray
    ) {
        WebRTCManager.toggleAudio()
    }
}

@Composable
fun ToggleVideo(modifier: Modifier = Modifier) {
    val isVideoState by remember { mutableStateOf(true) }
    CircularButton(
        icon = if (isVideoState){
            R.drawable.video
        }else{
            R.drawable.video_canceled
        },
        backgroundColor = Color.Gray
    ) {
        WebRTCManager.toggleVideo()
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun CallAnswerScreenPreview() {
    val navController = rememberNavController()
    AnswerScreen(
        navController = navController,
        roomId = "test-room",
        callerName = "Ama"
    )
}
