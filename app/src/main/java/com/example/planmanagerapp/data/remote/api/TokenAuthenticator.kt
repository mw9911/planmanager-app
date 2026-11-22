package com.example.planmanagerapp.data.remote.api

import com.example.planmanagerapp.data.local.AuthEventManager
import com.example.planmanagerapp.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiProvider: Provider<AuthApiService>,
    private val authEventManager: AuthEventManager // 💡 글로벌 이벤트 버스 주입
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 💡 방어막 1, 2: 연속 루프 및 재귀 루프 차단
        if (response.priorResponse?.priorResponse != null) return null
        if (response.request.url.encodedPath.contains("reissue")) return null

        val currentRefreshToken = runBlocking { tokenManager.getRefreshToken() }
        if (currentRefreshToken.isNullOrEmpty()) return null

        return try {
            val reissueResponse = authApiProvider.get().reissueTokenSync(currentRefreshToken).execute()

            if (reissueResponse.isSuccessful) {
                val newTokens = reissueResponse.body()
                runBlocking {
                    newTokens?.accessToken?.let { tokenManager.saveAccessToken(it) }
                    newTokens?.refreshToken?.let { tokenManager.saveRefreshToken(it) }
                }

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens?.accessToken}")
                    .build()
            } else {
                runBlocking {
                    tokenManager.clearTokens()
                    authEventManager.emitSessionExpired() // 💡 갱신 API가 401을 뱉으면 강제 로그아웃 신호 발송
                }
                null
            }
        } catch (e: Exception) {
            runBlocking {
                tokenManager.clearTokens()
                authEventManager.emitSessionExpired() // 💡 네트워크 에러 시 강제 로그아웃 신호 발송
            }
            null
        }
    }
}