package com.example.callerscreen

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.Navigation
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.callerscreen.ui.theme.CallerScreenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CallerScreenTheme {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    val navController = rememberNavController()

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

