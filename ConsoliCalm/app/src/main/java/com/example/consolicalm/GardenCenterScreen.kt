package com.example.consolicalm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val GardenCream = Color(0xFFF6F1EA)
private val GardenSand = Color(0xFFE8E0D4)
private val GardenTan = Color(0xFFD9CDBE)
private val GardenCard = Color(0xFFF3EEE7)
private val GardenBlue = Color(0xFF89A3AE)
private val GardenBlueDark = Color(0xFF6F8D98)
private val GardenBlueSoft = Color(0xFFDCE7EB)
private val GardenLavender = Color(0xFFE7DDF3)
private val GardenText = Color(0xFF3D3A37)
private val GardenSubtext = Color(0xFF7A746E)
private val GardenAccent = Color(0xFFC7D8D0)
private val GardenGold = Color(0xFFF1C84C)

@Composable
fun GardenCenterScreen(
    onBackToStudy: () -> Unit
) {
    val points = GardenCenterStore.points
    val selected = GardenCenterStore.selectedPlant
    val owned = GardenCenterStore.owned
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F5F0),
                        GardenCream,
                        Color(0xFFF1ECE4)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopGardenBar(
                points = points,
                onBackToStudy = onBackToStudy
            )

            GardenHeroCard(
                selected = selected,
                stage = GardenCenterStore.stageFor(selected)
            )

            SectionTitle(
                title = "Plants",
                subtitle = "Choose a plant to grow through your focus sessions."
            )

            PlantShopCard(type = PlantType.JUNIPER_BONSAI, cost = 0)
            PlantShopCard(type = PlantType.REGULAR_TREE, cost = 120)
            PlantShopCard(type = PlantType.FLOWER, cost = 90)
            PlantShopCard(type = PlantType.CACTUS, cost = 110)

            SectionTitle(
                title = "Upgrades",
                subtitle = "Upgrades apply to your selected plant and help it grow faster."
            )

            val selectedPlantData = owned[selected]
            if (selectedPlantData == null) {
                EmptyUpgradeCard()
            } else {
                GardenCenterStore.upgradesFor(selected).forEach { up ->
                    UpgradeCard(
                        plantType = selected,
                        upgrade = up,
                        current = selectedPlantData
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = onBackToStudy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = GardenBlueDark
                ),
                border = BorderStroke(1.5.dp, GardenBlue.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Study", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TopGardenBar(
    points: Int,
    onBackToStudy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        TextButton(
            onClick = onBackToStudy,
            colors = ButtonDefaults.textButtonColors(contentColor = GardenBlueDark)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Back")
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, GardenBlue.copy(alpha = 0.18f)),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = GardenGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$points",
                    color = GardenText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GardenHeroCard(
    selected: PlantType,
    stage: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GardenBlueSoft,
                            Color(0xFFF2ECE5)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = GardenAccent.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFlorist,
                            contentDescription = null,
                            tint = GardenBlueDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Garden Center",
                            color = GardenBlueDark,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "Grow your focus into something beautiful",
                    style = MaterialTheme.typography.headlineSmall,
                    color = GardenText,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Spend points on plants and upgrades. Your selected plant grows every time you finish focus sessions.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GardenSubtext
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusPill(
                        title = "Currently Growing",
                        value = selected.title,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        title = "Stage",
                        value = "$stage / 4",
                        modifier = Modifier.width(110.dp)
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.58f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = selected.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = GardenText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selected.description,
                                color = GardenSubtext,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.White,
                                            GardenBlueSoft,
                                            Color(0xFFEFE7DB)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            PlantPreview(
                                type = selected,
                                stage = stage,
                                fullyUpgradedPreview = false,
                                modifier = Modifier.size(76.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, GardenBlue.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = GardenBlueDark
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = GardenText,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = GardenText,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = GardenSubtext
        )
    }
}

@Composable
private fun PlantShopCard(
    type: PlantType,
    cost: Int
) {
    val owned = GardenCenterStore.isOwned(type)
    val selected = GardenCenterStore.selectedPlant == type
    val stageNow = if (owned) GardenCenterStore.stageFor(type) else 0

    val containerBrush = when {
        selected -> Brush.horizontalGradient(
            colors = listOf(
                GardenLavender,
                Color(0xFFF2EAF8)
            )
        )
        owned -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFF7F3ED),
                Color(0xFFF0ECE5)
            )
        )
        else -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFAF7F2),
                Color(0xFFF3EEE6)
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerBrush)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.9f),
                                    GardenBlueSoft.copy(alpha = 0.55f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PlantPreview(
                        type = type,
                        stage = if (owned) stageNow else 1,
                        fullyUpgradedPreview = false,
                        modifier = Modifier.size(62.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = type.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = GardenText,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = type.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GardenSubtext
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.78f),
                            border = BorderStroke(1.dp, GardenBlue.copy(alpha = 0.18f))
                        ) {
                            Text(
                                text = "Stage $stageNow",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                color = GardenBlueDark,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selected) {
                            MiniBadge("Selected", GardenBlueSoft, GardenBlueDark)
                        } else if (owned) {
                            MiniBadge("Owned", Color(0xFFECE7DE), GardenSubtext)
                        } else {
                            MiniBadge("Shop Plant", Color(0xFFF0E8DD), GardenSubtext)
                        }

                        if (!owned) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = GardenGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cost.toString(),
                                    color = GardenText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.62f)),
                                contentAlignment = Alignment.Center
                            ) {
                                PlantPreview(
                                    type = type,
                                    stage = 4,
                                    fullyUpgradedPreview = true,
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Full look",
                                style = MaterialTheme.typography.labelSmall,
                                color = GardenSubtext
                            )
                        }

                        if (!owned) {
                            Button(
                                onClick = { GardenCenterStore.buyPlant(type, cost) },
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GardenBlue,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text("Buy Plant", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { GardenCenterStore.select(type) },
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) {
                                        Color.White.copy(alpha = 0.4f)
                                    } else {
                                        Color.Transparent
                                    },
                                    contentColor = GardenBlueDark
                                ),
                                border = BorderStroke(
                                    1.4.dp,
                                    if (selected) GardenBlueDark.copy(alpha = 0.35f)
                                    else GardenBlue.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text(
                                    if (selected) "Selected" else "Select",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniBadge(
    text: String,
    bg: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun UpgradeCard(
    plantType: PlantType,
    upgrade: PlantUpgrade,
    current: OwnedPlant
) {
    val ownedCount = when (upgrade.id) {
        "growth_boost" -> current.upgradeGrowthBoost
        "speed" -> current.upgradeSpeed
        "bonus_points" -> current.upgradeBonusPoints
        else -> 0
    }

    val icon = when (upgrade.id) {
        "growth_boost" -> Icons.Default.LocalFlorist
        "speed" -> Icons.Default.WaterDrop
        else -> Icons.Default.Star
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = GardenCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        GardenBlueSoft,
                                        Color.White
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = GardenBlueDark
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = upgrade.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = GardenText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = upgrade.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GardenSubtext
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GardenGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${upgrade.cost}",
                            color = GardenText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniBadge(
                    text = "Owned: $ownedCount",
                    bg = Color(0xFFEDE5D8),
                    textColor = GardenSubtext
                )

                Button(
                    onClick = { GardenCenterStore.buyUpgrade(plantType, upgrade) },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GardenBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(46.dp)
                ) {
                    Text("Buy upgrade", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EmptyUpgradeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GardenCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No plant selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = GardenText
            )
            Text(
                text = "Select a plant first to view its upgrades.",
                style = MaterialTheme.typography.bodyMedium,
                color = GardenSubtext,
                textAlign = TextAlign.Center
            )
        }
    }
}