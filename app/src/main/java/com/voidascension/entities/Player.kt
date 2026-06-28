package com.voidascension.entities

import com.voidascension.engine.*
import com.voidascension.utils.Vector2
import kotlin.math.max
import kotlin.math.min

data class CyberImplant(
    val id: String,
    val name: String,
    val description: String,
    val hpBonus: Float = 0f,
    val damageBonus: Float = 0f,
    val speedBonus: Float = 0f,
    val fireRateBonus: Float = 0f,
    val auraBonus: Float = 0f,
    val xpMultiplier: Float = 1f,
    val lifeSteal: Float = 0f,
    val critChance: Float = 0f,
    val critMultiplier: Float = 1f
)

data class ActiveAura(
    val type: AuraType,
    var level: Int = 1,
    var lastTickMs: Long = 0L
)

interface TimedBuff {
    val name: String
    val expiryTimeMs: Long
    fun onApply(player: Player) {}
    fun onExpire(player: Player) {}
}

class Player(
    var position: Vector2 = Vector2(),
    val radius: Float = GameConstants.PLAYER_RADIUS
) {
    // Core Stats
    var maxHp: Float = GameConstants.PLAYER_BASE_HP
    var currentHp: Float = maxHp
    var baseSpeed: Float = GameConstants.PLAYER_BASE_SPEED
    var baseDamage: Float = GameConstants.PLAYER_BASE_DAMAGE
    var baseFireRate: Float = GameConstants.PLAYER_BASE_FIRE_RATE // shots/sec

    // Level & XP
    var level: Int = 1
    var currentXp: Int = 0
    var xpToNext: Int = GameConstants.XP_BASE

    // Multipliers (from mutations/implants)
    var damageMultiplier: Float = 1f
    var speedMultiplier: Float = 1f
    var hpMultiplier: Float = 1f
    var fireRateMultiplier: Float = 1f
    var xpMultiplier: Float = 1f
    var auraMultiplier: Float = 1f
    var bossDamageMultiplier: Float = 1f
    var critChance: Float = 0.05f
    var critMultiplier: Float = 1.75f
    var lifeSteal: Float = 0f
    var projectileCount: Int = 1
    var projectilePierce: Int = 0
    var regenRate: Float = 0f // HP per second

    // Epic Upgrade Flags
    var isOneShotKill: Boolean = false
    var isLootMagnet: Boolean = false
    var extraLives: Int = 0
    var isEnemiesConfused: Boolean = false
    var isHomingShots: Boolean = false
    var hasUltimateAura: Boolean = false
    var hasTwins: Boolean = false
    var hasShockwave: Boolean = false
    var hasMauragSupremacy: Boolean = false

    var twin1Pos: Vector2 = Vector2()
    var twin2Pos: Vector2 = Vector2()
    
    // Cheat Flags
    var isAikiActive: Boolean = false
    var isMochiMochiActive: Boolean = false
    var isMotomemashitaActive: Boolean = false

    // Timed Buffs
    private val timedBuffs = mutableListOf<TimedBuff>()
    val activeBuffs: List<TimedBuff> get() = synchronized(timedBuffs) { ArrayList(timedBuffs) }

    // State
    var velocity: Vector2 = Vector2()
    var isAlive: Boolean = true
    var lastDamageTimeMs: Long = 0L
    var isInvincible: Boolean = false
    var score: Int = 0
    var killCount: Int = 0
    var waveNumber: Int = 1
    var collectedVoidShards: Int = 0

    // Equipment
    var weaponType: WeaponType = WeaponType.PLASMA_RIFLE
    val mutations = java.util.Collections.synchronizedList(mutableListOf<MutationType>())
    val cyberImplants = java.util.Collections.synchronizedList(mutableListOf<CyberImplant>())
    val auras = java.util.Collections.synchronizedList(mutableListOf<ActiveAura>())

    // Computed stats
    val effectiveDamage get() = baseDamage * damageMultiplier
    val effectiveSpeed get() = baseSpeed * speedMultiplier
    val effectiveFireRate get() = baseFireRate * fireRateMultiplier

    fun update(dt: Float, currentTimeMs: Long) {
        // Handle timed buffs
        synchronized(timedBuffs) {
            val iterator = timedBuffs.iterator()
            while (iterator.hasNext()) {
                val buff = iterator.next()
                if (currentTimeMs >= buff.expiryTimeMs) {
                    buff.onExpire(this)
                    iterator.remove()
                }
            }
        }

        // Apply regeneration
        if (regenRate > 0 && currentHp < maxHp) {
            heal(regenRate * dt)
        }
    }

    fun addTimedBuff(buff: TimedBuff) {
        synchronized(timedBuffs) {
            buff.onApply(this)
            timedBuffs.add(buff)
        }
    }

    fun applyPermanentUpgrades(upgrades: List<com.voidascension.data.PermanentUpgrade>) {
        for (upg in upgrades) {
            when (upg.id) {
                "start_hp"   -> {
                    val bonus = upg.level * 20f
                    maxHp += bonus
                    currentHp += bonus
                }
                "start_dmg"  -> baseDamage *= (1f + upg.level * 0.1f)
                "start_spd"  -> baseSpeed *= (1f + upg.level * 0.1f)
                "xp_bonus"   -> xpMultiplier += upg.level * 0.2f
                "boss_dmg"   -> bossDamageMultiplier += upg.level * 0.15f
                "life_steal" -> lifeSteal += upg.level * 0.02f
                "crit_start" -> critChance += upg.level * 0.05f
                "fire_rate"  -> baseFireRate *= (1f + upg.level * 0.1f)
                "extra_proj" -> projectileCount += upg.level // Lv1 gives +1, Lv2 gives another +1
                
                // Epics
                "epic_kokey" -> if (upg.level > 0) isOneShotKill = true
                "epic_kekay" -> if (upg.level > 0) isLootMagnet = true
                "epic_umamay" -> if (upg.level > 0) extraLives += 1
                "epic_kokoy" -> if (upg.level > 0) isEnemiesConfused = true
                "epic_sukhoi" -> if (upg.level > 0) isHomingShots = true
                "epic_king_cone" -> if (upg.level > 0) {
                    hasUltimateAura = true
                    auraMultiplier *= 2.0f
                }
                "epic_bebot" -> if (upg.level > 0) hasTwins = true
                "epic_korokoy" -> if (upg.level > 0) hasShockwave = true
                "epic_maurag" -> if (upg.level > 0) {
                    hasMauragSupremacy = true
                    // Apply all mutations
                    MutationType.entries.forEach { applyMutation(it) }
                }
            }
        }
    }

    fun takeDamage(amount: Float, currentTimeMs: Long): Float {
        if (isInvincible || !isAlive) return 0f
        val actual = max(0f, amount)
        currentHp -= actual
        lastDamageTimeMs = currentTimeMs
        isInvincible = true
        if (currentHp <= 0f) {
            currentHp = 0f
            isAlive = false
        }
        return actual
    }

    fun heal(amount: Float) {
        currentHp = min(maxHp, currentHp + amount)
    }

    fun addXp(xp: Int): Boolean {
        val gained = (xp * xpMultiplier).toInt()
        currentXp += gained
        score += gained * 10
        if (currentXp >= xpToNext && level < GameConstants.MAX_LEVEL) {
            currentXp -= xpToNext
            level++
            xpToNext = (xpToNext * GameConstants.XP_MULTIPLIER).toInt()
            return true // level up
        }
        return false
    }

    fun xpPercent() = if (xpToNext > 0) currentXp.toFloat() / xpToNext else 0f
    fun hpPercent() = if (maxHp > 0) currentHp / maxHp else 0f

    fun applyImplant(implant: CyberImplant) {
        cyberImplants.add(implant)
        maxHp += implant.hpBonus
        currentHp += implant.hpBonus
        baseDamage += implant.damageBonus
        baseSpeed += implant.speedBonus
        baseFireRate += implant.fireRateBonus
        auraMultiplier += implant.auraBonus
        xpMultiplier *= implant.xpMultiplier
        lifeSteal += implant.lifeSteal
        critChance = min(0.95f, critChance + implant.critChance)
        critMultiplier += (implant.critMultiplier - 1f)
    }

    fun applyMutation(mutation: MutationType) {
        mutations.add(mutation)
        when (mutation) {
            MutationType.IRON_SKIN       -> { maxHp += 40f; currentHp += 40f; hpMultiplier += 0.1f }
            MutationType.NEURAL_BOOST    -> { fireRateMultiplier += 0.3f; damageMultiplier += 0.15f }
            MutationType.VOID_STEP       -> speedMultiplier += 0.25f
            MutationType.QUANTUM_HEART   -> { maxHp += 25f; lifeSteal += 0.05f }
            MutationType.CYBER_REFLEXES  -> critChance = min(0.95f, critChance + 0.1f)
            MutationType.PLASMA_VEINS    -> { damageMultiplier += 0.2f; auraMultiplier += 0.2f }
            MutationType.PHASE_SHIFT     -> { speedMultiplier += 0.15f; projectilePierce++ }
            MutationType.ENTROPY_TOUCH   -> damageMultiplier += 0.3f
            MutationType.COSMIC_SIGHT    -> xpMultiplier *= 1.3f
            MutationType.TWIN_BARRELS    -> projectileCount++
            MutationType.EXPLOSIVE_ROUNDS-> damageMultiplier += 0.25f
            MutationType.LIFE_STEAL      -> lifeSteal += 0.08f
        }
    }

    fun addAura(type: AuraType) {
        synchronized(auras) {
            val existing = auras.find { it.type == type }
            if (existing != null) existing.level++ else auras.add(ActiveAura(type))
        }
    }

    fun checkInvincibility(currentTimeMs: Long) {
        if (isInvincible && (currentTimeMs - lastDamageTimeMs) > GameConstants.PLAYER_INVINCIBILITY_MS) {
            isInvincible = false
        }
    }

    fun reset() {
        maxHp = GameConstants.PLAYER_BASE_HP
        currentHp = maxHp
        baseSpeed = GameConstants.PLAYER_BASE_SPEED
        baseDamage = GameConstants.PLAYER_BASE_DAMAGE
        baseFireRate = GameConstants.PLAYER_BASE_FIRE_RATE
        damageMultiplier = 1f; speedMultiplier = 1f
        hpMultiplier = 1f; fireRateMultiplier = 1f
        xpMultiplier = 1f; auraMultiplier = 1f
        critChance = 0.05f; critMultiplier = 1.75f
        lifeSteal = 0f; projectileCount = 1; projectilePierce = 0
        level = 1; currentXp = 0; xpToNext = GameConstants.XP_BASE
        score = 0; killCount = 0; waveNumber = 1
        isAlive = true; isInvincible = false
        weaponType = WeaponType.PLASMA_RIFLE
        mutations.clear(); cyberImplants.clear(); auras.clear()
        velocity.set(0f, 0f)
    }
}
