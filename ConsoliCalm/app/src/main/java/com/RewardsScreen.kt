package com.example.consolicalm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class RewardOption(
    val title: String,
    val subtitle: String,
    val pointsCost: Int,
    val description: String,
    val iconRes: Int? = null,
    val iconVector: ImageVector? = null,
    val unlockLabel: String,
    val deliveryLabel: String
)

@Composable
fun RewardsScreen(
    calmPoints: Int,
    onBack: () -> Unit,
    onRedeem: (Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { RewardRedemptionPrefs(context) }
    val currentEmail = remember { currentUserRewardEmail() }

    val rewards = remember {
        listOf(
            RewardOption(
                title = "Amazon Gift Card",
                subtitle = "$10 eGift",
                pointsCost = 2500,
                description = "Shop anything",
                iconRes = R.drawable.amazon,
                unlockLabel = "Milestone reward",
                deliveryLabel = "Email delivery"
            ),
            RewardOption(
                title = "Starbucks Gift Card",
                subtitle = "$5 eGift",
                pointsCost = 1500,
                description = "Coffee reward ☕",
                iconRes = R.drawable.starbucks,
                unlockLabel = "Quick treat",
                deliveryLabel = "Email delivery"
            ),
            RewardOption(
                title = "Campus Bookstore Credit",
                subtitle = "$10 eGift",
                pointsCost = 2200,
                description = "Books & supplies 📚",
                iconRes = R.drawable.bookstore,
                unlockLabel = "School essentials",
                deliveryLabel = "Email delivery"
            ),
            RewardOption(
                title = "Dunkin Gift Card",
                subtitle = "$5 eGift",
                pointsCost = 1400,
                description = "Snacks + coffee",
                iconRes = R.drawable.dunkin,
                unlockLabel = "Quick pick-me-up",
                deliveryLabel = "Email delivery"
            ),
            RewardOption(
                title = "Target Gift Card",
                subtitle = "$10 eGift",
                pointsCost = 2800,
                description = "Dorm + school essentials",
                iconRes = R.drawable.target,
                unlockLabel = "Everyday reward",
                deliveryLabel = "Email delivery"
            ),
            RewardOption(
                title = "DoorDash Gift Card",
                subtitle = "$10 eGift",
                pointsCost = 3200,
                description = "Study-night food break",
                iconRes = R.drawable.doordash,
                unlockLabel = "Bigger reward",
                deliveryLabel = "Email delivery"
            )
        )
    }

    var selectedReward by remember { mutableStateOf<RewardOption?>(null) }
    var latestRedemption by remember { mutableStateOf<RewardRedemptionRecord?>(null) }
    var redemptionHistory by remember { mutableStateOf(prefs.getRecords()) }

    LaunchedEffect(Unit) {
        redemptionHistory = prefs.getRecords()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rewards 🎁", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onBack) { Text("Back") }
            }
        }

        item {
            Text("You have $calmPoints Calm Points 🌿")
        }

        if (currentEmail.isNotBlank()) {
            item {
                Text(
                    "Gift cards are delivered to $currentEmail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(rewards) { reward ->
            RewardCard(
                reward = reward,
                calmPoints = calmPoints,
                onClick = { selectedReward = reward }
            )
        }

        if (redemptionHistory.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Recent redemptions", fontWeight = FontWeight.SemiBold)
            }

            items(redemptionHistory.take(4)) { record ->
                RedemptionHistoryCard(record = record)
            }
        }
    }

    selectedReward?.let { reward ->
        val canRedeem = calmPoints >= reward.pointsCost
        val needsEmail = currentEmail.isBlank()

        AlertDialog(
            onDismissRequest = { selectedReward = null },
            title = { Text("Redeem ${reward.title}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(reward.subtitle, fontWeight = FontWeight.SemiBold)
                    Text(reward.description)
                    Text("Cost: ${reward.pointsCost} points")
                    Text("Delivery: ${reward.deliveryLabel}")
                    when {
                        needsEmail -> Text(
                            "Log in with an email account to deliver the eGift card.",
                            color = MaterialTheme.colorScheme.error
                        )
                        !canRedeem -> Text(
                            "You need ${reward.pointsCost - calmPoints} more points to redeem this reward.",
                            color = MaterialTheme.colorScheme.error
                        )
                        else -> Text(
                            "A gift-card-style code will be generated, saved to your reward history, and prepared for email delivery.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = canRedeem && !needsEmail,
                    onClick = {
                        val record = RewardRedemptionRecord(
                            id = reward.title + System.currentTimeMillis(),
                            rewardTitle = reward.title,
                            rewardSubtitle = reward.subtitle,
                            pointsCost = reward.pointsCost,
                            recipientEmail = currentEmail,
                            code = generateGiftCardCode(reward.title),
                            timestampMillis = System.currentTimeMillis()
                        )
                        onRedeem(reward.pointsCost)
                        prefs.addRecord(record)
                        redemptionHistory = prefs.getRecords()
                        latestRedemption = record
                        selectedReward = null
                    }
                ) {
                    Text("Redeem")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedReward = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    latestRedemption?.let { record ->
        GiftCardDeliveredDialog(
            record = record,
            onDismiss = { latestRedemption = null }
        )
    }
}

@Composable
private fun RewardCard(
    reward: RewardOption,
    calmPoints: Int,
    onClick: () -> Unit
) {
    val canRedeem = calmPoints >= reward.pointsCost
    val progress = (calmPoints.toFloat() / reward.pointsCost.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RewardIcon(reward = reward)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(reward.title, fontWeight = FontWeight.Bold)
                        Text(
                            reward.unlockLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(reward.pointsCost.toString() + " pts")
                    Text(
                        reward.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(reward.description)

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (canRedeem) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = if (canRedeem) "Ready to redeem" else "Need ${reward.pointsCost - calmPoints} more points",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (canRedeem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RewardIcon(reward: RewardOption) {
    when {
        reward.iconRes != null -> {
            Image(
                painter = painterResource(id = reward.iconRes),
                contentDescription = reward.title,
                modifier = Modifier.size(40.dp)
            )
        }
        reward.iconVector != null -> {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    reward.iconVector,
                    contentDescription = reward.title,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CardGiftcard, contentDescription = reward.title)
            }
        }
    }
}

@Composable
private fun RedemptionHistoryCard(record: RewardRedemptionRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(record.rewardTitle, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Sent to ${record.recipientEmail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatRewardDate(record.timestampMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("-${record.pointsCost}")
        }
    }
}

@Composable
private fun GiftCardDeliveredDialog(
    record: RewardRedemptionRecord,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gift card delivered") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(record.rewardTitle, fontWeight = FontWeight.SemiBold)
                }
                Text(record.rewardSubtitle)
                Text("Sent to ${record.recipientEmail}")
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = record.code,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Keep this code for the project demo. It is also saved in Recent redemptions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { copyCode(context, record.code) }) {
                    Text("Copy code")
                }
                Button(onClick = { emailGiftCard(context, record) }) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Email")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

private fun copyCode(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Gift card code", code))
    Toast.makeText(context, "Gift card code copied", Toast.LENGTH_SHORT).show()
}

private fun emailGiftCard(context: Context, record: RewardRedemptionRecord) {
    val subject = "Your ${record.rewardTitle}"
    val body = buildString {
        appendLine("Thanks for using ConsoliCalm.")
        appendLine()
        appendLine("Reward: ${record.rewardTitle} (${record.rewardSubtitle})")
        appendLine("Gift card code: ${record.code}")
        appendLine("Redeemed on: ${formatRewardDate(record.timestampMillis)}")
        appendLine()
        appendLine("This is a class-project demo redemption email.")
    }

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = android.net.Uri.parse("mailto:${record.recipientEmail}")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found on this device.", Toast.LENGTH_SHORT).show()
    }
}
