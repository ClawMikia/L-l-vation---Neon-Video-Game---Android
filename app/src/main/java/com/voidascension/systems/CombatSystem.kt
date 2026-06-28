package com.voidascension.systems

import com.voidascension.engine.*
import com.voidascension.entities.*
import com.voidascension.utils.Vector2
import kotlin.math.*
import kotlin.random.Random

class CombatSystem {
    private var projectileIdCounter = 0L
    private var enemyProjectileIdCounter = 0L

    // ─── Player Auto-Attack ────────────────────────────────────────────────────
    fun createPlayerProjectiles(
        player: Player,
        targetPos: Vector2,
        currentTimeMs: Long
    ): List<Projectile> {
        val projectiles = mutableListOf<Projectile>()
        val baseDir = (targetPos - player.position).normalized()
        val damage = player.effectiveDamage
        val isCrit = Random.nextFloat() < player.critChance
        val finalDamage = if (isCrit) damage * player.critMultiplier else damage

        val count = player.projectileCount
        val spreadAngle = if (count > 1) 0.25f else 0f

        repeat(count) { i ->
            val offset = if (count == 1) 0f else (i - (count - 1) / 2f) * spreadAngle
            val angle = baseDir.angle() + offset
            val dir = Vector2.fromAngle(angle)

            val color = when (player.weaponType) {
                WeaponType.PLASMA_RIFLE    -> 0xFF00FFFF.toInt()
                WeaponType.VOID_CANNON     -> 0xFF8800FF.toInt()
                WeaponType.NEURAL_LANCE    -> 0xFF00FF88.toInt()
                WeaponType.QUANTUM_BURST   -> 0xFFFFFF00.toInt()
                WeaponType.COSMIC_SCYTHE   -> 0xFFFF00FF.toInt()
                WeaponType.ENTROPY_BEAM    -> 0xFFFF8800.toInt()
                WeaponType.PHASE_BLADE     -> 0xFF44FFFF.toInt()
                WeaponType.SINGULARITY_GUN -> 0xFFFF44AA.toInt()
                WeaponType.NEUROTOXIN_SPRAYER -> 0xFF88FF00.toInt()
            }

            val isExplosive = player.mutations.contains(MutationType.EXPLOSIVE_ROUNDS)

            projectiles.add(Projectile(
                id = projectileIdCounter++,
                position = player.position.copy(),
                velocity = dir * GameConstants.PROJECTILE_SPEED,
                damage = finalDamage,
                weaponType = player.weaponType,
                pierceLeft = player.projectilePierce,
                spawnTimeMs = currentTimeMs,
                isCrit = isCrit,
                isExplosive = isExplosive,
                color = color,
                radius = if (isExplosive) 12f else GameConstants.PROJECTILE_RADIUS
            ))
        }
        return projectiles
    }

    // ─── Enemy Attack ─────────────────────────────────────────────────────────
    fun createEnemyProjectile(
        enemy: Enemy,
        playerPos: Vector2,
        currentTimeMs: Long
    ): EnemyProjectile? {
        if (!enemy.canAttack(currentTimeMs)) return null
        enemy.recordAttack(currentTimeMs)

        val dir = (playerPos - enemy.position).normalized()
        val speed = when (enemy.type) {
            EnemyType.RIFT_STALKER -> 380f
            EnemyType.COSMIC_GOD   -> 320f
            EnemyType.ENTROPY_HERALD -> 280f
            else -> 250f
        }

        val isHoming = enemy.isBoss && enemy.phase >= 2
        return EnemyProjectile(
            id = enemyProjectileIdCounter++,
            position = enemy.position.copy(),
            velocity = dir * speed,
            damage = enemy.damage,
            spawnTimeMs = currentTimeMs,
            color = when (enemy.type) {
                EnemyType.VOID_CRAWLER   -> 0xFFFF2200.toInt()
                EnemyType.COSMIC_GOD     -> 0xFFFF00FF.toInt()
                EnemyType.ENTROPY_HERALD -> 0xFFFF8800.toInt()
                EnemyType.ELDRITCH_HORROR-> 0xFF8800FF.toInt()
                else -> 0xFFFF4400.toInt()
            },
            isHoming = isHoming,
            homingTarget = if (isHoming) playerPos.copy() else null
        )
    }

    // ─── Projectile vs Enemy Collisions ───────────────────────────────────────
    data class HitResult(
        val hitEnemy: Enemy,
        val damage: Float,
        val isCrit: Boolean,
        val isKill: Boolean,
        val explosionPos: Vector2? = null
    )

    fun checkProjectileEnemyCollisions(
        projectiles: MutableList<Projectile>,
        enemies: List<Enemy>,
        player: Player
    ): List<HitResult> {
        val hits = mutableListOf<HitResult>()

        for (proj in projectiles) {
            if (!proj.isAlive) continue
            for (enemy in enemies) {
                if (!enemy.isAlive) continue
                val distSq = proj.position.distanceSquaredTo(enemy.position)
                val radiusSum = proj.radius + enemy.radius
                if (distSq <= radiusSum * radiusSum) {
                    var finalDamage = if (enemy.isBoss) proj.damage * player.bossDamageMultiplier else proj.damage
                    
                    // Kokey: 1 shot, 1 kill, except for the bosses
                    if (player.isOneShotKill && !enemy.isBoss) {
                        finalDamage = enemy.currentHp + 1000f
                    }

                    // Apply Attack Auras
                    player.auras.forEach { aura ->
                        when (aura.type) {
                            AuraType.TITAN_SLAYER -> if (enemy.isBoss) finalDamage *= (1.2f + aura.level * 0.1f)
                            AuraType.CORROSIVE_AURA -> finalDamage *= (1.05f + aura.level * 0.05f)
                            AuraType.FROST_AURA -> enemy.speed *= (0.85f - aura.level * 0.02f)
                            AuraType.CHAIN_LIGHTNING -> {
                                if (Random.nextFloat() < 0.2f) {
                                    // Logic for arcing would require passing all enemies here, 
                                    // but let's just add bonus damage for now to keep it simple
                                    finalDamage *= 1.25f
                                }
                            }
                            else -> {}
                        }
                    }

                    val isKill = enemy.takeDamage(finalDamage)
                    // Life steal
                    if (player.lifeSteal > 0f) {
                        player.heal(finalDamage * player.lifeSteal)
                    }
                    hits.add(HitResult(
                        hitEnemy = enemy,
                        damage = finalDamage,
                        isCrit = proj.isCrit,
                        isKill = isKill,
                        explosionPos = if (proj.isExplosive) proj.position.copy() else null
                    ))

                    if (proj.canPierce()) {
                        proj.recordPierce()
                    } else {
                        proj.isAlive = false
                        break
                    }
                }
            }
        }
        return hits
    }

    // ─── Explosion Splash ─────────────────────────────────────────────────────
    fun applyExplosionDamage(
        explosionPos: Vector2,
        radius: Float,
        damage: Float,
        enemies: List<Enemy>
    ): List<HitResult> {
        val radiusSq = radius * radius
        return enemies.filter { it.isAlive }
            .filter { it.position.distanceSquaredTo(explosionPos) <= radiusSq }
            .map { enemy ->
                val isKill = enemy.takeDamage(damage * 0.5f)
                HitResult(enemy, damage * 0.5f, false, isKill)
            }
    }

    // ─── Enemy vs Player Collisions ───────────────────────────────────────────
    fun checkEnemyPlayerCollisions(
        enemies: List<Enemy>,
        player: com.voidascension.entities.Player,
        currentTimeMs: Long
    ): Float {
        var totalDamage = 0f
        val playerRadius = player.radius
        for (enemy in enemies) {
            if (!enemy.isAlive || !player.isAlive || enemy.aiState == com.voidascension.entities.EnemyAIState.SPAWNING) continue
            val distSq = player.position.distanceSquaredTo(enemy.position)
            val radiusSum = playerRadius + enemy.radius
            if (distSq <= radiusSum * radiusSum) {
                val dmg = player.takeDamage(enemy.damage, currentTimeMs)
                totalDamage += dmg
            }
        }
        return totalDamage
    }

    // ─── Enemy Projectile vs Player ───────────────────────────────────────────
    fun checkEnemyProjectilePlayerCollisions(
        enemyProjectiles: MutableList<EnemyProjectile>,
        player: Player,
        currentTimeMs: Long
    ): Float {
        var totalDamage = 0f
        val playerRadius = player.radius
        for (proj in enemyProjectiles) {
            if (!proj.isAlive) continue
            val distSq = proj.position.distanceSquaredTo(player.position)
            val radiusSum = proj.radius + playerRadius
            if (distSq <= radiusSum * radiusSum) {
                val dmg = player.takeDamage(proj.damage, currentTimeMs)
                totalDamage += dmg
                proj.isAlive = false
            }
        }
        return totalDamage
    }

    // ─── Aura Tick ────────────────────────────────────────────────────────────
    data class AuraEffect(
        val auraType: AuraType,
        val affectedEnemies: List<Enemy>,
        val healAmount: Float = 0f,
        val damage: Float = 0f
    )

    fun tickAuras(
        player: Player,
        enemies: List<Enemy>,
        currentTimeMs: Long
    ): List<AuraEffect> {
        val effects = mutableListOf<AuraEffect>()

        for (aura in player.auras) {
            if (currentTimeMs - aura.lastTickMs < GameConstants.AURA_TICK_MS) continue
            aura.lastTickMs = currentTimeMs

            val auraRadius = GameConstants.AURA_RADIUS * player.auraMultiplier * (1f + aura.level * 0.15f)
            val auraRadiusSq = auraRadius * auraRadius
            val nearbyEnemies = enemies.filter {
                it.isAlive && it.position.distanceSquaredTo(player.position) <= auraRadiusSq
            }

            when (aura.type) {
                AuraType.PLASMA_SHIELD -> {
                    // Handled in renderer as shield, absorbs next hit
                    effects.add(AuraEffect(aura.type, emptyList(), 0f, 0f))
                }
                AuraType.VOID_DRAIN -> {
                    val drainDmg = (player.effectiveDamage * 0.15f * aura.level)
                    var totalDrained = 0f
                    nearbyEnemies.forEach { enemy ->
                        enemy.takeDamage(drainDmg)
                        totalDrained += drainDmg * 0.3f
                    }
                    if (nearbyEnemies.isNotEmpty()) player.heal(totalDrained)
                    effects.add(AuraEffect(aura.type, nearbyEnemies, totalDrained, drainDmg))
                }
                AuraType.STATIC_FIELD -> {
                    val staticDmg = (player.effectiveDamage * 0.08f * aura.level)
                    nearbyEnemies.forEach { enemy ->
                        enemy.takeDamage(staticDmg)
                        enemy.speed *= 0.92f // slow
                    }
                    effects.add(AuraEffect(aura.type, nearbyEnemies, 0f, staticDmg))
                }
                AuraType.QUANTUM_ECHO -> {
                    effects.add(AuraEffect(aura.type, nearbyEnemies))
                }
                AuraType.ENTROPIC_PULSE -> {
                    if (currentTimeMs - aura.lastTickMs < 3000L) continue
                    val pulseDmg = (player.effectiveDamage * 0.8f * aura.level)
                    nearbyEnemies.forEach { enemy -> enemy.takeDamage(pulseDmg) }
                    effects.add(AuraEffect(aura.type, nearbyEnemies, 0f, pulseDmg))
                }
                AuraType.NEUROSTATIC_WAVE -> {
                    nearbyEnemies.forEach { enemy ->
                        enemy.velocity.set(0f, 0f)
                        enemy.aiState = com.voidascension.entities.EnemyAIState.SPAWNING
                    }
                    effects.add(AuraEffect(aura.type, nearbyEnemies))
                }
                AuraType.REGEN_CORE -> {
                    player.heal(1f * aura.level)
                    effects.add(AuraEffect(aura.type, emptyList()))
                }
                AuraType.GRAVITY_FIELD -> {
                    nearbyEnemies.forEach { enemy ->
                        enemy.speed *= 0.7f
                        val pullDir = (player.position - enemy.position).normalized()
                        enemy.position.x += pullDir.x * 30f * GameConstants.AURA_TICK_MS / 1000f
                        enemy.position.y += pullDir.y * 30f * GameConstants.AURA_TICK_MS / 1000f
                    }
                    effects.add(AuraEffect(aura.type, nearbyEnemies))
                }
                AuraType.BLAZING_AURA -> {
                    val dmg = player.effectiveDamage * 0.1f * aura.level
                    nearbyEnemies.forEach { enemy -> enemy.takeDamage(dmg) }
                    effects.add(AuraEffect(aura.type, nearbyEnemies, 0f, dmg))
                }
                AuraType.HOLY_BARRIER -> {
                    // Visual or damage reduction logic elsewhere? 
                    // Let's just say it heals a tiny bit
                    player.heal(0.5f * aura.level)
                    effects.add(AuraEffect(aura.type, emptyList()))
                }
                else -> {}
            }
        }
        return effects
    }

    // ─── Find Auto-Target ─────────────────────────────────────────────────────
    fun findNearestEnemy(playerPos: Vector2, enemies: List<Enemy>): Enemy? {
        return enemies.filter { it.isAlive }
            .minByOrNull { it.position.distanceSquaredTo(playerPos) }
    }
}
