package com.example.callerscreen.callUi


import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.example.callerscreen.Permissions.RequestPermissions
import com.example.callerscreen.WebRTC.WebRTCManager
import com.example.sigtrack_calll.R
import org.webrtc.SurfaceViewRenderer

//@Composable
//fun AnswerScreen(navController: NavController) {
//    val cameraSelectorState = remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
//    val isLocalVideoSmall = remember { mutableStateOf(true) } // Track which preview is small
//    val pipOffset = remember { mutableStateOf(Offset(50f, 100f)) } // PiP position
//
//    RequestPermissions()
//
//    Box(modifier = Modifier
//        .fillMaxSize()
//        .background(Color(0xFFEFEFEF))) {
//        // Large Preview (Can be either local or remote video)
//        if (isLocalVideoSmall.value) {
//            RemoteVideoPreview(modifier = Modifier.fillMaxSize())
//        } else {
//            CameraPreview(LocalLifecycleOwner.current, cameraSelectorState, Modifier.fillMaxSize())
//        }
//
//        // Draggable PiP Local Preview
//        Box(
//            modifier = Modifier
//                .offset { IntOffset(pipOffset.value.x.toInt(), pipOffset.value.y.toInt()) }
//                .size(150.dp)
//                .clip(RoundedCornerShape(10.dp))
//                .background(Color.Black)
//                .pointerInput(Unit) {
//                    detectDragGestures { change, dragAmount ->
//                        change.consume()
//                        pipOffset.value = pipOffset.value.copy(
//                            x = (pipOffset.value.x + dragAmount.x).coerceIn(0f, 1000f),
//                            y = (pipOffset.value.y + dragAmount.y).coerceIn(0f, 2000f)
//                        )
//                    }
//                }
//                .clickable { isLocalVideoSmall.value = !isLocalVideoSmall.value },
//            contentAlignment = Alignment.Center
//        ) {
//            if (isLocalVideoSmall.value) {
//                CameraPreview(
//                    LocalLifecycleOwner.current,
//                    cameraSelectorState,
//                    Modifier.fillMaxSize()
//                )
//            } else {
//                RemoteVideoPreview(modifier = Modifier.fillMaxSize())
//            }
//        }
//
//        // Buttons
//        Column(
//            modifier = Modifier
//                .align(Alignment.CenterEnd)
//                .padding(end = 20.dp),
//            verticalArrangement = Arrangement.spacedBy(20.dp)
//        ) {
//            CameraToggleButton(cameraSelectorState)
//            EndCall(navController)
//        }
//    }
//}


@Composable
fun AnswerScreen(
    navController: NavHostController,
    roomId: String,
    isCaller: Boolean // Pass whether this screen is for the caller or receiver
) {
    // Remember the lifecycle of your activity
    val context = LocalContext.current
    val localRenderer = remember { SurfaceViewRenderer(context) }
    val remoteRenderer = remember { SurfaceViewRenderer(context) }
    val cameraSelectorState = remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    RequestPermissions()
    // Initialize WebRTC once
    LaunchedEffect(Unit) {
        WebRTCManager.init(context)
        WebRTCManager.setSurfaceViews(localRenderer, remoteRenderer)
        WebRTCManager.startLocalVideo()
        WebRTCManager.joinCall(roomId, isCaller)
    }

    // Handle UI layout for the screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFEFEF))
    ) {
        // Title Text
        Text(
            "ALPHA KILO",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9A9A00),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        )


        // Buttons on the right
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CameraButton(cameraSelectorState)

            EndCall {
                WebRTCManager.endCall()  // Handle end call
                navController.navigate("home") // Navigate back to the previous screen
            }
        }

        // Local and Remote video views
        AndroidView(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(200.dp),
            factory = { localRenderer }
        )

        AndroidView(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(200.dp),
            factory = { remoteRenderer }
        )
    }
}





@Composable
fun CameraPreview(
    lifecycleOwner: LifecycleOwner,
    cameraSelectorState: MutableState<CameraSelector>,
    modifier: Modifier
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(cameraSelectorState.value) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelectorState.value, preview)
        } catch (e: Exception) {
            Log.e("CameraPreview", "Use case binding failed", e)
        }
    }
    AndroidView({ previewView }, modifier = modifier)
}

@Composable
fun RemoteVideoPreview(modifier: Modifier) {
    Box(modifier = modifier
        .background(Color.Gray),
        contentAlignment = Alignment.Center) {
        Text("Remote Video", color = Color.White)
    }
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
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun CameraButton(cameraSelectorState: MutableState<CameraSelector>) {
    CircularButton(
        icon = R.drawable.camera,
        backgroundColor = Color.LightGray,
        onClick = {
            cameraSelectorState.value =
                if (cameraSelectorState.value == CameraSelector.DEFAULT_BACK_CAMERA)
                    CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        }
    )
}


@Composable
fun EndCall(modifier: Modifier = Modifier, onClick: () -> Unit) {
    CircularButton(
        icon = R.drawable.call2,
        backgroundColor = Color.LightGray,
        onClick = onClick
    )
}


