package com.voidascension.entities

import com.voidascension.engine.LootRarity
import com.voidascension.engine.WeaponType
import com.voidascension.engine.GameConstants
import com.voidascension.utils.Vector2
import kotlin.random.Random

// ─── Projectile ────────────────────────────────────────────────────────────────
data class Projectile(
    val id: Long,
    var position: Vector2,
    val velocity: Vector2,
    val damage: Float,
    val radius: Float = GameConstants.PROJECTILE_RADIUS,
    val weaponType: WeaponType,
    val pierceLeft: Int = 0,
    var pierceCount: Int = 0,
    val spawnTimeMs: Long = 0L,
    val lifetimeMs: Long = 2000L,
    var isAlive: Boolean = true,
    val isCrit: Boolean = false,
    val isExplosive: Boolean = false,
    val explosionRadius: Float = 60f,
    val color: Int = 0xFF00FFFF.toInt()
) {
    fun update(dt: Float) {
        position.x += velocity.x * dt
        position.y += velocity.y * dt
    }

    fun isExpired(currentTimeMs: Long) = currentTimeMs - spawnTimeMs > lifetimeMs
    fun canPierce() = pierceCount < pierceLeft
    fun recordPierce() { pierceCount++ }
}

// ─── Enemy Projectile ──────────────────────────────────────────────────────────
data class EnemyProjectile(
    val id: Long,
    var position: Vector2,
    val velocity: Vector2,
    val damage: Float,
    val radius: Float = 10f,
    val spawnTimeMs: Long = 0L,
    val lifetimeMs: Long = 3000L,
    var isAlive: Boolean = true,
    val color: Int = 0xFFFF4400.toInt(),
    val isHoming: Boolean = false,
    var homingTarget: Vector2? = null
) {
    fun update(dt: Float, playerPos: Vector2?) {
        if (isHoming && playerPos != null && homingTarget != null) {
            val dir = (playerPos - position).normalized()
            velocity.x += dir.x * 200f * dt
            velocity.y += dir.y * 200f * dt
            // Cap speed
            val spd = velocity.length()
            if (spd > 350f) {
                velocity.x = velocity.x / spd * 350f
                velocity.y = velocity.y / spd * 350f
            }
        }
        position.x += velocity.x * dt
        position.y += velocity.y * dt
    }

    fun isExpired(currentTimeMs: Long) = currentTimeMs - spawnTimeMs > lifetimeMs
}

// ─── Loot Item ─────────────────────────────────────────────────────────────────
data class LootItem(
    val id: Long,
    var position: Vector2,
    val rarity: LootRarity,
    val name: String,
    val description: String,
    val type: LootItemType,
    val value: Float = 0f,
    var spawnTimeMs: Long = 0L,
    val lifetimeMs: Long = 12000L,
    var collected: Boolean = false,
    var pulsePhase: Float = Random.nextFloat() * 6.28f
) {
    fun isExpired(currentTimeMs: Long) = currentTimeMs - spawnTimeMs > lifetimeMs
}

enum class LootItemType {
    WEAPON_UPGRADE, MUTATION, CYBER_IMPLANT,
    AURA_UNLOCK, HEALTH_PACK, XP_SHARD, SCORE_MULTIPLIER,
    VOID_SHARD
}

// ─── Particle ─────────────────────────────────────────────────────────────────
data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,         // 0..1 remaining
    var maxLife: Float,
    var size: Float,
    var color: Int,
    var alpha: Float = 1f,
    val type: ParticleType = ParticleType.SPARK
)

enum class ParticleType {
    SPARK, BLOOD, EXPLOSION, XP_ORB, SMOKE,
    VOID_PARTICLE, COSMIC_DUST, AURA_RING
}

// ─── Screen Shake ─────────────────────────────────────────────────────────────
data class ScreenShake(
    var durationMs: Long,
    val amplitude: Float,
    val startTimeMs: Long,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f
) {
    fun update(currentTimeMs: Long): Boolean {
        val elapsed = currentTimeMs - startTimeMs
        if (elapsed >= durationMs) return false
        val progress = 1f - elapsed.toFloat() / durationMs
        val shake = amplitude * progress
        offsetX = (Random.nextFloat() * 2f - 1f) * shake
        offsetY = (Random.nextFloat() * 2f - 1f) * shake
        return true
    }
}

// ─── Floating Text ────────────────────────────────────────────────────────────
data class FloatingText(
    var x: Float,
    var y: Float,
    val text: String,
    val color: Int,
    var life: Float = 1f,
    val isCrit: Boolean = false,
    var vy: Float = -80f
) {
    fun update(dt: Float): Boolean {
        y += vy * dt
        vy *= 0.95f
        life -= dt * 1.2f
        return life > 0f
    }
}
