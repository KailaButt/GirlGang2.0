package com.example.consolicalm

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * AuthGate shows AuthScreen when logged out,
 * and shows your normal app UI when logged in.
 */
@Composable
fun AuthGate(content: @Composable () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var loggedIn by remember { mutableStateOf(auth.currentUser != null) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            loggedIn = it.currentUser != null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (loggedIn) content() else AuthScreen()
}

@Composable
fun AuthScreen() {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()

    fun submit() {
        message = null
        loading = true
        scope.launch {
            try {
                if (isLogin) {
                    auth.signInWithEmailAndPassword(email.trim(), password).await()
                    message = "Logged in ✅"
                } else {
                    auth.createUserWithEmailAndPassword(email.trim(), password).await()
                    message = "Account created ✅"
                }
            } catch (e: Exception) {
                message = e.message ?: "Something went wrong."
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLogin) "Log in" else "Create account",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { submit() },
            enabled = !loading && email.isNotBlank() && password.length >= 6,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (loading) "Please wait..."
                else if (isLogin) "Log in"
                else "Sign up"
            )
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { isLogin = !isLogin },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isLogin) "Need an account? Sign up"
                else "Already have an account? Log in"
            )
        }

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it)
        }
    }
}

fun signOut() {
    FirebaseAuth.getInstance().signOut()
}