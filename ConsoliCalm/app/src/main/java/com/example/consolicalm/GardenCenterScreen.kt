package com.example.consolicalm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GardenCenterScreen(
    onBackToStudy: () -> Unit
) {
    val points = GardenCenterStore.points
    val selected = GardenCenterStore.selectedPlant
    val owned = GardenCenterStore.owned
    val scroll = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(18.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ✅ Top row with Back + Points
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackToStudy) { Text("Back") }
            AssistChip(onClick = {}, label = { Text("⭐ $points") })
        }

        Text("Garden Center", style = MaterialTheme.typography.headlineSmall)

        Text(
            "Spend points on new plants and upgrades. Your selected plant grows when you finish focus sessions.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Currently growing", style = MaterialTheme.typography.titleMedium)
                Text("${selected.title} • Stage ${GardenCenterStore.stageFor(selected)} / 4")
                Text(selected.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text("Plants", style = MaterialTheme.typography.titleMedium)
        Text("Right side shows the fully-grown + fully-upgraded look.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        PlantShopCard(PlantType.JUNIPER_BONSAI, cost = 0)
        PlantShopCard(PlantType.REGULAR_TREE, cost = 120)
        PlantShopCard(PlantType.FLOWER, cost = 90)
        PlantShopCard(PlantType.CACTUS, cost = 110)
        PlantShopCard(PlantType.CHERRY_BLOSSOM, cost = 180)

        Spacer(Modifier.height(6.dp))

        Text("Upgrades", style = MaterialTheme.typography.titleMedium)
        Text("Upgrades apply to the selected plant.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        val selectedPlantData = owned[selected]
        if (selectedPlantData == null) {
            Text("Select a plant first.")
        } else {
            GardenCenterStore.upgradesFor(selected).forEach { up ->
                UpgradeCard(plantType = selected, upgrade = up, current = selectedPlantData)
            }
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBackToStudy) { Text("Back to Study") }
    }
}

@Composable
private fun PlantShopCard(type: PlantType, cost: Int) {
    val owned = GardenCenterStore.isOwned(type)
    val selected = GardenCenterStore.selectedPlant == type
    val stageNow = if (owned) GardenCenterStore.stageFor(type) else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(type.title, style = MaterialTheme.typography.titleMedium)
                    AssistChip(onClick = {}, label = { Text("Stage $stageNow") })
                }

                Text(type.description, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!owned) {
                        Text("Cost: ⭐ $cost", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { GardenCenterStore.buyPlant(type, cost) }) { Text("Buy") }
                    } else {
                        Text(if (selected) "Selected" else "Owned", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { GardenCenterStore.select(type) }) {
                            Text(if (selected) "Selected" else "Select")
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlantPreview(
                    type = type,
                    stage = 4,
                    fullyUpgradedPreview = true,
                    modifier = Modifier.size(86.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Fully upgraded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    plantType: PlantType,
    upgrade: PlantUpgrade,
    current: OwnedPlant
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(upgrade.name, style = MaterialTheme.typography.titleMedium)
                Text("⭐ ${upgrade.cost}")
            }

            Text(upgrade.description, color = MaterialTheme.colorScheme.onSurfaceVariant)

            val extra = when (upgrade.id) {
                "growth_boost" -> "Owned: ${current.upgradeGrowthBoost}"
                "speed" -> "Owned: ${current.upgradeSpeed}"
                "bonus_points" -> "Owned: ${current.upgradeBonusPoints}"
                else -> ""
            }
            if (extra.isNotBlank()) Text(extra, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = { GardenCenterStore.buyUpgrade(plantType, upgrade) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Buy upgrade")
            }
        }
    }
}

