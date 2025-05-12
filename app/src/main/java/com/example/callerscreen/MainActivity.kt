package com.example.callerscreen

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.callerscreen.ui.theme.CallerScreenTheme
import com.example.pushnotification.CallEventBus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CallerScreenTheme {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    val navController = rememberNavController()

//                LaunchedEffect(Unit) {
//                    CallEventBus.navigateToCall.collect {
//                        Log.d("NAV", "Navigating to CALL screen")
//                        navController.navigate("navigation/{roomId}")
//                    }
//                }

                LaunchedEffect(Unit) {
                    CallEventBus.setListener { roomId, callerName ->
                        navController.navigate("incoming_call_screen/$roomId/$callerName")
                    }
                }


                NavHost(navController, startDestination = "home") {
                        composable("home") { HomeScreen() }
                        composable("navigation/{roomId}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("roomId")
                            Navigation(roomId = id ?: "")
                        }
                    }
            }
        }
    }
}


@Preview
@Composable
private fun CallerScreen() {

}

