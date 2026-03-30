package com.example.consolicalm

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Calendar

data class ActivityItem(
    val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val reactions: Map<String, List<String>> = emptyMap()
)

data class AppNotification(
    val id: String = "",
    val type: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val activityId: String = "",
    val activityMessage: String = "",
    val emoji: String = "",
    val timestamp: Long = 0L,
    val read: Boolean = false
)

fun saveActivity(message: String) {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val db = FirebaseFirestore.getInstance()

    val userRef = db.collection("public_users").document(user.uid)

    userRef.get().addOnSuccessListener { doc ->
        val nickname = doc.getString("nickname")?.trim().orEmpty()
        val fallbackEmail = user.email ?: "Someone"
        val finalDisplayName = if (nickname.isNotBlank()) nickname else fallbackEmail

        val activity = hashMapOf(
            "uid" to user.uid,
            "displayName" to finalDisplayName,
            "email" to fallbackEmail,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "reactions" to hashMapOf<String, List<String>>()
        )

        db.collection("activity_feed").add(activity)
    }
}

fun addReaction(activityId: String, emoji: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val currentUid = currentUser.uid
    val db = FirebaseFirestore.getInstance()

    val activityRef = db.collection("activity_feed").document(activityId)
    val currentUserRef = db.collection("public_users").document(currentUid)

    db.runTransaction { transaction ->
        val activitySnap = transaction.get(activityRef)
        val currentUserSnap = transaction.get(currentUserRef)

        val rawReactions = activitySnap.get("reactions") as? Map<*, *> ?: emptyMap<Any, Any>()
        val updatedReactions = mutableMapOf<String, MutableList<String>>()

        rawReactions.forEach { (key, value) ->
            val emojiKey = key as? String ?: return@forEach
            val uidList = (value as? List<*>)?.mapNotNull { it as? String }?.toMutableList()
                ?: mutableListOf()
            updatedReactions[emojiKey] = uidList
        }

        val selectedList = updatedReactions[emoji] ?: mutableListOf()
        val alreadyReacted = selectedList.contains(currentUid)

        if (alreadyReacted) {
            selectedList.remove(currentUid)
        } else {
            selectedList.add(currentUid)
        }

        updatedReactions[emoji] = selectedList
        transaction.update(activityRef, "reactions", updatedReactions)

        val activityOwnerUid = activitySnap.getString("uid").orEmpty()
        val activityMessage = activitySnap.getString("message").orEmpty()

        if (!alreadyReacted && activityOwnerUid.isNotBlank() && activityOwnerUid != currentUid) {
            val nickname = currentUserSnap.getString("nickname")?.trim().orEmpty()
            val fallbackEmail = currentUser.email ?: "Someone"
            val reactorName = if (nickname.isNotBlank()) nickname else fallbackEmail

            val notificationRef = db.collection("public_users")
                .document(activityOwnerUid)
                .collection("notifications")
                .document()

            val notification = hashMapOf(
                "type" to "reaction",
                "fromUid" to currentUid,
                "fromName" to reactorName,
                "activityId" to activityId,
                "activityMessage" to activityMessage,
                "emoji" to emoji,
                "timestamp" to System.currentTimeMillis(),
                "read" to false
            )

            transaction.set(notificationRef, notification)
        }
    }
}

fun listenToTodayActivityFeed(onResult: (List<ActivityItem>) -> Unit): ListenerRegistration {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfDay = calendar.timeInMillis

    return FirebaseFirestore.getInstance()
        .collection("activity_feed")
        .whereGreaterThanOrEqualTo("timestamp", startOfDay)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, _ ->
            if (snapshot == null) {
                onResult(emptyList())
                return@addSnapshotListener
            }

            val items = snapshot.documents.map { doc ->
                val parsedReactions = mutableMapOf<String, List<String>>()
                val rawReactions = doc.get("reactions") as? Map<*, *>

                rawReactions?.forEach { (key, value) ->
                    val emoji = key as? String
                    val uidList = (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    if (emoji != null) {
                        parsedReactions[emoji] = uidList
                    }
                }

                ActivityItem(
                    id = doc.id,
                    uid = doc.getString("uid") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    email = doc.getString("email") ?: "",
                    message = doc.getString("message") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    reactions = parsedReactions
                )
            }

            onResult(items)
        }
}