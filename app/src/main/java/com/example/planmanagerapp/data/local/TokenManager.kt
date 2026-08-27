package com.example.planmanagerapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// 최상단에 DataStore 인스턴스 위임 생성 (싱글톤 보장)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        // 💡 Refresh Token을 위한 고유 키 추가
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }

    // 1. Access 토큰 저장
    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = token
        }
    }

    // 💡 1-1. Refresh 토큰 저장 (추가됨)
    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN_KEY] = token
        }
    }

    // 2. 비동기 환경용 토큰 스트림 (ViewModel 등에서 사용)
    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN_KEY]
    }

    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_KEY]
    }

    // 3. 동기 환경용 단일 토큰 조회 (Interceptor에서 사용)
    suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }.first()
    }

    // 💡 3-1. 동기 환경용 Refresh 단일 토큰 조회 (Authenticator에서 사용됨)
    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }.first()
    }

    // 💡 4. 로그아웃 또는 401(만료) 시 전체 토큰 일괄 물리 삭제 (메서드명 clearTokens로 변경됨)
    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            // 특정 키만 지우거나 .clear()로 prefs 전체를 초기화할 수 있습니다.
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }
}