package com.example.planmanagerapp.data.local

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    // 💡 1. Context 대신 SharedPreferences를 바로 주입받습니다.
    // Hilt가 DatabaseModule에서 만든 EncryptedSharedPreferences를 여기에 꽂아줍니다.
    private val prefs: SharedPreferences
) {
    // 💡 2. 직접 getSharedPreferences를 호출하던 줄은 삭제되었습니다.

    // UI 관찰용 Flow 상태 관리
    private val _accessTokenFlow = MutableStateFlow(getAccessToken())
    val accessTokenFlow: Flow<String?> = _accessTokenFlow.asStateFlow()

    private val _refreshTokenFlow = MutableStateFlow(getRefreshToken())
    val refreshTokenFlow: Flow<String?> = _refreshTokenFlow.asStateFlow()

    fun saveAccessToken(token: String) {
        prefs.edit().putString("access_token", token).apply() // 메모리와 디스크에 즉시 갱신
        _accessTokenFlow.value = token
    }

    fun saveRefreshToken(token: String) {
        prefs.edit().putString("refresh_token", token).apply()
        _refreshTokenFlow.value = token
    }

    fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun getRefreshToken(): String? {
        return prefs.getString("refresh_token", null)
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
        _accessTokenFlow.value = null
        _refreshTokenFlow.value = null
    }
}