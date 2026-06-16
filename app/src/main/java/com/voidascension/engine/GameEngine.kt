package com.voidascension.engine

import com.voidascension.entities.*
import com.voidascension.systems.*
import com.voidascension.utils.Vector2
import kotlinx.coroutines.*
import kotlin.math.*
import kotlin.random.Random

data class GameSnapshot(
    val player: Player,
    val enemies: List<Enemy>,
    val projectiles: List<Projectile>,
    val enemyProjectiles: List<EnemyProjectile>,
    val lootItems: List<LootItem>,
    val particles: List<Particle>,
    val floatingTexts: List<FloatingText>,
    val screenShake: ScreenShake?,
    val gameState: GameState,
    val waveNumber: Int,
    val waveTitle: String,
    val isBossWave: Boolean,
    val upgradeOptions: List<UpgradeOption>,
    val screenWidth: Float,
    val screenHeight: Float,
    val avatarIndex: Int = 0
)

class GameEngine(
    private var screenWidth: Float,
    private var screenHeight: Float,
    private val avatarIndex: Int,
    private val onSnapshot: (GameSnapshot) -> Unit,
    private val onGameOver: (score: Int, wave: Int, kills: Int) -> Unit,
    private val onLevelUp: () -> Unit
) {
    // ─── Core Systems ──────────────────────────────────────────────────────────
    val player = Player(Vector2(screenWidth / 2f, screenHeight / 2f))
    private val combatSystem = CombatSystem()
    private val waveManager = WaveManager(screenWidth, screenHeight)
    private val lootSystem = LootSystem()
    val particleSystem = ParticleSystem()

    // ─── Entity Lists ──────────────────────────────────────────────────────────
    val enemies = ArrayList<Enemy>(64)
    val projectiles = ArrayList<Projectile>(128)
    val enemyProjectiles = ArrayList<EnemyProjectile>(64)
    val lootItems = ArrayList<LootItem>(32)
    val floatingTexts = ArrayList<FloatingText>(32)

    // ─── State ─────────────────────────────────────────────────────────────────
    var gameState = GameState.PLAYING
    private var lastUpdateMs = System.currentTimeMillis()
    private var lastFireMs = 0L
    private var lastFireDirectionMs = 0L
    private var currentFireTarget: Vector2? = null
    private var screenShake: ScreenShake? = null
    private var upgradeOptions = listOf<UpgradeOption>()
    private var waveTitle = "WAVE 1"
    private var isBossWave = false
    private var pendingLevelUp = false

    // ─── Joystick Input ───────────────────────────────────────────────────────
    var joystickDelta: Vector2 = Vector2()
    var fireJoystickDelta: Vector2 = Vector2()

    // ─── Game Loop ────────────────────────────────────────────────────────────
    private var gameJob: Job? = null

    fun updateScreenSize(newW: Float, newH: Float) {
        screenWidth = newW
        screenHeight = newH
        waveManager.updateScreenSize(newW, newH)
    }

    fun start(scope: CoroutineScope) {
        waveManager.startWave(1, System.currentTimeMillis())
        waveTitle = "WAVE 1"
        gameJob = scope.launch(Dispatchers.Default) {
            lastUpdateMs = System.currentTimeMillis()
            while (isActive && player.isAlive) {
                val now = System.currentTimeMillis()
                val dt = ((now - lastUpdateMs) / 1000f).coerceAtMost(0.05f)
                lastUpdateMs = now

                if (gameState == GameState.PLAYING) {
                    update(dt, now)
                }

                val snapshot = buildSnapshot()
                onSnapshot(snapshot)

                val elapsed = System.currentTimeMillis() - now
                val sleep = GameConstants.FRAME_TIME_MS - elapsed
                if (sleep > 0) delay(sleep)
            }
        }
    }

    fun pause() { gameState = GameState.PAUSED }
    fun resume() { if (gameState == GameState.PAUSED) gameState = GameState.PLAYING }
    fun stop() { gameJob?.cancel() }

    // ─── Main Update ──────────────────────────────────────────────────────────
    private fun update(dt: Float, currentTimeMs: Long) {
        updatePlayer(dt, currentTimeMs)
        updateEnemies(dt, currentTimeMs)
        updateProjectiles(dt, currentTimeMs)
        updateEnemyProjectiles(dt, currentTimeMs)
        updateLoot(currentTimeMs)
        updateParticles(dt)
        updateFloatingTexts(dt)
        updateScreenShake(currentTimeMs)
        handleCombat(currentTimeMs)
        handleAuras(currentTimeMs)
        handleWave(currentTimeMs)
        cleanup()
    }

    private fun updatePlayer(dt: Float, currentTimeMs: Long) {
        player.checkInvincibility(currentTimeMs)

        // Movement from joystick
        if (joystickDelta.length() > 0.05f) {
            val move = joystickDelta.normalized() * player.effectiveSpeed * dt
            player.position.x = (player.position.x + move.x).coerceIn(player.radius, screenWidth - player.radius)
            player.position.y = (player.position.y + move.y).coerceIn(player.radius, screenHeight - player.radius)
            particleSystem.emitTrail(player.position.x, player.position.y, 0xFF004488.toInt())
        }

        // Auto attack
        val fireInterval = (1000f / player.effectiveFireRate).toLong()
        if (currentTimeMs - lastFireMs >= fireInterval) {
            val target = findFireTarget()
            if (target != null) {
                val newProjectiles = combatSystem.createPlayerProjectiles(player, target, currentTimeMs)
                projectiles.addAll(newProjectiles)
                lastFireMs = currentTimeMs
                particleSystem.emitTrail(player.position.x, player.position.y, 0xFF0088FF.toInt())
            }
        }
    }

    private fun findFireTarget(): Vector2? {
        // If fire joystick is active, use that direction
        if (fireJoystickDelta.length() > 0.2f) {
            return Vector2(
                player.position.x + fireJoystickDelta.x * 300f,
                player.position.y + fireJoystickDelta.y * 300f
            )
        }
        // Otherwise auto-aim at nearest enemy
        val nearest = combatSystem.findNearestEnemy(player.position, enemies)
        return nearest?.position
    }

    private fun updateEnemies(dt: Float, currentTimeMs: Long) {
        // Spawn from wave manager
        val toSpawn = waveManager.getEnemiesToSpawn(currentTimeMs)
        enemies.addAll(toSpawn)

        // Update AI
        for (enemy in enemies) {
            if (!enemy.isAlive) continue
            enemy.updateAI(player.position, currentTimeMs, dt)
            enemy.position.x += enemy.velocity.x * dt
            enemy.position.y += enemy.velocity.y * dt

            // Boss shoots projectiles
            if (enemy.isBoss || enemy.type == EnemyType.RIFT_STALKER ||
                enemy.type == EnemyType.ENTROPY_HERALD || enemy.type == EnemyType.COSMIC_GOD) {
                val proj = combatSystem.createEnemyProjectile(enemy, player.position, currentTimeMs)
                if (proj != null) enemyProjectiles.add(proj)
            }

            // Boss abilities
            if (enemy.isBoss) {
                handleBossAbilities(enemy, currentTimeMs)
            }
        }
    }

    private fun handleBossAbilities(enemy: Enemy, currentTimeMs: Long) {
        if (enemy.canUseAbility("summon_minions", currentTimeMs) &&
            enemies.count { !it.isBoss && it.isAlive } < 8) {
            enemy.useAbility("summon_minions", currentTimeMs)
            repeat(3) {
                val offset = Vector2.random(100f)
                enemies.add(Enemy(
                    type = EnemyType.VOID_CRAWLER,
                    isBoss = false,
                    wave = enemy.wave,
                    startPos = Vector2(enemy.position.x + offset.x, enemy.position.y + offset.y)
                ))
            }
        }
    }

    private fun updateProjectiles(dt: Float, currentTimeMs: Long) {
        for (proj in projectiles) {
            if (!proj.isAlive) continue
            proj.update(dt)
            // Optimization: Throttled trail emission
            if (currentTimeMs % 48 < 16) {
                particleSystem.emitTrail(proj.position.x, proj.position.y, proj.color)
            }
            if (proj.isExpired(currentTimeMs) ||
                proj.position.x < -50 || proj.position.x > screenWidth + 50 ||
                proj.position.y < -50 || proj.position.y > screenHeight + 50) {
                proj.isAlive = false
            }
        }
    }

    private fun updateEnemyProjectiles(dt: Float, currentTimeMs: Long) {
        for (proj in enemyProjectiles) {
            if (!proj.isAlive) continue
            proj.update(dt, player.position)
            particleSystem.emitTrail(proj.position.x, proj.position.y, proj.color)
            if (proj.isExpired(currentTimeMs) ||
                proj.position.x < -50 || proj.position.x > screenWidth + 50 ||
                proj.position.y < -50 || proj.position.y > screenHeight + 50) {
                proj.isAlive = false
            }
        }
    }

    private fun updateLoot(currentTimeMs: Long) {
        val iter = lootItems.iterator()
        while (iter.hasNext()) {
            val loot = iter.next()
            if (loot.collected || loot.isExpired(currentTimeMs)) {
                iter.remove()
                continue
            }
            loot.pulsePhase += 0.05f
            // Auto-collect nearby loot
            if (player.position.distanceTo(loot.position) <= player.radius + 30f) {
                collectLoot(loot, currentTimeMs)
                iter.remove()
            }
        }
    }

    private fun collectLoot(loot: LootItem, currentTimeMs: Long) {
        loot.collected = true
        when (loot.type) {
            LootItemType.HEALTH_PACK -> {
                player.heal(loot.value)
                addFloatingText(loot.position, "+${loot.value.toInt()} HP", 0xFF00FF44.toInt())
            }
            LootItemType.XP_SHARD -> {
                val leveledUp = player.addXp(loot.value.toInt())
                addFloatingText(loot.position, "+${loot.value.toInt()} XP", 0xFF00FFAA.toInt())
                particleSystem.emitXpOrb(loot.position.x, loot.position.y)
                if (leveledUp) triggerLevelUp()
            }
            LootItemType.MUTATION -> {
                val mutation = MutationType.entries.random()
                player.applyMutation(mutation)
                addFloatingText(loot.position, "MUTATION!", loot.rarity.color)
            }
            LootItemType.WEAPON_UPGRADE -> {
                player.damageMultiplier += loot.value
                addFloatingText(loot.position, "+DMG", loot.rarity.color)
            }
            LootItemType.CYBER_IMPLANT -> {
                addFloatingText(loot.position, "IMPLANT!", loot.rarity.color)
            }
            LootItemType.AURA_UNLOCK -> {
                val aura = AuraType.entries.random()
                player.addAura(aura)
                addFloatingText(loot.position, "AURA!", loot.rarity.color)
            }
            LootItemType.SCORE_MULTIPLIER -> {
                player.score = (player.score * loot.value).toInt()
                addFloatingText(loot.position, "SCORE x${loot.value}", loot.rarity.color)
            }
            LootItemType.VOID_SHARD -> {
                player.collectedVoidShards += loot.value.toInt()
                addFloatingText(loot.position, "+${loot.value.toInt()} SHARDS", 0xFFFFAA00.toInt())
            }
        }
    }

    private fun updateParticles(dt: Float) = particleSystem.update(dt)

    private fun updateFloatingTexts(dt: Float) {
        floatingTexts.removeAll { !it.update(dt) }
    }

    private fun updateScreenShake(currentTimeMs: Long) {
        screenShake?.let {
            if (!it.update(currentTimeMs)) screenShake = null
        }
    }

    private fun handleCombat(currentTimeMs: Long) {
        // Player projectiles vs enemies
        val hits = combatSystem.checkProjectileEnemyCollisions(projectiles, enemies, player)
        for (hit in hits) {
            addFloatingText(
                hit.hitEnemy.position,
                if (hit.isCrit) "CRIT ${hit.damage.toInt()}!" else "${hit.damage.toInt()}",
                if (hit.isCrit) 0xFFFFFF00.toInt() else 0xFFFFAA00.toInt(),
                hit.isCrit
            )
            particleSystem.emitHit(hit.hitEnemy.position.x, hit.hitEnemy.position.y,
                0xFF00FFFF.toInt(), hit.isCrit)

            // Explosion splash
            hit.explosionPos?.let { epos ->
                combatSystem.applyExplosionDamage(epos, 60f, hit.damage * 0.5f, enemies)
                particleSystem.emitExplosion(epos.x, epos.y, 60f)
                triggerScreenShake(GameConstants.SHAKE_AMPLITUDE * 0.5f, currentTimeMs)
            }

            if (hit.isKill) {
                onEnemyKilled(hit.hitEnemy, currentTimeMs)
            }
        }

        // Enemy body vs player
        val bodyDmg = combatSystem.checkEnemyPlayerCollisions(enemies, player, currentTimeMs)
        if (bodyDmg > 0f) {
            particleSystem.emitPlayerDamage(player.position.x, player.position.y)
            triggerScreenShake(GameConstants.SHAKE_AMPLITUDE, currentTimeMs)
            addFloatingText(player.position, "-${bodyDmg.toInt()}", 0xFFFF0000.toInt())
        }

        // Enemy projectiles vs player
        val projDmg = combatSystem.checkEnemyProjectilePlayerCollisions(enemyProjectiles, player, currentTimeMs)
        if (projDmg > 0f) {
            particleSystem.emitPlayerDamage(player.position.x, player.position.y)
            triggerScreenShake(GameConstants.SHAKE_AMPLITUDE * 0.7f, currentTimeMs)
            addFloatingText(player.position, "-${projDmg.toInt()}", 0xFFFF2200.toInt())
        }

        if (!player.isAlive) {
            gameState = GameState.GAME_OVER
            onGameOver(player.score, player.waveNumber, player.killCount)
        }
    }

    private fun handleAuras(currentTimeMs: Long) {
        val effects = combatSystem.tickAuras(player, enemies, currentTimeMs)
        for (effect in effects) {
            for (enemy in effect.affectedEnemies) {
                if (effect.damage > 0f) {
                    particleSystem.emitHit(enemy.position.x, enemy.position.y, getAuraColor(effect.auraType))
                }
            }
            val auraColor = getAuraColor(effect.auraType)
            particleSystem.emitAura(player.position.x, player.position.y, GameConstants.AURA_RADIUS, auraColor)
        }
    }

    private fun handleWave(currentTimeMs: Long) {
        if (!waveManager.isWaveActive) return

        val activeEnemies = enemies.count { it.isAlive }
        if (waveManager.isWaveComplete(activeEnemies)) {
            // Give upgrade choices on wave complete
            upgradeOptions = lootSystem.generateUpgradeChoices(player.waveNumber)
            gameState = GameState.UPGRADE_SELECT

            player.waveNumber++
            waveTitle = if (player.waveNumber % GameConstants.BOSS_WAVE_INTERVAL == 0)
                "BOSS INCOMING" else "WAVE ${player.waveNumber}"
            isBossWave = player.waveNumber % GameConstants.BOSS_WAVE_INTERVAL == 0
        }
    }

    fun startNextWave() {
        upgradeOptions = emptyList()
        gameState = GameState.PLAYING
        waveManager.startWave(player.waveNumber, System.currentTimeMillis())
    }

    fun selectUpgrade(option: UpgradeOption) {
        when (option.type) {
            UpgradeType.MUTATION -> option.mutationType?.let { player.applyMutation(it) }
            UpgradeType.AURA -> option.auraType?.let { player.addAura(it) }
            UpgradeType.HEAL -> player.heal(player.maxHp * 0.4f)
            UpgradeType.WEAPON_UPGRADE -> player.damageMultiplier += 0.2f
            UpgradeType.CYBER_IMPLANT -> { /* handled via loot */ }
        }
        startNextWave()
    }

    private fun onEnemyKilled(enemy: Enemy, currentTimeMs: Long) {
        player.killCount++
        val xpGained = enemy.xpValue
        val leveledUp = player.addXp(xpGained)
        particleSystem.emitDeath(enemy.position.x, enemy.position.y, enemy.isBoss)
        if (enemy.isBoss) {
            triggerScreenShake(GameConstants.SHAKE_AMPLITUDE * 2f, currentTimeMs)
            // Boss always drops loot + guaranteed shards
            val bossLoot = lootSystem.rollMultipleLoot(enemy.position, currentTimeMs, 3).toMutableList()
            bossLoot.add(LootItem(
                id = 0, // id will be set by rollLoot usually but we can just use 0 here or counter
                position = Vector2(enemy.position.x, enemy.position.y),
                rarity = LootRarity.EPIC,
                name = "Boss Shard",
                description = "25 Void Shards",
                type = LootItemType.VOID_SHARD,
                value = 25f,
                spawnTimeMs = currentTimeMs,
                lifetimeMs = 60000L,
                collected = false,
                pulsePhase = 0f
            ))
            lootItems.addAll(bossLoot)
        } else if (Random.nextFloat() < enemy.lootChance) {
            val loot = lootSystem.rollLoot(enemy.position, currentTimeMs)
            if (loot != null) lootItems.add(loot)
        }
        if (leveledUp) triggerLevelUp()
    }

    private fun triggerLevelUp() {
        pendingLevelUp = true
        addFloatingText(player.position, "LEVEL UP!", 0xFFFFFF00.toInt())
        particleSystem.emitExplosion(player.position.x, player.position.y, 40f)
        onLevelUp()
    }

    private fun triggerScreenShake(amplitude: Float, currentTimeMs: Long) {
        screenShake = ScreenShake(GameConstants.SHAKE_DURATION_MS, amplitude, currentTimeMs)
    }

    private fun addFloatingText(pos: Vector2, text: String, color: Int, isCrit: Boolean = false) {
        floatingTexts.add(FloatingText(pos.x, pos.y, text, color, isCrit = isCrit))
    }

    private fun getAuraColor(auraType: AuraType) = when (auraType) {
        AuraType.PLASMA_SHIELD    -> 0xFF0088FF.toInt()
        AuraType.VOID_DRAIN       -> 0xFF8800FF.toInt()
        AuraType.STATIC_FIELD     -> 0xFFFFFF00.toInt()
        AuraType.QUANTUM_ECHO     -> 0xFF00FFFF.toInt()
        AuraType.ENTROPIC_PULSE   -> 0xFFFF8800.toInt()
        AuraType.NEUROSTATIC_WAVE -> 0xFF00FF88.toInt()
    }

    private fun cleanup() {
        enemies.removeAll { !it.isAlive }
        projectiles.removeAll { !it.isAlive }
        enemyProjectiles.removeAll { !it.isAlive }
    }

    private fun buildSnapshot() = GameSnapshot(
        player = player,
        enemies = ArrayList(enemies),
        projectiles = ArrayList(projectiles),
        enemyProjectiles = ArrayList(enemyProjectiles),
        lootItems = ArrayList(lootItems),
        particles = ArrayList(particleSystem.particles),
        floatingTexts = ArrayList(floatingTexts),
        screenShake = screenShake,
        gameState = gameState,
        waveNumber = player.waveNumber,
        waveTitle = waveTitle,
        isBossWave = isBossWave,
        upgradeOptions = upgradeOptions,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        avatarIndex = avatarIndex
    )
}
