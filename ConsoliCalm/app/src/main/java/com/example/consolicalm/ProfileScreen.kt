package com.example.consolicalm

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.consolicalm.ui.theme.AppTheme
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AddedYouItem(
    val fromUid: String = "",
    val fromName: String = "",
    val fromFriendCode: String = "",
    val timestamp: Long = 0L
)

private const val PROFILE_TAG = "ProfileScreen"

private fun fallbackFriendCode(uid: String): String = uid.take(6).uppercase()

private fun friendMessageForException(e: Exception): String {
    val firestoreError = e as? FirebaseFirestoreException
    return when (firestoreError?.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Couldn’t load friend code. Firestore permissions are blocking access."
        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            "Couldn’t load friend code. Please log out and back in."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Couldn’t reach Firestore right now. Try again in a moment."
        else -> "Couldn’t load friend code right now."
    }
}

private suspend fun ensurePublicUserDoc(
    db: FirebaseFirestore,
    uid: String,
    nickname: String,
    existingFriendCode: String? = null
): String {
    val userRef = db.collection("public_users").document(uid)
    val fallbackCode = existingFriendCode ?: fallbackFriendCode(uid)

    val doc = userRef.get().await()
    if (!doc.exists()) {
        userRef.set(
            mapOf(
                "uid" to uid,
                "nickname" to nickname,
                "friendCode" to fallbackCode
            ),
            SetOptions.merge()
        ).await()
        return fallbackCode
    }

    val storedCode = doc.getString("friendCode")?.takeIf { it.isNotBlank() } ?: fallbackCode
    val updates = mutableMapOf<String, Any>(
        "uid" to uid,
        "friendCode" to storedCode
    )
    if (nickname.isNotBlank() && doc.getString("nickname") != nickname) {
        updates["nickname"] = nickname
    }
    userRef.set(updates, SetOptions.merge()).await()
    return storedCode
}

@Composable
fun ProfileScreen(
    selectedTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: "Not logged in"

    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    val userPrefs = UserPrefs(context)
    var nickname by remember { mutableStateOf(userPrefs.nickname ?: "") }
    var cachedFriendCode by remember { mutableStateOf(user?.uid?.let { fallbackFriendCode(it) }) }

    var myFriendCode by remember { mutableStateOf<String?>(cachedFriendCode) }
    var friendCodeInput by remember { mutableStateOf("") }
    var friendStatus by remember { mutableStateOf("") }
    var loadingFriendOp by remember { mutableStateOf(false) }

    var resetInfo by remember { mutableStateOf<String?>(null) }
    var resetError by remember { mutableStateOf<String?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user?.uid) {
        if (user == null) {
            myFriendCode = null
            cachedFriendCode = null
            return@LaunchedEffect
        }

        val localFallback = fallbackFriendCode(user.uid)
        myFriendCode = cachedFriendCode ?: localFallback
        cachedFriendCode = localFallback
        friendStatus = ""

        try {
            val resolvedCode = ensurePublicUserDoc(
                db = db,
                uid = user.uid,
                nickname = userPrefs.nickname ?: nickname,
                existingFriendCode = localFallback
            )
            myFriendCode = resolvedCode
            cachedFriendCode = resolvedCode
        } catch (e: Exception) {
            Log.e(PROFILE_TAG, "Failed to load friend code", e)
            myFriendCode = localFallback
            cachedFriendCode = localFallback
            // Keep the usable fallback code visible without showing a scary error on load.
            // Firestore permission or sync issues should only surface when the cloud action
            // actually matters, such as sending a friend request.
            friendStatus = if (localFallback.isBlank()) friendMessageForException(e) else ""
        }
    }

    suspend fun addFriendByCode(codeRaw: String): String {
        val me = FirebaseAuth.getInstance().currentUser ?: return "Not logged in"
        val code = codeRaw.trim().uppercase()

        if (code.length < 6) return "Friend code must be 6+ characters"

        val myCode = ensurePublicUserDoc(
            db = db,
            uid = me.uid,
            nickname = userPrefs.nickname ?: nickname,
            existingFriendCode = myFriendCode ?: cachedFriendCode
        )
        myFriendCode = myCode
        cachedFriendCode = myCode

        val match = db.collection("public_users")
            .whereEqualTo("friendCode", code)
            .get()
            .await()

        val friendDoc = match.documents.firstOrNull() ?: return "No user found for that code"

        val friendUid = friendDoc.getString("uid") ?: friendDoc.id
        if (friendUid == me.uid) return "That’s your own code 😭"

        val friendNickname = friendDoc.getString("nickname") ?: ""
        val friendCode = friendDoc.getString("friendCode") ?: code

        val friendData = hashMapOf(
            "uid" to friendUid,
            "nickname" to friendNickname,
            "friendCode" to friendCode,
            "addedAt" to System.currentTimeMillis()
        )

        db.collection("public_users")
            .document(me.uid)
            .collection("friends")
            .document(friendUid)
            .set(friendData)
            .await()

        val myDoc = db.collection("public_users")
            .document(me.uid)
            .get()
            .await()

        val myNickname = myDoc.getString("nickname")?.trim().orEmpty()
        val myDisplayName = if (myNickname.isNotBlank()) myNickname else (me.email ?: "Someone")
        val myCodeForRequest = myDoc.getString("friendCode") ?: myCode

        val addedYouData = hashMapOf(
            "fromUid" to me.uid,
            "fromName" to myDisplayName,
            "fromFriendCode" to myCodeForRequest,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("public_users")
            .document(friendUid)
            .collection("added_you")
            .document(me.uid)
            .set(addedYouData)
            .await()

        return "Friend request sent ✅"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )
        }

        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Nickname",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enter nickname") }
                )

                Button(
                    onClick = {
                        userPrefs.nickname = nickname
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            scope.launch {
                                try {
                                    val resolvedCode = ensurePublicUserDoc(
                                        db = db,
                                        uid = currentUser.uid,
                                        nickname = nickname,
                                        existingFriendCode = myFriendCode ?: cachedFriendCode
                                    )
                                    myFriendCode = resolvedCode
                                    cachedFriendCode = resolvedCode
                                    friendStatus = "Nickname saved ✅"
                                } catch (e: Exception) {
                                    Log.e(PROFILE_TAG, "Failed to save nickname", e)
                                    friendStatus = "Saved nickname locally. Cloud sync will retry later."
                                }
                            }
                        } else {
                            friendStatus = "Nickname saved locally ✅"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Nickname")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Friends",
                    style = MaterialTheme.typography.titleMedium
                )

                if (user == null) {
                    Text(
                        text = "Log in to use friends.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Your Friend Code: ${myFriendCode ?: "Loading..."}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = friendCodeInput,
                        onValueChange = { friendCodeInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Enter friend code") },
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                loadingFriendOp = true
                                friendStatus = ""
                                try {
                                    friendStatus = addFriendByCode(friendCodeInput)
                                    if (friendStatus.contains("✅")) {
                                        friendCodeInput = ""
                                    }
                                } catch (e: Exception) {
                                    Log.e(PROFILE_TAG, "Failed to add friend", e)
                                    friendStatus = when ((e as? FirebaseFirestoreException)?.code) {
                                        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                            "Couldn’t add friend. Firestore permissions are blocking access."
                                        FirebaseFirestoreException.Code.UNAVAILABLE ->
                                            "Couldn’t reach Firestore right now. Try again in a moment."
                                        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                                            "Please log out and back in, then try again."
                                        else -> "Couldn’t add friend right now."
                                    }
                                } finally {
                                    loadingFriendOp = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loadingFriendOp
                    ) {
                        Text(if (loadingFriendOp) "Adding..." else "Add Friend")
                    }

                    if (friendStatus.isNotBlank()) {
                        Text(
                            text = friendStatus,
                            color = if (friendStatus.contains("✅")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Themes",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Pick a look for the app.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeButton(
                        label = "Default",
                        selected = selectedTheme == AppTheme.DEFAULT,
                        onClick = { onSelectTheme(AppTheme.DEFAULT) }
                    )
                    ThemeButton(
                        label = "Sage",
                        selected = selectedTheme == AppTheme.SAGE,
                        onClick = { onSelectTheme(AppTheme.SAGE) }
                    )
                    ThemeButton(
                        label = "Mocha",
                        selected = selectedTheme == AppTheme.MOCHA,
                        onClick = { onSelectTheme(AppTheme.MOCHA) }
                    )
                }
            }
        }

        TextButton(
            onClick = {
                resetInfo = null
                resetError = null
                showChangePasswordDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Change password")
        }

        if (resetError != null) {
            Text(
                text = resetError!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (resetInfo != null) {
            Text(
                text = resetInfo!!,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (showChangePasswordDialog) {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            val currentEmail = currentUser?.email

            var currentPassword by remember { mutableStateOf("") }
            var newPassword by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showChangePasswordDialog = false },
                title = { Text("Change password") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Enter your current password and your new password.")

                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            label = { Text("Current password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm new password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            resetInfo = null
                            resetError = null

                            if (currentEmail.isNullOrBlank() || currentUser == null) {
                                resetError = "No logged-in account found."
                                return@TextButton
                            }
                            if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                                resetError = "Please fill out all fields."
                                return@TextButton
                            }
                            if (newPassword != confirmPassword) {
                                resetError = "New passwords do not match."
                                return@TextButton
                            }
                            if (newPassword.length < 6) {
                                resetError = "New password must be at least 6 characters."
                                return@TextButton
                            }

                            val credential = EmailAuthProvider.getCredential(currentEmail, currentPassword)
                            currentUser.reauthenticate(credential)
                                .addOnCompleteListener { reauthTask ->
                                    if (!reauthTask.isSuccessful) {
                                        resetError = reauthTask.exception?.localizedMessage
                                            ?: "Current password is incorrect."
                                        return@addOnCompleteListener
                                    }

                                    currentUser.updatePassword(newPassword)
                                        .addOnCompleteListener { updateTask ->
                                            if (updateTask.isSuccessful) {
                                                resetInfo = "Password updated successfully."
                                                showChangePasswordDialog = false
                                            } else {
                                                resetError = updateTask.exception?.localizedMessage
                                                    ?: "Could not update password. Try again."
                                            }
                                        }
                                }
                        }
                    ) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangePasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log out")
        }
    }
}

fun formatNotificationTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "today"

    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minute = 60_000L
    val hour = 60 * minute

    return when {
        diff < minute -> "just now"
        diff < hour -> "${diff / minute}m ago"
        diff < 24 * hour -> "${diff / hour}h ago"
        else -> "earlier"
    }
}

@Composable
private fun ThemeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(label)
        }
    }
}