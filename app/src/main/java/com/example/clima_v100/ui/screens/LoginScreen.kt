@file:Suppress("SpellCheckingInspection")
@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.clima_v100.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.clima_v100.ui.viewmodel.AuthState
import com.example.clima_v100.ui.viewmodel.RecoveryState
import com.example.clima_v100.ui.viewmodel.UserViewModel
import androidx.compose.material.icons.automirrored.filled.Help

val securityQuestions = listOf(
    "¿Cuál es el nombre de tu primera mascota?",
    "¿En qué ciudad naciste?",
    "¿Cuál es el nombre de tu mejor amigo de la infancia?",
    "¿Cuál es el nombre de tu escuela primaria?",
    "¿Cuál es tu comida favorita?"
)

enum class AuthScreen { LOGIN, REGISTER, FORGOT_EMAIL, FORGOT_ANSWER, FORGOT_RESET }

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val recoveryState by viewModel.recoveryState.collectAsState()

    var currentScreen by remember { mutableStateOf(AuthScreen.LOGIN) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var securityQuestion by remember { mutableStateOf(securityQuestions[0]) }
    var securityAnswer by remember { mutableStateOf("") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmNewPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var questionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> onLoginSuccess()
            is AuthState.Error -> {
                errorMessage = (authState as AuthState.Error).message
                viewModel.resetState()
            }
            else -> {}
        }
    }

    LaunchedEffect(recoveryState) {
        when (recoveryState) {
            is RecoveryState.QuestionFound -> {
                currentScreen = AuthScreen.FORGOT_ANSWER
                errorMessage = null
            }
            is RecoveryState.AnswerVerified -> {
                currentScreen = AuthScreen.FORGOT_RESET
                errorMessage = null
            }
            is RecoveryState.PasswordChanged -> {
                currentScreen = AuthScreen.LOGIN
                errorMessage = null
                viewModel.resetRecoveryState()
            }
            is RecoveryState.Error -> {
                errorMessage = (recoveryState as RecoveryState.Error).message
                viewModel.resetRecoveryState()
            }
            else -> {}
        }
    }

    val isLoading = authState is AuthState.Loading || recoveryState is RecoveryState.Loading

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (currentScreen) {

                    AuthScreen.LOGIN -> {
                        Text("Iniciar Sesión", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo electrónico") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility, null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                viewModel.login(email, password)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            else Text("Entrar")
                        }

                        TextButton(onClick = {
                            currentScreen = AuthScreen.FORGOT_EMAIL
                            errorMessage = null
                        }) {
                            Text("¿Olvidaste tu contraseña?")
                        }

                        HorizontalDivider()

                        TextButton(onClick = {
                            currentScreen = AuthScreen.REGISTER
                            errorMessage = null
                        }) {
                            Text("¿No tienes cuenta? Regístrate")
                        }
                    }

                    AuthScreen.REGISTER -> {
                        Text("Crear cuenta", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Usuario") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo electrónico") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility, null
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirmar contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        if (confirmPasswordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility, null
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        HorizontalDivider()

                        Text(
                            "Pregunta de seguridad",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )

                        ExposedDropdownMenuBox(
                            expanded = questionExpanded,
                            onExpandedChange = { questionExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = securityQuestion,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Selecciona una pregunta") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Help, null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(questionExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = questionExpanded,
                                onDismissRequest = { questionExpanded = false }
                            ) {
                                securityQuestions.forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q, fontSize = 13.sp) },
                                        onClick = {
                                            securityQuestion = q
                                            questionExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = securityAnswer,
                            onValueChange = { securityAnswer = it },
                            label = { Text("Tu respuesta") },
                            leadingIcon = { Icon(Icons.Default.Shield, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                if (password != confirmPassword) {
                                    errorMessage = "Las contraseñas no coinciden"
                                    return@Button
                                }
                                viewModel.register(username, email, password, securityQuestion, securityAnswer)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            else Text("Crear cuenta")
                        }

                        TextButton(onClick = {
                            currentScreen = AuthScreen.LOGIN
                            errorMessage = null
                        }) {
                            Text("¿Ya tienes cuenta? Inicia sesión")
                        }
                    }

                    AuthScreen.FORGOT_EMAIL -> {
                        Icon(
                            Icons.Default.LockReset,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Recuperar contraseña", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Ingresa el correo con el que te registraste",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo electrónico") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                viewModel.findSecurityQuestion(email)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            else Text("Continuar")
                        }

                        TextButton(onClick = {
                            currentScreen = AuthScreen.LOGIN
                            errorMessage = null
                            viewModel.resetRecoveryState()
                        }) { Text("Volver al login") }
                    }

                    AuthScreen.FORGOT_ANSWER -> {
                        val question = (recoveryState as? RecoveryState.QuestionFound)?.question ?: ""

                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Pregunta de seguridad", fontSize = 22.sp, fontWeight = FontWeight.Bold)

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                question,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        OutlinedTextField(
                            value = recoveryAnswer,
                            onValueChange = { recoveryAnswer = it },
                            label = { Text("Tu respuesta") },
                            leadingIcon = { Icon(Icons.Default.Shield, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                viewModel.verifySecurityAnswer(recoveryAnswer)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            else Text("Verificar")
                        }

                        TextButton(onClick = {
                            currentScreen = AuthScreen.FORGOT_EMAIL
                            errorMessage = null
                            viewModel.resetRecoveryState()
                        }) { Text("Volver") }
                    }

                    AuthScreen.FORGOT_RESET -> {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Nueva contraseña", fontSize = 22.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Nueva contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        if (newPasswordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility, null
                                    )
                                }
                            },
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            label = { Text("Confirmar nueva contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                viewModel.resetPassword(newPassword, confirmNewPassword)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(
                                modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            else Text("Cambiar contraseña")
                        }
                    }
                }
            }
        }
    }
}