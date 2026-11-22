package com.example.planmanagerapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planmanagerapp.data.local.AppDatabase
import com.example.planmanagerapp.data.local.AuthEventManager
import com.example.planmanagerapp.data.local.TokenManager
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
    val errorMessage: String? = null,
    val isCheckingAuth: Boolean = true,
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager,
    private val appDatabase: AppDatabase,
    private val authEventManager: AuthEventManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val sessionExpiredEvent = authEventManager.sessionExpiredEvent

    init {
        checkAutoLogin()
    }

    private fun checkAutoLogin() {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken()

                if (!token.isNullOrBlank()) {
                    _uiState.update { it.copy(isCheckingAuth = false, isAuthenticated = true, isSuccess = true) }
                } else {
                    _uiState.update { it.copy(isCheckingAuth = false, isAuthenticated = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCheckingAuth = false, isAuthenticated = false) }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // 💡 1. DTO 필드명 규격에 맞게 loginId로 전달
                val response = authApi.login(LoginRequest(loginId = email, password = password))
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null && !authResponse.accessToken.isNullOrBlank()) {
                        tokenManager.saveAccessToken(authResponse.accessToken)
                        authResponse.refreshToken?.let { tokenManager.saveRefreshToken(it) }

                        _uiState.update { it.copy(isLoading = false, isSuccess = true, isAuthenticated = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "서버가 토큰을 반환하지 않았습니다.") }
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
                // 💡 2. 임시 username 생성 (이메일의 @ 앞부분 추출) 후 파라미터 전달
                val tempUsername = email.substringBefore("@")
                val response = authApi.signUp(SignUpRequest(username = tempUsername, email = email, password = password))

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
        _uiState.update { AuthUiState(isCheckingAuth = false, isAuthenticated = false) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                tokenManager.clearTokens()
                appDatabase.clearAllTables()
            } catch (e: Exception) {
                android.util.Log.e("AuthTest", "DB 초기화 에러: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    onLogoutComplete()
                }
            }
        }
    }

    fun resetState() {
        _uiState.update { AuthUiState(isCheckingAuth = false, isAuthenticated = false) }
    }
}