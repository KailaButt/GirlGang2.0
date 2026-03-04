package com.example.consolicalm

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.consolicalm.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(
    selectedTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // ✅ Current Firebase user (null if logged out)
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: "Not logged in"

    // ✅ Firestore
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    // ✅ Nickname storage (local)
    val userPrefs = UserPrefs(context)
    var nickname by remember { mutableStateOf(userPrefs.nickname ?: "") }

    // ✅ Friends UI state
    var myFriendCode by remember { mutableStateOf<String?>(null) }
    var friendCodeInput by remember { mutableStateOf("") }
    var friendStatus by remember { mutableStateOf("") }
    var loadingFriendOp by remember { mutableStateOf(false) }

    /**
     * Ensure the user has a doc in public_users and has a friendCode.
     * Runs once when ProfileScreen opens (or when user changes).
     */
    LaunchedEffect(user?.uid) {
        if (user == null) {
            myFriendCode = null
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
                myFriendCode = (existingCode ?: user.uid.take(6).uppercase())
            }
        } catch (e: Exception) {
            friendStatus = "Couldn’t load friend code. Check internet."
        }
    }

    /**
     * Adds a friend by friendCode:
     * - Finds matching user in public_users where friendCode == input
     * - Saves friend doc into: public_users/{myUid}/friends/{friendUid}
     */
    suspend fun addFriendByCode(codeRaw: String): String {
        val me = FirebaseAuth.getInstance().currentUser ?: return "Not logged in"
        val code = codeRaw.trim().uppercase()

        if (code.length < 6) return "Friend code must be 6+ characters"

        // find the other user
        val match = db.collection("public_users")
            .whereEqualTo("friendCode", code)
            .get()
            .await()

        val friendDoc = match.documents.firstOrNull() ?: return "No user found for that code"

        val friendUid = friendDoc.getString("uid") ?: friendDoc.id
        if (friendUid == me.uid) return "That’s your own code 😭"

        val friendNickname = friendDoc.getString("nickname") ?: ""
        val friendCode = friendDoc.getString("friendCode") ?: code

        // save under my friends subcollection
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

        return "Friend added ✅"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // ✅ THIS IS THE FIX
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

        // Profile picture placeholder
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

        // ✅ Show user email
        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium
        )

        // ---------------- NICKNAME SECTION ----------------
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

                        // also update Firestore nickname if logged in + doc exists
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
        // --------------------------------------------------

        // ---------------- FRIENDS SECTION ----------------
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
                                    if (friendStatus.contains("✅")) friendCodeInput = ""
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
        // --------------------------------------------------

        // Themes
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

        // ✅ Log out button (you will now be able to scroll to see it)
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