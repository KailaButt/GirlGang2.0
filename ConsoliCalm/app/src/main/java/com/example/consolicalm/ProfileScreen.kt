package com.example.consolicalm

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.consolicalm.ui.theme.AppTheme
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AddedYouItem(
    val fromUid: String = "",
    val fromName: String = "",
    val fromFriendCode: String = "",
    val timestamp: Long = 0L
)

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

    var myFriendCode by remember { mutableStateOf<String?>(null) }
    var friendCodeInput by remember { mutableStateOf("") }
    var friendStatus by remember { mutableStateOf("") }
    var loadingFriendOp by remember { mutableStateOf(false) }

    var addedYouList by remember { mutableStateOf<List<AddedYouItem>>(emptyList()) }
    var requestStatus by remember { mutableStateOf("") }

    var resetInfo by remember { mutableStateOf<String?>(null) }
    var resetError by remember { mutableStateOf<String?>(null) }

    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var notificationStatus by remember { mutableStateOf("") }

    suspend fun loadAddedYouRequests(currentUid: String) {
        val result = db.collection("public_users")
            .document(currentUid)
            .collection("added_you")
            .get()
            .await()

        addedYouList = result.documents.mapNotNull {
            it.toObject(AddedYouItem::class.java)
        }.sortedByDescending { it.timestamp }
    }

    suspend fun loadNotifications(currentUid: String) {
        val result = db.collection("public_users")
            .document(currentUid)
            .collection("notifications")
            .orderBy("timestamp")
            .get()
            .await()

        notifications = result.documents.map { doc ->
            AppNotification(
                id = doc.id,
                type = doc.getString("type") ?: "",
                fromUid = doc.getString("fromUid") ?: "",
                fromName = doc.getString("fromName") ?: "",
                activityId = doc.getString("activityId") ?: "",
                activityMessage = doc.getString("activityMessage") ?: "",
                emoji = doc.getString("emoji") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                read = doc.getBoolean("read") ?: false
            )
        }.sortedByDescending { it.timestamp }
    }

    LaunchedEffect(user?.uid) {
        if (user == null) {
            myFriendCode = null
            addedYouList = emptyList()
            notifications = emptyList()
            return@LaunchedEffect
        }

        try {
            val userRef = db.collection("public_users").document(user.uid)
            val doc = userRef.get().await()

            if (!doc.exists()) {
                val generatedCode = user.uid.take(6).uppercase()
                val newUser = hashMapOf(
                    "uid" to user.uid,
                    "nickname" to (userPrefs.nickname ?: ""),
                    "friendCode" to generatedCode
                )
                userRef.set(newUser).await()
                myFriendCode = generatedCode
            } else {
                val existingCode = doc.getString("friendCode")
                myFriendCode = existingCode ?: user.uid.take(6).uppercase()
            }
        } catch (e: Exception) {
            friendStatus = "Couldn’t load friend code. Check internet."
        }

        try {
            loadAddedYouRequests(user.uid)
        } catch (e: Exception) {
            addedYouList = emptyList()
        }

        try {
            loadNotifications(user.uid)
        } catch (e: Exception) {
            notifications = emptyList()
        }
    }

    suspend fun addFriendByCode(codeRaw: String): String {
        val me = FirebaseAuth.getInstance().currentUser ?: return "Not logged in"
        val code = codeRaw.trim().uppercase()

        if (code.length < 6) return "Friend code must be 6+ characters"

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
        val myCode = myDoc.getString("friendCode") ?: me.uid.take(6).uppercase()

        val addedYouData = hashMapOf(
            "fromUid" to me.uid,
            "fromName" to myDisplayName,
            "fromFriendCode" to myCode,
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

    suspend fun acceptFriendRequest(item: AddedYouItem): String {
        val me = FirebaseAuth.getInstance().currentUser ?: return "Not logged in"

        val friendData = hashMapOf(
            "uid" to item.fromUid,
            "nickname" to item.fromName,
            "friendCode" to item.fromFriendCode,
            "addedAt" to System.currentTimeMillis()
        )

        db.collection("public_users")
            .document(me.uid)
            .collection("friends")
            .document(item.fromUid)
            .set(friendData)
            .await()

        db.collection("public_users")
            .document(me.uid)
            .collection("added_you")
            .document(item.fromUid)
            .delete()
            .await()

        loadAddedYouRequests(me.uid)
        return "Friend request accepted ✅"
    }

    suspend fun declineFriendRequest(item: AddedYouItem): String {
        val me = FirebaseAuth.getInstance().currentUser ?: return "Not logged in"

        db.collection("public_users")
            .document(me.uid)
            .collection("added_you")
            .document(item.fromUid)
            .delete()
            .await()

        loadAddedYouRequests(me.uid)
        return "Friend request declined"
    }

    suspend fun markAllNotificationsRead() {
        val me = FirebaseAuth.getInstance().currentUser ?: return
        val batch = db.batch()

        notifications.forEach { item ->
            val ref = db.collection("public_users")
                .document(me.uid)
                .collection("notifications")
                .document(item.id)

            batch.update(ref, "read", true)
        }

        batch.commit().await()
        loadNotifications(me.uid)
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
            Text("Profile", style = MaterialTheme.typography.titleLarge)
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

                        val u = FirebaseAuth.getInstance().currentUser
                        if (u != null) {
                            db.collection("public_users")
                                .document(u.uid)
                                .set(
                                    mapOf(
                                        "uid" to u.uid,
                                        "nickname" to nickname,
                                        "friendCode" to (myFriendCode ?: u.uid.take(6).uppercase())
                                    )
                                )
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
                Text("Friends", style = MaterialTheme.typography.titleMedium)

                if (user == null) {
                    Text("Log in to use friends.", style = MaterialTheme.typography.bodyMedium)
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
                                    friendStatus = "Couldn’t add friend. Check internet."
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
                            color = if (friendStatus.contains("✅"))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
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
                    text = "Friend Requests",
                    style = MaterialTheme.typography.titleMedium
                )

                if (user == null) {
                    Text("Log in to see requests.", style = MaterialTheme.typography.bodyMedium)
                } else if (addedYouList.isEmpty()) {
                    Text(
                        text = "No pending requests.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    addedYouList.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = item.fromName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "wants to connect with you",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                requestStatus = try {
                                                    acceptFriendRequest(item)
                                                } catch (e: Exception) {
                                                    "Couldn’t accept request."
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Accept")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                requestStatus = try {
                                                    declineFriendRequest(item)
                                                } catch (e: Exception) {
                                                    "Couldn’t decline request."
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Decline")
                                    }
                                }
                            }
                        }
                    }
                }

                if (requestStatus.isNotBlank()) {
                    Text(
                        text = requestStatus,
                        color = if (requestStatus.contains("✅"))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null
                        )
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (notifications.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        markAllNotificationsRead()
                                        notificationStatus = "Marked all as read"
                                    } catch (_: Exception) {
                                        notificationStatus = "Couldn’t update notifications"
                                    }
                                }
                            }
                        ) {
                            Text("Mark all read")
                        }
                    }
                }

                if (user == null) {
                    Text("Log in to see notifications.")
                } else if (notifications.isEmpty()) {
                    Text("No notifications yet 🌸")
                } else {
                    notifications.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.read) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = when (item.type) {
                                        "reaction" -> "${item.fromName} reacted ${item.emoji} to your activity"
                                        else -> "New notification"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (item.activityMessage.isNotBlank()) {
                                    Text(
                                        text = "\"${item.activityMessage}\"",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Text(
                                    text = formatNotificationTimestamp(item.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                if (notificationStatus.isNotBlank()) {
                    Text(
                        text = notificationStatus,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                Text("Themes", style = MaterialTheme.typography.titleMedium)

                Text(
                    "Pick a look for the app.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

        Spacer(modifier = Modifier.height(12.dp))

        var showChangePasswordDialog by remember { mutableStateOf(false) }

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
            Text(resetError!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (resetInfo != null) {
            Text(resetInfo!!, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Enter your current password and your new password.")
                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            label = { Text("Current password") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New password") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm new password") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
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
                    ) { Text("Update") }
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

private fun formatNotificationTimestamp(timestamp: Long): String {
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
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}