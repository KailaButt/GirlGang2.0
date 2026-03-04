package com.example.consolicalm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onStart() {
        super.onStart()
        // ✅ Auto-login: if already signed in, skip login screen
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LoginOrSignUpScreen(
                onAuthSuccess = {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            )
        }
    }
}

@Composable
fun LoginOrSignUpScreen(onAuthSuccess: () -> Unit) {

    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ✅ toggle between login vs sign up
    var isSignUpMode by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = if (isSignUpMode) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = {
                errorMessage = null
                val trimmedEmail = email.trim()

                // ✅ Block empty
                if (trimmedEmail.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter your email and password."
                    return@Button
                }

                isLoading = true

                // ✅ Sign up OR login depending on mode
                val task = if (isSignUpMode) {
                    auth.createUserWithEmailAndPassword(trimmedEmail, password)
                } else {
                    auth.signInWithEmailAndPassword(trimmedEmail, password)
                }

                task.addOnCompleteListener { result ->
                    isLoading = false
                    if (result.isSuccessful) {
                        onAuthSuccess()
                    } else {
                        errorMessage = result.exception?.localizedMessage
                            ?: "Something went wrong. Try again."
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    isLoading && isSignUpMode -> "Creating..."
                    isLoading -> "Logging in..."
                    isSignUpMode -> "Create Account"
                    else -> "Login"
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = { isSignUpMode = !isSignUpMode }
        ) {
            Text(
                text = if (isSignUpMode)
                    "Already have an account? Log in"
                else
                    "New here? Create an account"
            )
        }
    }
}