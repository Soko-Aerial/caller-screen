package com.example.callerscreen

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.callerscreen.callUi.AnswerScreen
import com.example.callerscreen.callUi.CallScreen

@Composable
fun Navigation(
    roomId: String
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "callScreen") {
        composable("callScreen") { CallScreen(navController) }
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

    }
}