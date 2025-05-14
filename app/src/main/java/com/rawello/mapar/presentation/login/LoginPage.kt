package com.rawello.mapar.presentation.login

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rawello.mapar.data.TokenManager
import com.rawello.mapar.data.models.LoginRequest
import com.rawello.mapar.data.network.ApiFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LoginPage(
    onDismiss: () -> Unit,
    onLogin: suspend (String, String) -> Unit,
    onRegister: suspend (String, String) -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: Context
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вход в аккаунт") },
        text = {
            Column {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Имя пользователя") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                onLogin(username, password)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
                ) {
                    Text("Войти")
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                onRegister(username, password)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank()
                ) {
                    Text("Регистрация")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Отмена")
            }
        }
    )
}