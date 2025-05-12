package com.example.pushnotification


object CallEventBus {
    private var listener: ((roomId: String, callerName: String) -> Unit)? = null

    fun setListener(callback: (String, String) -> Unit) {
        listener = callback
    }

    fun triggerNavigateToCall(roomId: String, callerName: String) {
        listener?.invoke(roomId, callerName)
    }
}


