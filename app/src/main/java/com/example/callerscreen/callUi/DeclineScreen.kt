package com.example.callerscreen.callUi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

//  Declined Call Screen
@Composable
fun DeclineScreen(navController: NavHostController) {
    ScreenLayout(
        title = "Call Declined",
        button1Text = "Back",
        button1Color = Color.Gray,
        button1Action = { navController.popBackStack() }
    )
}


@Composable
fun ScreenLayout(
    title: String,
    button1Text: String,
    button1Color: Color,
    button1Action: () -> Unit,
    button2Text: String? = null,
    button2Color: Color? = null,
    button2Action: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 24.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = button1Action, colors = ButtonDefaults.buttonColors(button1Color)) {
            Text(button1Text)
        }

        button2Text?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = button2Action!!, colors = ButtonDefaults.buttonColors(button2Color!!)) {
                Text(it)
            }
        }
    }
}