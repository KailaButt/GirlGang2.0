package com.example.consolicalm

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class RewardRedemptionRecord(
    val id: String,
    val rewardTitle: String,
    val rewardSubtitle: String,
    val pointsCost: Int,
    val recipientEmail: String,
    val code: String,
    val timestampMillis: Long
)

class RewardRedemptionPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("reward_redemptions", Context.MODE_PRIVATE)
    private val key = "records"

    fun getRecords(): List<RewardRedemptionRecord> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    RewardRedemptionRecord(
                        id = obj.optString("id"),
                        rewardTitle = obj.optString("rewardTitle"),
                        rewardSubtitle = obj.optString("rewardSubtitle"),
                        pointsCost = obj.optInt("pointsCost"),
                        recipientEmail = obj.optString("recipientEmail"),
                        code = obj.optString("code"),
                        timestampMillis = obj.optLong("timestampMillis")
                    )
                )
            }
        }
    }

    fun addRecord(record: RewardRedemptionRecord) {
        val array = JSONArray()
        val updated = listOf(record) + getRecords()
        updated.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("rewardTitle", item.rewardTitle)
                    .put("rewardSubtitle", item.rewardSubtitle)
                    .put("pointsCost", item.pointsCost)
                    .put("recipientEmail", item.recipientEmail)
                    .put("code", item.code)
                    .put("timestampMillis", item.timestampMillis)
            )
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}

fun currentUserRewardEmail(): String = FirebaseAuth.getInstance().currentUser?.email?.trim().orEmpty()

fun generateGiftCardCode(rewardTitle: String): String {
    val letters = rewardTitle.uppercase(Locale.US).filter { it.isLetter() }.take(4).padEnd(4, 'X')
    val token = UUID.randomUUID().toString().replace("-", "").uppercase(Locale.US)
    return listOf(token.substring(0, 4), token.substring(4, 8), token.substring(8, 12)).joinToString("-") { "$letters$it" }
}

fun formatRewardDate(timestampMillis: Long): String {
    val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return fmt.format(Date(timestampMillis))
}
