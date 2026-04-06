package com.example.consolicalm

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class WeeklyInsightsRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun leaderboardCollection(weekId: String) =
        db.collection("weekly_insights")
            .document(weekId)
            .collection("leaderboard")

    fun saveMyWeeklyInsight(
        nickname: String,
        pointsEarned: Int,
        studySessions: Int,
        tasksCompleted: Int,
        calmMinutes: Int,
        moodCheckIns: Int,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val user = auth.currentUser
        if (user == null) {
            onError(IllegalStateException("No signed in user"))
            return
        }

        val weekId = currentWeekId()

        val insight = WeeklyInsight(
            uid = user.uid,
            nickname = nickname,
            pointsEarned = pointsEarned,
            studySessions = studySessions,
            tasksCompleted = tasksCompleted,
            calmMinutes = calmMinutes,
            moodCheckIns = moodCheckIns,
            weekId = weekId,
            updatedAt = System.currentTimeMillis()
        )

        leaderboardCollection(weekId)
            .document(user.uid)
            .set(insight)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun listenToMyWeeklyInsight(
        onUpdate: (WeeklyInsight?) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration? {
        val user = auth.currentUser
        if (user == null) {
            onUpdate(null)
            return null
        }

        val weekId = currentWeekId()

        return leaderboardCollection(weekId)
            .document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val insight = snapshot?.toObject(WeeklyInsight::class.java)
                onUpdate(insight)
            }
    }
    fun incrementWeeklyStats(
        nickname: String = "You",
        pointsToAdd: Int = 0,
        sessionsToAdd: Int = 0,
        tasksToAdd: Int = 0,
        calmMinutesToAdd: Int = 0,
        moodCheckInsToAdd: Int = 0,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val user = auth.currentUser
        if (user == null) {
            onError(IllegalStateException("No signed in user"))
            return
        }

        val weekId = currentWeekId()
        val docRef = leaderboardCollection(weekId).document(user.uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)

            val current = snapshot.toObject(WeeklyInsight::class.java) ?: WeeklyInsight(
                uid = user.uid,
                nickname = nickname,
                weekId = weekId
            )

            val updated = current.copy(
                nickname = if (nickname.isBlank()) current.nickname else nickname,
                pointsEarned = current.pointsEarned + pointsToAdd,
                studySessions = current.studySessions + sessionsToAdd,
                tasksCompleted = current.tasksCompleted + tasksToAdd,
                calmMinutes = current.calmMinutes + calmMinutesToAdd,
                moodCheckIns = current.moodCheckIns + moodCheckInsToAdd,
                updatedAt = System.currentTimeMillis()
            )

            transaction.set(docRef, updated)
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener {
            onError(it)
        }
    }
    fun listenToLeaderboard(
        limitCount: Long = 10,
        onUpdate: (List<WeeklyInsight>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        val weekId = currentWeekId()

        return leaderboardCollection(weekId)
            .orderBy("pointsEarned", Query.Direction.DESCENDING)
            .limit(limitCount)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(WeeklyInsight::class.java)
                } ?: emptyList()

                onUpdate(list)
            }
    }
}