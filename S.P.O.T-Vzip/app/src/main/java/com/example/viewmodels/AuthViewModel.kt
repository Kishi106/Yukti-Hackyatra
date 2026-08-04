package com.example.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthSessionPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

/**
 * No backend exists in this app, so sign-in/sign-up are placeholder operations:
 * they validate input locally, simulate a network round trip, then persist a
 * local session via [AuthSessionPrefs]. Swap [simulateAuthCall] for a real
 * repository call if/when a backend is wired up.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionPrefs = AuthSessionPrefs(application)

    private val _loginState = MutableStateFlow(AuthUiState())
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    private val _signUpState = MutableStateFlow(AuthUiState())
    val signUpState: StateFlow<AuthUiState> = _signUpState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow(AuthUiState())
    val forgotPasswordState: StateFlow<AuthUiState> = _forgotPasswordState.asStateFlow()

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            _loginState.value = AuthUiState(errorMessage = "Enter a valid email address.")
            return
        }
        if (password.length < 6) {
            _loginState.value = AuthUiState(errorMessage = "Password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            _loginState.value = AuthUiState(isLoading = true)
            simulateAuthCall()
            sessionPrefs.setLoggedIn(trimmedEmail)
            _loginState.value = AuthUiState(isSuccess = true)
        }
    }

    fun signUp(name: String, email: String, password: String, confirmPassword: String) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        if (trimmedName.isBlank()) {
            _signUpState.value = AuthUiState(errorMessage = "Enter your full name.")
            return
        }
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            _signUpState.value = AuthUiState(errorMessage = "Enter a valid email address.")
            return
        }
        if (password.length < 6) {
            _signUpState.value = AuthUiState(errorMessage = "Password must be at least 6 characters.")
            return
        }
        if (password != confirmPassword) {
            _signUpState.value = AuthUiState(errorMessage = "Passwords do not match.")
            return
        }

        viewModelScope.launch {
            _signUpState.value = AuthUiState(isLoading = true)
            simulateAuthCall()
            sessionPrefs.setLoggedIn(trimmedEmail)
            _signUpState.value = AuthUiState(isSuccess = true)
        }
    }

    fun sendPasswordReset(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            _forgotPasswordState.value = AuthUiState(errorMessage = "Enter a valid email address.")
            return
        }

        viewModelScope.launch {
            _forgotPasswordState.value = AuthUiState(isLoading = true)
            simulateAuthCall()
            _forgotPasswordState.value = AuthUiState(isSuccess = true)
        }
    }

    private suspend fun simulateAuthCall() {
        delay(800L)
    }
}
