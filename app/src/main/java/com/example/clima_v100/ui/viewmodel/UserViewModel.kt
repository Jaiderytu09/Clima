package com.example.clima_v100.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clima_v100.data.local.dao.UserDao
import com.example.clima_v100.data.local.entity.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class RecoveryState {
    object Idle : RecoveryState()
    object Loading : RecoveryState()
    data class QuestionFound(val question: String) : RecoveryState()
    object AnswerVerified : RecoveryState()
    object PasswordChanged : RecoveryState()
    data class Error(val message: String) : RecoveryState()
}

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userDao: UserDao
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _recoveryState = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState

    private var recoveryEmail: String = ""

    private fun hashText(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = userDao.login(email.trim(), hashText(password))
                if (user != null) {
                    userDao.logoutAll()
                    userDao.setLoggedIn(user.id)
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Correo o contraseña incorrectos")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error inesperado")
            }
        }
    }

    fun register(username: String, email: String, password: String,
                 securityQuestion: String, securityAnswer: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                if (username.isBlank() || email.isBlank() || password.isBlank()
                    || securityQuestion.isBlank() || securityAnswer.isBlank()) {
                    _authState.value = AuthState.Error("Todos los campos son obligatorios")
                    return@launch
                }
                val user = User(
                    username = username.trim(),
                    email = email.trim(),
                    passwordHash = hashText(password),
                    securityQuestion = securityQuestion,
                    securityAnswerHash = hashText(securityAnswer.trim().lowercase())
                )
                userDao.registerUser(user)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error("El correo ya está registrado")
            }
        }
    }

    fun findSecurityQuestion(email: String) {
        viewModelScope.launch {
            _recoveryState.value = RecoveryState.Loading
            try {
                val user = userDao.getUserByEmail(email.trim())
                if (user != null && user.securityQuestion.isNotBlank()) {
                    recoveryEmail = email.trim()
                    _recoveryState.value = RecoveryState.QuestionFound(user.securityQuestion)
                } else {
                    _recoveryState.value = RecoveryState.Error("No encontramos una cuenta con ese correo")
                }
            } catch (e: Exception) {
                _recoveryState.value = RecoveryState.Error("Error inesperado")
            }
        }
    }

    fun verifySecurityAnswer(answer: String) {
        viewModelScope.launch {
            _recoveryState.value = RecoveryState.Loading
            try {
                val user = userDao.getUserByEmail(recoveryEmail)
                if (user != null && user.securityAnswerHash == hashText(answer.trim().lowercase())) {
                    _recoveryState.value = RecoveryState.AnswerVerified
                } else {
                    _recoveryState.value = RecoveryState.Error("Respuesta incorrecta")
                }
            } catch (e: Exception) {
                _recoveryState.value = RecoveryState.Error("Error inesperado")
            }
        }
    }

    fun resetPassword(newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _recoveryState.value = RecoveryState.Loading
            try {
                if (newPassword != confirmPassword) {
                    _recoveryState.value = RecoveryState.Error("Las contraseñas no coinciden")
                    return@launch
                }
                if (newPassword.length < 6) {
                    _recoveryState.value = RecoveryState.Error("Mínimo 6 caracteres")
                    return@launch
                }
                userDao.updatePassword(recoveryEmail, hashText(newPassword))
                _recoveryState.value = RecoveryState.PasswordChanged
            } catch (e: Exception) {
                _recoveryState.value = RecoveryState.Error("Error al cambiar contraseña")
            }
        }
    }

    fun resetState() { _authState.value = AuthState.Idle }
    fun resetRecoveryState() { _recoveryState.value = RecoveryState.Idle }
}