package com.example.callerscreen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.callerscreen.callUi.AnswerScreen
import com.example.callerscreen.callUi.CallScreen
import com.example.callerscreen.callUi.DeclineScreen

@Composable
fun Navigation(
    roomId: String
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "callScreen") {
        composable("callScreen") { CallScreen(navController) }
        composable("declineScreen") { DeclineScreen(navController) }
        composable("answeredScreen") { AnswerScreen(
            navController = navController,
            roomId = roomId,
            isCaller = true
        ) }
    }
}