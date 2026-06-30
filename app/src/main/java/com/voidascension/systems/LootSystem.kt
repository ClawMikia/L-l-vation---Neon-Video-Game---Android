package com.voidascension.systems

import com.voidascension.engine.*
import com.voidascension.entities.*
import com.voidascension.utils.Vector2
import kotlin.random.Random

data class LootTable(
    val items: List<LootTableEntry>
)

data class LootTableEntry(
    val name: String,
    val description: String,
    val type: LootItemType,
    val rarity: LootRarity,
    val value: Float = 0f,
    val weight: Float = 1f
)

class LootSystem {
    private var idCounter = 0L

    private val lootTables = mapOf(
        LootRarity.COMMON to LootTable(listOf(
            LootTableEntry("Nano-Medkit", "+30 HP", LootItemType.HEALTH_PACK, LootRarity.COMMON, 30f, 3f),
            LootTableEntry("XP Shard", "+50 XP", LootItemType.XP_SHARD, LootRarity.COMMON, 50f, 3f),
            LootTableEntry("Void Crystal", "+75 XP", LootItemType.XP_SHARD, LootRarity.COMMON, 75f, 2f),
            LootTableEntry("Plasma Cell", "+10% fire rate", LootItemType.WEAPON_UPGRADE, LootRarity.COMMON, 0.1f, 2f)
        )),
        LootRarity.UNCOMMON to LootTable(listOf(
            LootTableEntry("Neural Chip", "+15% damage", LootItemType.WEAPON_UPGRADE, LootRarity.UNCOMMON, 0.15f, 2f),
            LootTableEntry("Speed Booster", "+20% speed", LootItemType.WEAPON_UPGRADE, LootRarity.UNCOMMON, 0.20f, 2f),
            LootTableEntry("Bio-Stim", "+60 HP + regen", LootItemType.HEALTH_PACK, LootRarity.UNCOMMON, 60f, 1.5f),
            LootTableEntry("Mutation Seed", "Random mutation", LootItemType.MUTATION, LootRarity.UNCOMMON, 0f, 2f),
            LootTableEntry("Void Capacitor", "Score x1.5", LootItemType.SCORE_MULTIPLIER, LootRarity.UNCOMMON, 1.5f, 1f)
        )),
        LootRarity.RARE to LootTable(listOf(
            LootTableEntry("Cyber Lens", "+10% crit chance", LootItemType.CYBER_IMPLANT, LootRarity.RARE, 0.1f, 2f),
            LootTableEntry("Phase Emitter", "Unlock Phase Aura", LootItemType.AURA_UNLOCK, LootRarity.RARE, 0f, 1.5f),
            LootTableEntry("Void Core", "+25% damage, +15% speed", LootItemType.CYBER_IMPLANT, LootRarity.RARE, 0f, 2f),
            LootTableEntry("Void Shard Bundle", "5 Void Shards", LootItemType.VOID_SHARD, LootRarity.RARE, 5f, 1.5f),
            LootTableEntry("Twin Barrel Mod", "+1 projectile", LootItemType.WEAPON_UPGRADE, LootRarity.RARE, 0f, 1f)
        )),
        LootRarity.EPIC to LootTable(listOf(
            LootTableEntry("Quantum Heart", "+100 HP, +8% life steal", LootItemType.CYBER_IMPLANT, LootRarity.EPIC, 0f, 1.5f),
            LootTableEntry("Entropy Module", "+40% damage, pierce +1", LootItemType.CYBER_IMPLANT, LootRarity.EPIC, 0f, 1f),
            LootTableEntry("Void Shard Cache", "15 Void Shards", LootItemType.VOID_SHARD, LootRarity.EPIC, 15f, 1f),
            LootTableEntry("Neural Overclock", "+50% fire rate, +20% dmg", LootItemType.CYBER_IMPLANT, LootRarity.EPIC, 0f, 1f),
            LootTableEntry("Cosmic Mutation", "Elite random mutation", LootItemType.MUTATION, LootRarity.EPIC, 0f, 1.5f)
        )),
        LootRarity.LEGENDARY to LootTable(listOf(
            LootTableEntry("Singularity Core", "+100% damage, +1 pierce", LootItemType.CYBER_IMPLANT, LootRarity.LEGENDARY, 0f, 1f),
            LootTableEntry("Void God Fragment", "All stats +25%", LootItemType.CYBER_IMPLANT, LootRarity.LEGENDARY, 0f, 0.8f),
            LootTableEntry("Eldritch Mutation", "3 random mutations", LootItemType.MUTATION, LootRarity.LEGENDARY, 0f, 1f),
            LootTableEntry("Cosmic Aura Nexus", "Unlock all auras lvl2", LootItemType.AURA_UNLOCK, LootRarity.LEGENDARY, 0f, 0.6f),
            LootTableEntry("Reality Anchor", "+200 HP, full heal, +20% all", LootItemType.CYBER_IMPLANT, LootRarity.LEGENDARY, 0f, 0.6f)
        ))
    )

    fun rollVoidShard(position: Vector2, currentTimeMs: Long, value: Float): LootItem {
        return LootItem(
            id = idCounter++,
            position = position.copy(),
            rarity = if (value >= 10f) LootRarity.RARE else LootRarity.COMMON,
            name = "Void Shard",
            description = "+${value.toInt()} Shards",
            type = LootItemType.VOID_SHARD,
            value = value,
            spawnTimeMs = currentTimeMs
        )
    }

    fun rollLoot(position: Vector2, currentTimeMs: Long, isBoss: Boolean = false): LootItem? {
        val rarity = rollRarity(isBoss)
        val table = lootTables[rarity] ?: return null
        val entry = weightedRandom(table.items) ?: return null
        return LootItem(
            id = idCounter++,
            position = position.copy(),
            rarity = rarity,
            name = entry.name,
            description = entry.description,
            type = entry.type,
            value = entry.value,
            spawnTimeMs = currentTimeMs
        )
    }

    fun rollMultipleLoot(position: Vector2, currentTimeMs: Long, count: Int = 3): List<LootItem> {
        return (0 until count).mapNotNull {
            val offset = Vector2(
                (Random.nextFloat() - 0.5f) * 100f,
                (Random.nextFloat() - 0.5f) * 100f
            )
            rollLoot(Vector2(position.x + offset.x, position.y + offset.y), currentTimeMs, true)
        }
    }

    private fun rollRarity(isBoss: Boolean): LootRarity {
        val rates = if (isBoss) {
            mapOf(
                LootRarity.COMMON    to 0.15f,
                LootRarity.UNCOMMON  to 0.25f,
                LootRarity.RARE      to 0.30f,
                LootRarity.EPIC      to 0.20f,
                LootRarity.LEGENDARY to 0.10f
            )
        } else GameConstants.LOOT_DROP_RATES

        val roll = Random.nextFloat()
        var cumulative = 0f
        for ((rarity, rate) in rates) {
            cumulative += rate
            if (roll <= cumulative) return rarity
        }
        return LootRarity.COMMON
    }

    private fun <T : LootTableEntry> weightedRandom(items: List<T>): T? {
        if (items.isEmpty()) return null
        val totalWeight = items.sumOf { it.weight.toDouble() }.toFloat()
        val roll = Random.nextFloat() * totalWeight
        var cumulative = 0f
        for (item in items) {
            cumulative += item.weight
            if (roll <= cumulative) return item
        }
        return items.last()
    }

    fun generateUpgradeChoices(player: com.voidascension.entities.Player, wave: Int): List<UpgradeOption> {
        val allOptions = mutableListOf<UpgradeOption>()

        // Add mutation choices - filter out ones player already has
        MutationType.entries.filter { mutation -> 
            !player.mutations.contains(mutation) 
        }.forEach { mutation ->
            allOptions.add(UpgradeOption(
                type = UpgradeType.MUTATION,
                mutationType = mutation,
                name = getMutationName(mutation),
                description = getMutationDesc(mutation),
                rarity = getMutationRarity(mutation, wave)
            ))
        }

        // Add aura choices - filter out ones player already has at max level or just any level?
        // Let's assume one-time for now as requested
        AuraType.entries.filter { aura ->
            player.auras.none { it.type == aura }
        }.forEach { aura ->
            allOptions.add(UpgradeOption(
                type = UpgradeType.AURA,
                auraType = aura,
                name = getAuraName(aura),
                description = getAuraDesc(aura),
                rarity = LootRarity.RARE
            ))
        }

        // Always offer heal
        allOptions.add(UpgradeOption(
            type = UpgradeType.HEAL,
            name = "Emergency Repair",
            description = "Restore 40% max HP",
            rarity = LootRarity.COMMON
        ))

        // New: Regeneration Buff for 2 minutes
        allOptions.add(UpgradeOption(
            type = UpgradeType.BUFF,
            name = "Xeno-Pulse Rejuvenator",
            description = "Regenerate 5 HP/sec for 120 seconds",
            rarity = LootRarity.EPIC
        ))

        // Shuffle and return 3
        return allOptions.shuffled().take(3)
    }

    private fun getMutationRarity(type: MutationType, wave: Int): LootRarity = when (type) {
        MutationType.TWIN_BARRELS, MutationType.EXPLOSIVE_ROUNDS,
        MutationType.ENTROPY_TOUCH -> if (wave > 10) LootRarity.EPIC else LootRarity.RARE
        MutationType.LIFE_STEAL, MutationType.PHASE_SHIFT -> LootRarity.RARE
        else -> LootRarity.UNCOMMON
    }

    private fun getMutationName(type: MutationType) = when (type) {
        MutationType.IRON_SKIN       -> "Iron Skin"
        MutationType.NEURAL_BOOST    -> "Neural Boost"
        MutationType.VOID_STEP       -> "Void Step"
        MutationType.QUANTUM_HEART   -> "Quantum Heart"
        MutationType.CYBER_REFLEXES  -> "Cyber Reflexes"
        MutationType.PLASMA_VEINS    -> "Plasma Veins"
        MutationType.PHASE_SHIFT     -> "Phase Shift"
        MutationType.ENTROPY_TOUCH   -> "Null-State Resonance"
        MutationType.COSMIC_SIGHT    -> "Cosmic Sight"
        MutationType.TWIN_BARRELS    -> "Bifurcated Void-Reaper"
        MutationType.EXPLOSIVE_ROUNDS-> "Volatile Matter Ejector"
        MutationType.LIFE_STEAL      -> "Life Steal"
    }

    private fun getMutationDesc(type: MutationType) = when (type) {
        MutationType.IRON_SKIN       -> "+40 HP, +10% max HP"
        MutationType.NEURAL_BOOST    -> "+30% fire rate, +15% damage"
        MutationType.VOID_STEP       -> "+25% movement speed"
        MutationType.QUANTUM_HEART   -> "+25 HP, +5% life steal"
        MutationType.CYBER_REFLEXES  -> "+10% critical hit chance"
        MutationType.PLASMA_VEINS    -> "+20% damage, +20% aura power"
        MutationType.PHASE_SHIFT     -> "+15% speed, projectiles pierce +1"
        MutationType.ENTROPY_TOUCH   -> "+30% damage to all attacks"
        MutationType.COSMIC_SIGHT    -> "+30% XP gain from all sources"
        MutationType.TWIN_BARRELS    -> "Fire an additional projectile"
        MutationType.EXPLOSIVE_ROUNDS-> "+25% damage, explosive projectiles"
        MutationType.LIFE_STEAL      -> "+8% life steal on hit"
    }

    private fun getAuraName(type: AuraType) = when (type) {
        AuraType.PLASMA_SHIELD    -> "Plasma Shield"
        AuraType.VOID_DRAIN       -> "Void Drain"
        AuraType.STATIC_FIELD     -> "Static Field"
        AuraType.QUANTUM_ECHO     -> "Quantum Echo"
        AuraType.ENTROPIC_PULSE   -> "Entropic Pulse"
        AuraType.NEUROSTATIC_WAVE -> "Neurostatic Wave"
        AuraType.REGEN_CORE       -> "Regen Core"
        AuraType.GRAVITY_FIELD    -> "Gravity Field"
        AuraType.BLAZING_AURA     -> "Blazing Aura"
        AuraType.HOLY_BARRIER     -> "Holy Barrier"
        AuraType.FROST_AURA       -> "Frost Aura"
        AuraType.CHAIN_LIGHTNING  -> "Chain Lightning"
        AuraType.CORROSIVE_AURA   -> "Corrosive Aura"
        AuraType.TITAN_SLAYER     -> "Titan Slayer"
    }

    private fun getAuraDesc(type: AuraType) = when (type) {
        AuraType.PLASMA_SHIELD    -> "Absorb damage periodically"
        AuraType.VOID_DRAIN       -> "Drain HP from nearby enemies"
        AuraType.STATIC_FIELD     -> "Slow and damage close enemies"
        AuraType.QUANTUM_ECHO     -> "Echo 25% of damage dealt"
        AuraType.ENTROPIC_PULSE   -> "Burst damage pulse every 3s"
        AuraType.NEUROSTATIC_WAVE -> "Stun nearby enemies briefly"
        AuraType.REGEN_CORE       -> "Passive health regeneration"
        AuraType.GRAVITY_FIELD    -> "Pulls and slows enemies"
        AuraType.BLAZING_AURA     -> "Burns nearby enemies"
        AuraType.HOLY_BARRIER     -> "Damage reduction and minor heal"
        AuraType.FROST_AURA       -> "Attacks slow enemies"
        AuraType.CHAIN_LIGHTNING  -> "Attacks have chance to chain"
        AuraType.CORROSIVE_AURA   -> "Attacks deal bonus damage"
        AuraType.TITAN_SLAYER     -> "Bonus damage against bosses"
    }
}

data class UpgradeOption(
    val type: UpgradeType,
    val name: String,
    val description: String,
    val rarity: LootRarity,
    val mutationType: MutationType? = null,
    val auraType: AuraType? = null,
    val implantId: String? = null
)

enum class UpgradeType {
    MUTATION, AURA, CYBER_IMPLANT, HEAL, WEAPON_UPGRADE, BUFF
}
