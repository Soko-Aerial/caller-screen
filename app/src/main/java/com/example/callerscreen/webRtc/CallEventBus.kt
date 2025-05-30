package com.example.callerscreen.webRtc

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object CallEventBus {
    private val _navigateToCall = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val navigateToCall = _navigateToCall.asSharedFlow()

    suspend fun triggerNavigateToCall(roomId: String, callerName: String) {
        _navigateToCall.emit(Pair(roomId, callerName))
    }
}
