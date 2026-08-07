package com.nhakhoaquangninh.telesales

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object UnauthorizedEventBus {
    private val mutableEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    fun notifyUnauthorized() {
        mutableEvents.tryEmit(Unit)
    }
}
