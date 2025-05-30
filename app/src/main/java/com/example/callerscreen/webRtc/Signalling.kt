package com.example.callerscreen

import android.annotation.SuppressLint
import com.google.firebase.firestore.FirebaseFirestore


object SignalingManager {

    @SuppressLint("StaticFieldLeak")
    private val signalingRef = FirebaseFirestore.getInstance()

    fun sendCallAccept(roomId: String) {
        val callStatus = mapOf("status" to "accepted")
        signalingRef.collection("calls")
            .document(roomId)
            .set(callStatus)
            .addOnSuccessListener {
                println("Call accepted successfully.")
            }
            .addOnFailureListener { e ->
                println("Error accepting call: $e")
            }
    }

    fun sendCallDecline(roomId: String) {
        val callStatus = mapOf("status" to "declined")
        signalingRef.collection("calls")
            .document(roomId)
            .set(callStatus)
            .addOnSuccessListener {
                println("Call declined successfully.")
            }
            .addOnFailureListener { e ->
                println("Error declining call: $e")
            }
    }

    fun sendQuickMessage(roomId: String, message: String) {
        val quickMessage = mapOf("message" to message)
        signalingRef.collection("calls")
            .document(roomId)
            .collection("messages")
            .add(quickMessage)
            .addOnSuccessListener {
                println("Quick message sent successfully.")
            }
            .addOnFailureListener { e ->
                println("Error sending quick message: $e")
            }
    }
}
