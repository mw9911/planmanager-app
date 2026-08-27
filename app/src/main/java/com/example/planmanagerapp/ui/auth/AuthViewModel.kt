package com.example.planmanagerapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.planmanagerapp.data.local.TokenManager // 💡 TokenManager 임포트 필수
import com.example.planmanagerapp.data.remote.api.AuthApiService
import com.example.planmanagerapp.data.remote.dto.LoginRequest
import com.example.planmanagerapp.data.remote.dto.SignUpRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenManager: TokenManager // 💡 SessionManager가 아닌 TokenManager를 주입
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = authApi.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    // 💡 핵심: 통신 가로채기(Interceptor)가 읽을 수 있도록 TokenManager(DataStore)에 저장!
                    response.body()?.let { authResponse ->
                        // 💡 1. Access Token 추출 및 저장
                        authResponse.accessToken?.let {
                            tokenManager.saveAccessToken(it)
                        }
                        // 💡 2. Refresh Token 추출 및 저장
                        authResponse.refreshToken?.let {
                            tokenManager.saveRefreshToken(it)
                        }
                    }
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
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
}