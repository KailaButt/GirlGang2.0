package com.example.consolicalm

import androidx.compose.runtime.*

enum class PlantType(val title: String, val description: String) {
    JUNIPER_BONSAI("Juniper Bonsai", "Windswept classic. Slow + satisfying growth."),
    REGULAR_TREE("Mountain Pine", "A hardy pine that looks best in the snow."),
    FLOWER("Flower", "Bright and rewarding. Blooms fast."),
    CACTUS("Cactus", "Low maintenance. Tough and simple."),
    CHERRY_BLOSSOM("Cherry Blossom", "Elegant. Unlock blossoms as you level it.")
}

data class PlantUpgrade(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int
)

data class OwnedPlant(
    val type: PlantType,
    val sessionsGrown: Int = 0,
    val upgradeGrowthBoost: Int = 0,
    val upgradeSpeed: Int = 0,
    val upgradeBonusPoints: Int = 0
)

object GardenCenterStore {
    var points by mutableIntStateOf(0)
        private set

    var owned by mutableStateOf(
        mapOf(
            PlantType.JUNIPER_BONSAI to OwnedPlant(PlantType.JUNIPER_BONSAI)
        )
    )
        private set

    var selectedPlant by mutableStateOf(PlantType.JUNIPER_BONSAI)
        private set

    fun addPoints(amount: Int) {
        points = (points + amount).coerceAtLeast(0)
    }

    fun spendPoints(amount: Int): Boolean {
        if (points < amount) return false
        points -= amount
        return true
    }

    fun isOwned(type: PlantType): Boolean = owned.containsKey(type)

    fun select(type: PlantType) {
        if (isOwned(type)) selectedPlant = type
    }

    fun buyPlant(type: PlantType, cost: Int): Boolean {
        if (isOwned(type)) return true
        if (!spendPoints(cost)) return false
        owned = owned + (type to OwnedPlant(type))
        selectedPlant = type
        return true
    }

    fun onFocusSessionComplete(basePoints: Int) {
        val current = owned[selectedPlant] ?: return
        val bonus = current.upgradeBonusPoints
        addPoints(basePoints + bonus)
        owned = owned + (selectedPlant to current.copy(sessionsGrown = current.sessionsGrown + 1))
    }

    fun getPlant(type: PlantType): OwnedPlant? = owned[type]

    fun buyUpgrade(type: PlantType, upgrade: PlantUpgrade): Boolean {
        val current = owned[type] ?: return false
        if (!spendPoints(upgrade.cost)) return false

        val updated = when (upgrade.id) {
            "growth_boost" -> current.copy(upgradeGrowthBoost = current.upgradeGrowthBoost + 1)
            "speed" -> current.copy(upgradeSpeed = current.upgradeSpeed + 1)
            "bonus_points" -> current.copy(upgradeBonusPoints = current.upgradeBonusPoints + 1)
            else -> current
        }

        owned = owned + (type to updated)
        return true
    }

    fun upgradesFor(type: PlantType): List<PlantUpgrade> {
        return listOf(
            PlantUpgrade("growth_boost", "Growth Boost", "Adds +1 visual growth stage.", cost = 60),
            PlantUpgrade("speed", "Faster Growth", "Needs fewer sessions per stage.", cost = 80),
            PlantUpgrade("bonus_points", "Bonus Points", "+1 extra point per completed focus session.", cost = 120)
        )
    }

    fun stageFor(type: PlantType): Int {
        val p = owned[type] ?: return 0
        val baseSessionsPerStage = 2
        val speedBonus = p.upgradeSpeed.coerceIn(0, 3)
        val sessionsPerStage = (baseSessionsPerStage - speedBonus).coerceAtLeast(1)

        val rawStage = (p.sessionsGrown / sessionsPerStage) + p.upgradeGrowthBoost
        return rawStage.coerceIn(0, 4)
    }

    fun resetAll() {
        points = 0
        owned = mapOf(PlantType.JUNIPER_BONSAI to OwnedPlant(PlantType.JUNIPER_BONSAI))
        selectedPlant = PlantType.JUNIPER_BONSAI
    }
}

