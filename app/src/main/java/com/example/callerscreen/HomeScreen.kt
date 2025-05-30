package com.example.callerscreen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.google.firebase.messaging.FirebaseMessaging


@Composable
fun HomeScreen(navController: NavHostController) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val token = FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                val token = task.result
            }.toString()

            Spacer(modifier = Modifier.padding(20.dp))
            CopyTokenBox(token = token)
            Spacer(modifier = Modifier.padding(20.dp))
            NotificationBox(navController = navController)
        }
    }
}

@Composable
fun CopyTokenBox(token: String) {
//    val context = LocalContext.current
//    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(40.dp)
            .background(Color.LightGray, shape = RoundedCornerShape(16.dp))
            .clickable {
//                val clip = ClipData.newPlainText("FCM Token", token)
//                clipboard.setPrimaryClip(clip)
//                Toast.makeText(context, "Token copied to clipboard!", Toast.LENGTH_SHORT).show()

                copyFcmToken(context)
            },
        contentAlignment = Alignment.Center
    ) {
        Text("Copy token",
            color = Color.Black
        )
    }
}


@Composable
fun NotificationBox(navController: NavController) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(40.dp)
            .background(Color.LightGray, shape = RoundedCornerShape(16.dp))
            .clickable {
                //navController.navigate(MainActivity.Routes.NOTIFICATION_LOG)
            },

        contentAlignment = Alignment.Center,
    ) {
        Text("Notification Log",
            color = Color.Black)
    }
    Box(
//        modifier = Modifier.clickable(
//            //onClick = { navController.navigate(MainActivity.Routes.NOTIFICATION_LOG) }
//        )
    ){}
}

fun copyFcmToken(context: Context) {
    FirebaseMessaging.getInstance().token
        .addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Toast.makeText(context, "Failed to get FCM token", Toast.LENGTH_SHORT).show()
                return@addOnCompleteListener
            }

            val token = task.result
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("FCM Token", token)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(context, "FCM token copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
}




