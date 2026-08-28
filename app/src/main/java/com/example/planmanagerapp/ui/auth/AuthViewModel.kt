package com.example.planmanagerapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planmanagerapp.data.local.AppDatabase
import com.example.planmanagerapp.data.local.TokenManager // 💡 TokenManager 임포트 필수
import com.example.planmanagerapp.data.remote.api.AuthApiService
import com.example.planmanagerapp.data.remote.dto.LoginRequest
import com.example.planmanagerapp.data.remote.dto.SignUpRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager, // 💡 SessionManager가 아닌 TokenManager를 주입
    private val appDatabase: AppDatabase // 💡 Hilt로 RoomDB 인스턴스 주입
    ) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = authApi.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val authResponse = response.body()

                    // 💡 응답 바디가 null이 아닌지, 토큰이 제대로 파싱되었는지 최후의 팩트 체크
                    android.util.Log.d("AuthTest", "DTO Parsed AccessToken: ${authResponse?.accessToken}")

                    if (authResponse != null && !authResponse.accessToken.isNullOrBlank()) {
                        // 확실하게 DataStore에 저장 (suspend 함수이므로 코루틴 내 순차 실행 보장)
                        tokenManager.saveAccessToken(authResponse.accessToken)

                        authResponse.refreshToken?.let {
                            tokenManager.saveRefreshToken(it)
                        }

                        // 💡 저장 직후, DataStore에 정말 들어갔는지 즉시 검증
                        val verifyToken = tokenManager.getAccessToken()
                        android.util.Log.d("AuthTest", "Verified Saved Token: $verifyToken")

                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "로그인은 성공했으나 서버가 토큰을 주지 않았습니다.") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "로그인 실패: ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = authApi.signUp(SignUpRequest(email, password))
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "회원가입 완료. 로그인해주세요.") }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "회원가입 실패") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }
    fun logout(onLogoutComplete: () -> Unit) {
        // 💡 핵심 팩트: 이전 로그인 성공 상태를 false로 초기화하여 '자동 로그인 루프'를 원천 차단
        _uiState.update { AuthUiState() }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                tokenManager.clearTokens()
                appDatabase.clearAllTables()
            } catch (e: Exception) {
                android.util.Log.e("AuthTest", "DB 초기화 에러: ${e.message}")
            } finally {
                // DB 삭제 및 토큰 파기가 완료되면 안전하게 화면 이동 트리거
                withContext(Dispatchers.Main) {
                    onLogoutComplete()
                }
            }
        }
    }
}