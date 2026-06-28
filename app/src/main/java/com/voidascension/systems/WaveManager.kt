package com.voidascension.systems

import com.voidascension.engine.*
import com.voidascension.entities.Enemy
import com.voidascension.utils.Vector2
import kotlin.math.*
import kotlin.random.Random

data class WaveConfig(
    val waveNumber: Int,
    val isBossWave: Boolean,
    val enemies: List<EnemySpawnConfig>,
    val durationMs: Long,
    val title: String
)

data class EnemySpawnConfig(
    val type: EnemyType,
    val count: Int,
    val isBoss: Boolean = false,
    val isMainBoss: Boolean = false,
    val spawnDelayMs: Long = 0L,
    val interval: Long = 500L
)

class WaveManager(
    private var screenWidth: Float,
    private var screenHeight: Float
) {
    var currentWave: Int = 1
    var waveStartTimeMs: Long = 0L
    var isWaveActive: Boolean = false
    var enemiesRemainingToSpawn: Int = 0
    private var spawnQueue: ArrayDeque<Pair<EnemySpawnConfig, Int>> = ArrayDeque()
    private var lastSpawnMs: Long = 0L

    fun updateScreenSize(newW: Float, newH: Float) {
        screenWidth = newW
        screenHeight = newH
    }

    fun buildWave(waveNumber: Int): WaveConfig {
        val isBossWave = waveNumber % GameConstants.BOSS_WAVE_INTERVAL == 0
        return if (isBossWave) buildBossWave(waveNumber) else buildNormalWave(waveNumber)
    }

    private fun buildNormalWave(wave: Int): WaveConfig {
        val baseCount = (GameConstants.WAVE_BASE_ENEMIES * (1f + wave * 0.15f)).toInt()
        val enemies = mutableListOf<EnemySpawnConfig>()

        when {
            wave <= 3 -> {
                enemies.add(EnemySpawnConfig(EnemyType.VOID_CRAWLER, baseCount))
            }
            wave <= 6 -> {
                enemies.add(EnemySpawnConfig(EnemyType.VOID_CRAWLER, (baseCount * 0.6f).toInt()))
                enemies.add(EnemySpawnConfig(EnemyType.COSMIC_SHADE, (baseCount * 0.4f).toInt(), spawnDelayMs = 3000L))
            }
            wave <= 10 -> {
                enemies.add(EnemySpawnConfig(EnemyType.VOID_CRAWLER, (baseCount * 0.3f).toInt()))
                enemies.add(EnemySpawnConfig(EnemyType.COSMIC_SHADE, (baseCount * 0.3f).toInt(), spawnDelayMs = 2000L))
                enemies.add(EnemySpawnConfig(EnemyType.NEBULA_WRAITH, (baseCount * 0.4f).toInt(), spawnDelayMs = 5000L))
            }
            wave <= 15 -> {
                enemies.add(EnemySpawnConfig(EnemyType.NEBULA_WRAITH, (baseCount * 0.3f).toInt()))
                enemies.add(EnemySpawnConfig(EnemyType.RIFT_STALKER, (baseCount * 0.3f).toInt(), spawnDelayMs = 3000L))
                enemies.add(EnemySpawnConfig(EnemyType.PHASE_PHANTOM, (baseCount * 0.2f).toInt(), spawnDelayMs = 6000L))
                enemies.add(EnemySpawnConfig(EnemyType.STAR_DEVOURER, (baseCount * 0.2f).toInt(), spawnDelayMs = 9000L))
            }
            wave <= 20 -> {
                enemies.add(EnemySpawnConfig(EnemyType.RIFT_STALKER, (baseCount * 0.25f).toInt()))
                enemies.add(EnemySpawnConfig(EnemyType.ABYSS_KNIGHT, (baseCount * 0.25f).toInt(), spawnDelayMs = 3000L))
                enemies.add(EnemySpawnConfig(EnemyType.SINGULARITY_SPAWN, (baseCount * 0.25f).toInt(), spawnDelayMs = 6000L))
                enemies.add(EnemySpawnConfig(EnemyType.VOID_TITAN, (baseCount * 0.1f).toInt(), spawnDelayMs = 10000L))
                enemies.add(EnemySpawnConfig(EnemyType.PHASE_PHANTOM, (baseCount * 0.15f).toInt(), spawnDelayMs = 4000L))
            }
            else -> {
                // Wave 21+: Mix of all types including new ones
                val allTypes = EnemyType.entries.filter { 
                    it != EnemyType.VOID_TITAN && it != EnemyType.COSMIC_GOD &&
                    it != EnemyType.ENTROPY_HERALD && it != EnemyType.ELDRITCH_HORROR &&
                    !it.name.contains("PRIME") && !it.name.contains("TITAN") &&
                    !it.name.contains("KING") && !it.name.contains("QUEEN") &&
                    !it.name.contains("GOD") && !it.name.contains("ARCHON") &&
                    !it.name.contains("SERPENT") && !it.name.contains("LORD")
                }
                val selection = allTypes.shuffled().take(6)
                val perType = max(1, baseCount / selection.size)
                selection.forEachIndexed { i, type ->
                    enemies.add(EnemySpawnConfig(type, perType, spawnDelayMs = i * 2500L))
                }
            }
        }

        val title = when {
            wave % 3 == 0 -> "CONVERGENCE POINT $wave"
            wave % 7 == 0 -> "VOID SURGE WAVE $wave"
            else          -> "WAVE $wave"
        }

        return WaveConfig(
            waveNumber = wave,
            isBossWave = false,
            enemies = enemies,
            durationMs = (GameConstants.WAVE_DURATION_SEC * 1000L + wave * 2000L),
            title = title
        )
    }

    private fun buildBossWave(wave: Int): WaveConfig {
        val bosses = listOf(
            EnemyType.VOID_TITAN, EnemyType.ENTROPY_HERALD, EnemyType.COSMIC_GOD,
            EnemyType.SINGULARITY_SPAWN, EnemyType.ELDRITCH_HORROR,
            EnemyType.PRIME_SINGULARITY, EnemyType.NEBULA_QUEEN, EnemyType.VOID_ARCHON,
            EnemyType.STAR_EATER_TITAN, EnemyType.CHRONOS_LORD, EnemyType.DIMENSIONAL_GOD,
            EnemyType.OMEGA_PHANTOM, EnemyType.GALAXY_TITAN, EnemyType.COSMIC_SERPENT,
            EnemyType.ENTROPY_KING
        )
        val bossType = bosses.random()

        val addons = when {
            wave > 10 -> listOf(
                EnemySpawnConfig(EnemyType.VOID_CRAWLER, 8, spawnDelayMs = 5000L),
                EnemySpawnConfig(EnemyType.COSMIC_SHADE, 6, spawnDelayMs = 10000L)
            )
            wave > 20 -> listOf(
                EnemySpawnConfig(EnemyType.NEBULA_WRAITH, 8, spawnDelayMs = 5000L),
                EnemySpawnConfig(EnemyType.RIFT_STALKER, 6, spawnDelayMs = 10000L),
                EnemySpawnConfig(EnemyType.ABYSS_KNIGHT, 4, spawnDelayMs = 15000L),
                EnemySpawnConfig(EnemyType.GRAVITY_WELLER, 4, spawnDelayMs = 20000L)
            )
            else -> emptyList()
        }

        val bossName = bossType.name.replace("_", " ")

        return WaveConfig(
            waveNumber = wave,
            isBossWave = true,
            enemies = listOf(EnemySpawnConfig(bossType, 1, isBoss = true, isMainBoss = true)) + addons,
            durationMs = 120_000L,
            title = bossName
        )
    }

    fun startWave(wave: Int, currentTimeMs: Long) {
        currentWave = wave
        waveStartTimeMs = currentTimeMs
        isWaveActive = true
        synchronized(spawnQueue) {
            spawnQueue.clear()
            val config = buildWave(wave)
            config.enemies.forEach { spawnConfig ->
                repeat(spawnConfig.count) { i ->
                    spawnQueue.add(Pair(spawnConfig, i))
                }
            }
            enemiesRemainingToSpawn = spawnQueue.size
        }
        lastSpawnMs = currentTimeMs
    }

    fun getEnemiesToSpawn(currentTimeMs: Long): List<Enemy> {
        val toSpawn = mutableListOf<Enemy>()
        synchronized(spawnQueue) {
            while (spawnQueue.isNotEmpty()) {
                val (config, index) = spawnQueue.first()
                val delay = config.spawnDelayMs + index.toLong() * config.interval
                if (currentTimeMs - waveStartTimeMs >= delay) {
                    spawnQueue.removeFirst()
                    val spawnPos = getSpawnPosition()
                    toSpawn.add(Enemy(
                        type = config.type,
                        isBoss = config.isBoss,
                        wave = currentWave,
                        startPos = spawnPos,
                        isMainBoss = config.isMainBoss
                    ))
                    enemiesRemainingToSpawn = spawnQueue.size
                } else break
            }
        }
        return toSpawn
    }

    private fun getSpawnPosition(): Vector2 {
        val side = Random.nextInt(4)
        val margin = 50f
        return when (side) {
            0 -> Vector2(Random.nextFloat() * screenWidth, -margin)
            1 -> Vector2(screenWidth + margin, Random.nextFloat() * screenHeight)
            2 -> Vector2(Random.nextFloat() * screenWidth, screenHeight + margin)
            else -> Vector2(-margin, Random.nextFloat() * screenHeight)
        }
    }

    fun isWaveComplete(activeEnemies: Int) =
        synchronized(spawnQueue) { spawnQueue.isEmpty() } && activeEnemies == 0

    fun getWaveProgress(currentTimeMs: Long): Float {
        val elapsed = currentTimeMs - waveStartTimeMs
        val config = buildWave(currentWave)
        return (elapsed.toFloat() / config.durationMs).coerceIn(0f, 1f)
    }
}
