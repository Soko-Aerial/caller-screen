package com.example.callerscreen

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.callerscreen.callUi.AnswerScreen
import com.example.callerscreen.callUi.CallScreen
import com.example.callerscreen.ui.theme.CallerScreenTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CallerScreenTheme {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                val navController = rememberNavController()


                val lifecycleOwner = LocalLifecycleOwner.current

                fun onNewIntent(intent: Intent?) {
                    if (intent != null) {
                        super.onNewIntent(intent)
                    }
                    handleNavigationIntent(intent)
                }


                LaunchedEffect(Unit) {
                    CallEventBus.navigateToCall.collect { (roomId, callerName) ->
                        Log.d("NAV", "Navigating to incoming_call_screen/$roomId/$callerName")
                        navController.navigate("incoming_call_screen/$roomId/$callerName")
                    }
                }


                NavHost(navController, startDestination = "home") {
                    composable("home") { HomeScreen(navController) }


                    composable(
                        "incoming_call_screen/{roomId}/{callerName}",
                        arguments = listOf(
                            navArgument("roomId") { type = NavType.StringType },
                            navArgument("callerName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                        val callerName = backStackEntry.arguments?.getString("callerName") ?: "Unknown"
                        CallScreen(roomId = roomId, callerName = callerName, navController = navController)
                    }

                    composable(
                        "answer_screen/{roomId}/{callerName}",
                        arguments = listOf(
                            navArgument("roomId") { type = NavType.StringType },
                            navArgument("callerName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                        val callerName = backStackEntry.arguments?.getString("callerName") ?: "Unknown"
                        AnswerScreen(roomId = roomId, callerName = callerName, navController = navController)
                    }
                }
            }
        }
    }
}


fun handleNavigationIntent(intent: Intent?) {
    val navigateTo = intent?.getStringExtra("navigateTo")
    val roomId = intent?.getStringExtra("room_id")
    val callerName = intent?.getStringExtra("caller_name")

    if (navigateTo == "call" && roomId != null && callerName != null) {
        CoroutineScope(Dispatchers.Main).launch {
            CallEventBus.triggerNavigateToCall(roomId, callerName)
        }
    }
}

//@Preview
//@Composable
//private fun CallerScreen() {
//
//}

