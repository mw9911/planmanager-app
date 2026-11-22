// AuthEventManager.kt
package com.example.planmanagerapp.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthEventManager @Inject constructor() {
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>()
    val sessionExpiredEvent = _sessionExpiredEvent.asSharedFlow()

    suspend fun emitSessionExpired() {
        _sessionExpiredEvent.emit(Unit)
    }
}