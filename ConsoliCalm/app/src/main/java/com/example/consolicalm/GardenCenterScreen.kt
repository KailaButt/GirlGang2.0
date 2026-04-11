package com.example.consolicalm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GardenCream     = Color(0xFFF6F1EA)
private val GardenSand      = Color(0xFFE8E0D4)
private val GardenTan       = Color(0xFFD9CDBE)
private val GardenCard      = Color(0xFFF3EEE7)
private val GardenBlue      = Color(0xFF89A3AE)
private val GardenBlueDark  = Color(0xFF6F8D98)
private val GardenBlueSoft  = Color(0xFFDCE7EB)
private val GardenLavender  = Color(0xFFE7DDF3)
private val GardenText      = Color(0xFF3D3A37)
private val GardenSubtext   = Color(0xFF7A746E)
private val GardenAccent    = Color(0xFFC7D8D0)
private val GardenGold      = Color(0xFFF1C84C)

// 🌿 FIXED WOOD COLORS (no more dark/green)
private val WoodLight       = Color(0xFFE8E0D4)   // same as sand
private val WoodMid         = Color(0xFFD9CDBE)   // same as tan
private val WoodDark        = Color(0xFFC5B6A4)
// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun GardenCenterScreen(
    onBackToStudy: () -> Unit
) {
    val points   = GardenCenterStore.points
    val selected = GardenCenterStore.selectedPlant
    val owned    = GardenCenterStore.owned
    val scroll   = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
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
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Signboard header
            GardenSignHeader(points = points, onBack = onBackToStudy)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(6.dp))

                // Currently growing bench card
                GrowingBenchCard(
                    selected = selected,
                    stage = GardenCenterStore.stageFor(selected)
                )

                // Plants section
                ShelfDivider(label = "Plants for Sale")

                PlantShopCard(type = PlantType.JUNIPER_BONSAI, cost = 0)
                PlantShopCard(type = PlantType.REGULAR_TREE,   cost = 120)
                PlantShopCard(type = PlantType.FLOWER,         cost = 90)
                PlantShopCard(type = PlantType.CACTUS,         cost = 110)

                // Upgrades section
                ShelfDivider(label = "Upgrades & Fertilisers")

                val selectedPlantData = owned[selected]
                if (selectedPlantData == null) {
                    EmptyUpgradeCard()
                } else {
                    GardenCenterStore.upgradesFor(selected).forEach { up ->
                        UpgradeCard(
                            plantType = selected,
                            upgrade   = up,
                            current   = selectedPlantData
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onBackToStudy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor   = GardenBlueDark
                    ),
                    border = BorderStroke(1.5.dp, GardenBlue.copy(alpha = 0.45f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Back to Study", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ─── Signboard Header ─────────────────────────────────────────────────────────

@Composable
private fun GardenSignHeader(points: Int, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(WoodDark, WoodMid, WoodLight, WoodMid)
                )
            )
    ) {
        // Wood grain texture lines
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
        ) {
            for (i in 0..10) {
                val y = size.height * i / 10f
                drawLine(Color(0x18FFFFFF), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.6f)
                drawLine(Color(0x0C000000), Offset(0f, y + 2f), Offset(size.width, y + 2f), strokeWidth = 0.8f)
            }
            for (i in 0..4) {
                val x = size.width * i / 4f
                drawLine(Color(0x14000000), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back + points row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(contentColor = GardenCream)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Back", fontWeight = FontWeight.SemiBold)
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GardenGold,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = GardenText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "$points pts",
                            color = GardenText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Title sign — cream panel sitting on wood
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GardenCream.copy(alpha = 0.90f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocalFlorist,
                                contentDescription = null,
                                tint = GardenBlueDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Garden Center",
                                color = GardenText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                        }
                        Text(
                            "Grow your focus · Earn points · Tend your garden",
                            color = GardenSubtext,
                            fontSize = 12.sp
                        )
                    }

                    // Ladybug decoration on the sign
                    Canvas(modifier = Modifier.size(44.dp)) {
                        drawLadybugInsect(
                            cx     = size.width * 0.50f,
                            cy     = size.height * 0.52f,
                            radius = size.width * 0.28f
                        )
                    }
                }
            }
        }
    }
}

// ─── Growing Bench Card ───────────────────────────────────────────────────────

@Composable
private fun GrowingBenchCard(selected: PlantType, stage: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(GardenBlueSoft, GardenSand)),
                    RoundedCornerShape(24.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // "Currently Growing" pill label
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = GardenAccent.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalFlorist,
                            contentDescription = null,
                            tint = GardenBlueDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Currently Growing",
                            color = GardenBlueDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Wooden bench surface holding the plant
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(WoodDark, WoodMid, WoodLight, WoodMid, WoodDark)
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(3.dp)
                ) {
                    // Plank grain lines
                    Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                        for (i in 0..6) {
                            drawLine(
                                Color(0x18000000),
                                Offset(size.width * i / 6f, 0f),
                                Offset(size.width * i / 6f, size.height),
                                strokeWidth = 1f
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GardenCream.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Plant preview circle
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color.White, GardenBlueSoft, GardenSand)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            PlantPreview(
                                type = selected,
                                stage = stage,
                                fullyUpgradedPreview = false,
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                selected.title,
                                color = GardenText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                            Text(
                                selected.description,
                                color = GardenSubtext,
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GardenTag("Stage $stage / 4", GardenBlueSoft, GardenBlueDark)
                                GardenTag("Growing", GardenAccent.copy(alpha = 0.65f), GardenBlueDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Shelf Divider ────────────────────────────────────────────────────────────

@Composable
private fun ShelfDivider(label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // Top ridge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(
                    Brush.verticalGradient(listOf(WoodLight, WoodMid)),
                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                )
        )
        // Main plank
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    Brush.horizontalGradient(listOf(WoodDark, WoodMid, WoodLight, WoodMid, WoodDark))
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0..7) {
                    drawLine(
                        Color(0x14000000),
                        Offset(size.width * i / 7f, 0f),
                        Offset(size.width * i / 7f, size.height),
                        strokeWidth = 1f
                    )
                }
            }
        }
        // Drop shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0x22000000), Color.Transparent))
                )
        )

        Spacer(Modifier.height(10.dp))

        Text(label, color = GardenText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            when {
                label.contains("Plant") -> "Choose a plant to grow through your focus sessions."
                else -> "Upgrades apply to your currently selected plant."
            },
            color = GardenSubtext,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
    }
}

// ─── Plant Shop Card ──────────────────────────────────────────────────────────

@Composable
private fun PlantShopCard(type: PlantType, cost: Int) {
    val owned    = GardenCenterStore.isOwned(type)
    val selected = GardenCenterStore.selectedPlant == type
    val stageNow = if (owned) GardenCenterStore.stageFor(type) else 0

    val containerBrush = when {
        selected -> Brush.horizontalGradient(listOf(GardenLavender, Color(0xFFF2EAF8)))
        owned    -> Brush.horizontalGradient(listOf(Color(0xFFF7F3ED), Color(0xFFF0ECE5)))
        else     -> Brush.horizontalGradient(listOf(Color(0xFFFAF7F2), Color(0xFFF3EEE6)))
    }

    val borderColor = when {
        selected -> GardenBlueDark.copy(alpha = 0.35f)
        owned    -> GardenTan
        else     -> GardenSand
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerBrush)
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Plant preview with greenhouse-glass sheen
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(Color.White, GardenBlueSoft, GardenSand)
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
                    // Glass sheen overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.20f), Color.Transparent)
                                ),
                                RoundedCornerShape(20.dp)
                            )
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Name row + cost/stage badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                type.title,
                                color = GardenText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                type.description,
                                color = GardenSubtext,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (!owned) {
                            PriceTag(cost)
                        } else {
                            GardenTag("Stage $stageNow", GardenBlueSoft, GardenBlueDark)
                        }
                    }

                    // Bottom row: max preview + action button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Full look" max-stage thumbnail
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.72f)),
                                contentAlignment = Alignment.Center
                            ) {
                                PlantPreview(
                                    type = type,
                                    stage = 4,
                                    fullyUpgradedPreview = true,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Full look",
                                color = GardenSubtext,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Buy / Select button
                        if (!owned) {
                            Button(
                                onClick = { GardenCenterStore.buyPlant(type, cost) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GardenBlue,
                                    contentColor   = Color.White
                                ),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("Buy Plant", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { GardenCenterStore.select(type) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) Color.White.copy(alpha = 0.4f)
                                    else Color.Transparent,
                                    contentColor   = GardenBlueDark
                                ),
                                border = BorderStroke(
                                    1.4.dp,
                                    if (selected) GardenBlueDark.copy(alpha = 0.35f)
                                    else GardenBlue.copy(alpha = 0.30f)
                                ),
                                modifier = Modifier.height(44.dp)
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

// ─── Upgrade Card ─────────────────────────────────────────────────────────────

@Composable
private fun UpgradeCard(
    plantType: PlantType,
    upgrade: PlantUpgrade,
    current: OwnedPlant
) {
    val ownedCount = when (upgrade.id) {
        "growth_boost" -> current.upgradeGrowthBoost
        "speed"        -> current.upgradeSpeed
        "bonus_points" -> current.upgradeBonusPoints
        else           -> 0
    }

    val icon = when (upgrade.id) {
        "growth_boost" -> Icons.Default.LocalFlorist
        "speed"        -> Icons.Default.WaterDrop
        else           -> Icons.Default.Star
    }

    val iconBg = when (upgrade.id) {
        "growth_boost" -> Brush.verticalGradient(listOf(GardenAccent, GardenBlueSoft))
        "speed"        -> Brush.verticalGradient(listOf(GardenBlueSoft, Color(0xFFCDE3EC)))
        else           -> Brush.verticalGradient(listOf(Color(0xFFFAEFBF), Color(0xFFF5E070)))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = GardenCard),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = GardenBlueDark,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Name + description + owned pill
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(upgrade.name, color = GardenText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(upgrade.description, color = GardenSubtext, fontSize = 12.sp)
                GardenTag("Owned: $ownedCount", GardenSand, GardenSubtext)
            }

            // Price + buy button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PriceTag(upgrade.cost)
                Button(
                    onClick = { GardenCenterStore.buyUpgrade(plantType, upgrade) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GardenBlue,
                        contentColor   = Color.White
                    ),
                    modifier = Modifier.height(38.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Text("Buy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ─── Empty upgrade card ───────────────────────────────────────────────────────

@Composable
private fun EmptyUpgradeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = GardenCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🌱", fontSize = 28.sp)
            Text(
                "No plant selected",
                color = GardenText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Select a plant above to view its upgrades.",
                color = GardenSubtext,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Small reusable composables ───────────────────────────────────────────────

@Composable
private fun GardenTag(text: String, bg: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = bg) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PriceTag(cost: Int) {
    Surface(shape = RoundedCornerShape(10.dp), color = GardenGold) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = GardenText,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                "$cost",
                color = GardenText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
    }
}

// ─── Ladybug drawn in Canvas (for header decoration) ─────────────────────────

private fun DrawScope.drawLadybugInsect(cx: Float, cy: Float, radius: Float) {
    val shell = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                cx - radius, cy - radius * 0.8f,
                cx + radius, cy + radius
            )
        )
    }
    drawPath(shell, Color(0xFFD32F2F))
    drawLine(
        Color(0xFF8B0000),
        Offset(cx, cy - radius * 0.8f),
        Offset(cx, cy + radius),
        strokeWidth = radius * 0.16f
    )
    // Head
    drawCircle(Color(0xFF1A1A1A), radius * 0.42f, Offset(cx, cy - radius * 0.90f))
    // Eyes
    drawCircle(Color(0xFFFFFFFF), radius * 0.13f, Offset(cx - radius * 0.17f, cy - radius * 0.98f))
    drawCircle(Color(0xFFFFFFFF), radius * 0.13f, Offset(cx + radius * 0.17f, cy - radius * 0.98f))
    // Spots
    drawCircle(Color(0xFF1A1A1A), radius * 0.20f, Offset(cx - radius * 0.46f, cy - radius * 0.22f))
    drawCircle(Color(0xFF1A1A1A), radius * 0.20f, Offset(cx + radius * 0.46f, cy - radius * 0.22f))
    drawCircle(Color(0xFF1A1A1A), radius * 0.17f, Offset(cx - radius * 0.42f, cy + radius * 0.34f))
    drawCircle(Color(0xFF1A1A1A), radius * 0.17f, Offset(cx + radius * 0.42f, cy + radius * 0.34f))
    // Antennae
    drawLine(
        Color(0xFF1A1A1A),
        Offset(cx - radius * 0.10f, cy - radius * 1.24f),
        Offset(cx - radius * 0.36f, cy - radius * 1.62f),
        strokeWidth = radius * 0.10f,
        cap = StrokeCap.Round
    )
    drawLine(
        Color(0xFF1A1A1A),
        Offset(cx + radius * 0.10f, cy - radius * 1.24f),
        Offset(cx + radius * 0.36f, cy - radius * 1.62f),
        strokeWidth = radius * 0.10f,
        cap = StrokeCap.Round
    )
    drawCircle(Color(0xFF1A1A1A), radius * 0.12f, Offset(cx - radius * 0.38f, cy - radius * 1.66f))
    drawCircle(Color(0xFF1A1A1A), radius * 0.12f, Offset(cx + radius * 0.38f, cy - radius * 1.66f))
}