package com.voidascension.entities

import com.voidascension.engine.*
import com.voidascension.utils.Vector2
import kotlin.math.*
import kotlin.random.Random

enum class EnemyAIState {
    SPAWNING, CHASING, ATTACKING, CIRCLING,
    FLEEING, TELEPORTING, SUMMONING, ENRAGED
}

data class EnemyAbility(
    val id: String,
    val cooldownMs: Long,
    var lastUsedMs: Long = 0L
)

class Enemy(
    val type: EnemyType,
    val isBoss: Boolean = false,
    val wave: Int = 1,
    startPos: Vector2 = Vector2(),
    val isMainBoss: Boolean = false
) {
    var position: Vector2 = startPos.copy()
    var radius: Float = if (isBoss) GameConstants.ENEMY_RADIUS * 2.4f else GameConstants.ENEMY_RADIUS

    // Stats scaled by wave
    val waveScale get() = 1f + (wave - 1) * (GameConstants.ENEMY_HP_SCALE - 1f)
    var maxHp: Float = calculateMaxHp()
    var currentHp: Float = maxHp
    var damage: Float = calculateDamage()
    var speed: Float = calculateSpeed()
    val xpValue: Int = calculateXp()
    val lootChance: Float = if (isBoss) 1f else calculateLootChance()

    // Vulnerability: Main bosses become significantly easier to damage in Phase 2 as waves progress
    val vulnerability: Float get() {
        var baseV = if (isMainBoss) 1f + (wave * 0.03f) else 1f
        if (isBoss && phase >= 2) {
            // "Bigger opponent" phase is even more vulnerable
            baseV *= (1.5f + (wave * 0.02f))
        }
        return baseV
    }

    var isAlive: Boolean = true
    var rewardsProcessed: Boolean = false
    var velocity: Vector2 = Vector2()
    var aiState: EnemyAIState = EnemyAIState.SPAWNING
    var spawnProgress: Float = 0f // 0..1 spawn animation
    var lastAttackMs: Long = 0L
    var attackCooldownMs: Long = calculateAttackCooldown()
    var enrageThreshold: Float = 0.3f // HP% at which to enrage
    var isEnraged: Boolean = false

    // Boss-specific
    var phase: Int = 1
    var summonCooldownMs: Long = 5000L
    var lastSummonMs: Long = 0L
    val abilities: MutableList<EnemyAbility> = mutableListOf()

    // Targeting
    var targetPosition: Vector2 = Vector2()
    var circleAngle: Float = Random.nextFloat() * 2f * PI.toFloat()
    var circleRadius: Float = 120f + Random.nextFloat() * 80f
    var teleportCooldownMs: Long = 3000L
    var lastTeleportMs: Long = 0L

    // Visual
    var glowIntensity: Float = if (isBoss) 1f else 0.5f
    var rotationAngle: Float = 0f

    init {
        if (isBoss) initBossAbilities()
    }

    private fun calculateMaxHp(): Float {
        val base = when (type) {
            EnemyType.VOID_CRAWLER      -> 25f
            EnemyType.COSMIC_SHADE      -> 35f
            EnemyType.NEBULA_WRAITH     -> 50f
            EnemyType.STAR_DEVOURER     -> 80f
            EnemyType.RIFT_STALKER      -> 60f
            EnemyType.VOID_TITAN        -> 250f
            EnemyType.COSMIC_GOD        -> 900f
            EnemyType.ENTROPY_HERALD    -> 600f
            EnemyType.SINGULARITY_SPAWN -> 150f
            EnemyType.ABYSS_KNIGHT      -> 180f
            EnemyType.PHASE_PHANTOM     -> 55f
            EnemyType.ELDRITCH_HORROR   -> 1500f
            // New Enemies
            EnemyType.GRAVITY_WELLER    -> 100f
            EnemyType.SHADOW_STALKER    -> 70f
            EnemyType.PLASMA_REAPAR     -> 120f
            EnemyType.NEURAL_HIVE       -> 200f
            EnemyType.DIMENSION_RIPPER  -> 110f
            EnemyType.QUARK_GLUTTON     -> 300f
            EnemyType.PHOTON_WRAITH     -> 65f
            EnemyType.CHRONO_EATER      -> 140f
            EnemyType.PULSE_WITCH       -> 90f
            EnemyType.ANTIMATTER_CORE    -> 400f
            EnemyType.NEUTRON_BEAST     -> 350f
            EnemyType.SOLAR_FLARE       -> 130f
            EnemyType.VOID_EEL          -> 80f
            EnemyType.COMET_CRASH       -> 160f
            EnemyType.ASTEROID_GOLEM    -> 500f
            EnemyType.GALAXY_EATER      -> 450f
            EnemyType.NEBULA_DRAGON     -> 550f
            EnemyType.DARK_ENERGY_WISP  -> 40f
            EnemyType.QUANTUM_GOLEM     -> 600f
            EnemyType.SINGULARITY_SEED   -> 30f
            // New Bosses
            EnemyType.PRIME_SINGULARITY -> 2500f
            EnemyType.NEBULA_QUEEN      -> 2000f
            EnemyType.VOID_ARCHON       -> 2200f
            EnemyType.STAR_EATER_TITAN  -> 3500f
            EnemyType.CHRONOS_LORD      -> 1800f
            EnemyType.DIMENSIONAL_GOD   -> 4000f
            EnemyType.OMEGA_PHANTOM     -> 1500f
            EnemyType.GALAXY_TITAN      -> 5000f
            EnemyType.COSMIC_SERPENT    -> 3000f
            EnemyType.ENTROPY_KING      -> 6000f
        }
        val bossMultiplier = if (isBoss) 6.0f else 1f
        return base * waveScale * bossMultiplier
    }

    private fun calculateDamage(): Float {
        val base = when (type) {
            EnemyType.VOID_CRAWLER      -> 6f
            EnemyType.COSMIC_SHADE      -> 9f
            EnemyType.NEBULA_WRAITH     -> 12f
            EnemyType.STAR_DEVOURER     -> 18f
            EnemyType.RIFT_STALKER      -> 15f
            EnemyType.VOID_TITAN        -> 30f
            EnemyType.COSMIC_GOD        -> 50f
            EnemyType.ENTROPY_HERALD    -> 40f
            EnemyType.SINGULARITY_SPAWN -> 25f
            EnemyType.ABYSS_KNIGHT      -> 28f
            EnemyType.PHASE_PHANTOM     -> 18f
            EnemyType.ELDRITCH_HORROR   -> 65f
            // New Enemies (averages)
            EnemyType.ANTIMATTER_CORE, EnemyType.NEUTRON_BEAST -> 35f
            EnemyType.PLASMA_REAPAR, EnemyType.NEURAL_HIVE -> 25f
            EnemyType.GRAVITY_WELLER, EnemyType.QUARK_GLUTTON -> 20f
            // New Bosses
            EnemyType.PRIME_SINGULARITY, EnemyType.STAR_EATER_TITAN -> 80f
            EnemyType.ENTROPY_KING, EnemyType.GALAXY_TITAN -> 100f
            EnemyType.VOID_ARCHON, EnemyType.DIMENSIONAL_GOD -> 90f
            else -> 15f
        }
        val bossDmgScale = if (isBoss) 1.25f else 1.0f
        val damageWaveScale = 1f + (wave - 1) * (GameConstants.ENEMY_DAMAGE_SCALE - 1f) * bossDmgScale
        return base * damageWaveScale * (if (isBoss) 3f else 1f)
    }

    private fun calculateSpeed(): Float {
        val base = when (type) {
            EnemyType.VOID_CRAWLER      -> 110f
            EnemyType.COSMIC_SHADE      -> 130f
            EnemyType.NEBULA_WRAITH     -> 90f
            EnemyType.STAR_DEVOURER     -> 70f
            EnemyType.RIFT_STALKER      -> 160f
            EnemyType.VOID_TITAN        -> 50f
            EnemyType.COSMIC_GOD        -> 80f
            EnemyType.ENTROPY_HERALD    -> 100f
            EnemyType.SINGULARITY_SPAWN -> 95f
            EnemyType.ABYSS_KNIGHT      -> 85f
            EnemyType.PHASE_PHANTOM     -> 145f
            EnemyType.ELDRITCH_HORROR   -> 60f
            // New ones
            EnemyType.SHADOW_STALKER    -> 180f
            EnemyType.PLASMA_REAPAR     -> 120f
            EnemyType.PHOTON_WRAITH     -> 200f
            EnemyType.CHRONO_EATER      -> 40f
            EnemyType.GALAXY_TITAN      -> 30f
            else -> 100f
        }
        return base * (if (isBoss) 0.8f else 1f)
    }

    private fun calculateAttackCooldown(): Long = when (type) {
        EnemyType.RIFT_STALKER, EnemyType.PHASE_PHANTOM, EnemyType.SHADOW_STALKER -> 800L
        EnemyType.STAR_DEVOURER, EnemyType.VOID_TITAN, EnemyType.ASTEROID_GOLEM   -> 2000L
        EnemyType.COSMIC_GOD, EnemyType.ELDRITCH_HORROR, EnemyType.ENTROPY_KING   -> 1500L
        else -> 1200L
    }

    private fun calculateXp(): Int {
        val base = when (type) {
            EnemyType.VOID_CRAWLER      -> 5
            EnemyType.COSMIC_SHADE      -> 8
            EnemyType.NEBULA_WRAITH     -> 12
            EnemyType.STAR_DEVOURER     -> 20
            EnemyType.RIFT_STALKER      -> 15
            EnemyType.VOID_TITAN        -> 60
            EnemyType.COSMIC_GOD        -> 200
            EnemyType.ENTROPY_HERALD    -> 150
            EnemyType.SINGULARITY_SPAWN -> 35
            EnemyType.ABYSS_KNIGHT      -> 40
            EnemyType.PHASE_PHANTOM     -> 10
            EnemyType.ELDRITCH_HORROR   -> 400
            // New ones
            EnemyType.ENTROPY_KING      -> 1000
            EnemyType.GALAXY_TITAN      -> 800
            EnemyType.PRIME_SINGULARITY -> 600
            else -> 25
        }
        return (base * (1f + wave * 0.1f)).toInt() * (if (isBoss) 5 else 1)
    }

    private fun calculateLootChance(): Float = when (type) {
        EnemyType.VOID_CRAWLER      -> 0.05f
        EnemyType.COSMIC_SHADE      -> 0.08f
        EnemyType.NEBULA_WRAITH     -> 0.12f
        EnemyType.STAR_DEVOURER     -> 0.20f
        EnemyType.RIFT_STALKER      -> 0.15f
        EnemyType.VOID_TITAN        -> 0.60f
        EnemyType.COSMIC_GOD        -> 1.00f
        EnemyType.ENTROPY_HERALD    -> 0.90f
        EnemyType.SINGULARITY_SPAWN -> 0.25f
        EnemyType.ABYSS_KNIGHT      -> 0.30f
        EnemyType.PHASE_PHANTOM     -> 0.10f
        EnemyType.ELDRITCH_HORROR   -> 1.00f
        else -> if (isBoss) 0.8f else 0.1f
    }

    private fun initBossAbilities() {
        abilities.addAll(listOf(
            EnemyAbility("void_burst", 4000L),
            EnemyAbility("summon_minions", 8000L),
            EnemyAbility("phase_dash", 3000L),
            EnemyAbility("cosmic_beam", 6000L)
        ))
    }

    fun takeDamage(amount: Float): Boolean {
        if (!isAlive) return false
        currentHp -= amount * vulnerability
        if (currentHp <= 0f) {
            currentHp = 0f
            isAlive = false
            return true
        }
        // Check enrage
        if (!isEnraged && (currentHp / maxHp) <= enrageThreshold) {
            isEnraged = true
            speed *= 1.4f
            damage *= 1.3f
            aiState = EnemyAIState.ENRAGED
        }
        // Boss phase transitions
        if (isBoss && currentHp / maxHp <= 0.5f && phase == 1) {
            phase = 2
            speed *= 1.2f
            radius *= 1.4f // Physically becomes a "bigger opponent"
            isEnraged = true
            aiState = EnemyAIState.ENRAGED
        }
        return false
    }

    fun hpPercent() = currentHp / maxHp

    fun updateAI(playerPos: Vector2, currentTimeMs: Long, dt: Float, isConfused: Boolean = false) {
        if (isConfused) {
            // Move towards a point that circles the player instead of directly at them
            val angle = (currentTimeMs * 0.001f) + (position.x * 0.01f)
            targetPosition.set(
                playerPos.x + cos(angle) * 400f,
                playerPos.y + sin(angle) * 400f
            )
        } else {
            targetPosition.set(playerPos)
        }
        rotationAngle += dt * 120f * (if (isEnraged) 2f else 1f)

        when (aiState) {
            EnemyAIState.SPAWNING -> {
                spawnProgress += dt * 2f
                if (spawnProgress >= 1f) {
                    spawnProgress = 1f
                    aiState = EnemyAIState.CHASING
                }
            }
            EnemyAIState.CHASING -> {
                val dir = (targetPosition - position).normalized()
                velocity.set(dir * speed)
                // Switch to circling at medium range
                val dist = position.distanceTo(targetPosition)
                if (dist < circleRadius && type != EnemyType.VOID_CRAWLER) {
                    if (Random.nextFloat() < 0.02f) aiState = EnemyAIState.CIRCLING
                }
                // Teleporter enemies
                if (type == EnemyType.PHASE_PHANTOM &&
                    currentTimeMs - lastTeleportMs > teleportCooldownMs) {
                    aiState = EnemyAIState.TELEPORTING
                }
            }
            EnemyAIState.CIRCLING -> {
                circleAngle += dt * 2.2f * (if (isEnraged) 1.6f else 1f)
                val targetX = targetPosition.x + cos(circleAngle) * circleRadius
                val targetY = targetPosition.y + sin(circleAngle) * circleRadius
                val dir = (Vector2(targetX, targetY) - position).normalized()
                velocity.set(dir * speed * 0.9f)
                if (Random.nextFloat() < 0.008f) aiState = EnemyAIState.CHASING
            }
            EnemyAIState.TELEPORTING -> {
                lastTeleportMs = currentTimeMs
                val angle = Random.nextFloat() * 2f * PI.toFloat()
                val dist = 80f + Random.nextFloat() * 100f
                position.set(
                    targetPosition.x + cos(angle) * dist,
                    targetPosition.y + sin(angle) * dist
                )
                velocity.set(0f, 0f)
                aiState = EnemyAIState.ATTACKING
            }
            EnemyAIState.ENRAGED -> {
                val dir = (targetPosition - position).normalized()
                velocity.set(dir * speed)
            }
            else -> {
                val dir = (targetPosition - position).normalized()
                velocity.set(dir * speed)
            }
        }
    }

    fun canAttack(currentTimeMs: Long) =
        currentTimeMs - lastAttackMs >= attackCooldownMs

    fun recordAttack(currentTimeMs: Long) {
        lastAttackMs = currentTimeMs
    }

    fun canUseAbility(abilityId: String, currentTimeMs: Long): Boolean {
        val ability = abilities.find { it.id == abilityId } ?: return false
        return currentTimeMs - ability.lastUsedMs >= ability.cooldownMs
    }

    fun useAbility(abilityId: String, currentTimeMs: Long) {
        abilities.find { it.id == abilityId }?.lastUsedMs = currentTimeMs
    }
}
