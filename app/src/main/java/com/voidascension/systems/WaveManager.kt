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
                // Wave 21+: Mix of all elite types
                val allTypes = listOf(
                    EnemyType.RIFT_STALKER, EnemyType.ABYSS_KNIGHT,
                    EnemyType.SINGULARITY_SPAWN, EnemyType.VOID_TITAN,
                    EnemyType.PHASE_PHANTOM, EnemyType.ENTROPY_HERALD
                )
                val perType = max(1, baseCount / allTypes.size)
                allTypes.forEachIndexed { i, type ->
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
        val bossType = when (wave / GameConstants.BOSS_WAVE_INTERVAL) {
            1    -> EnemyType.VOID_TITAN
            2    -> EnemyType.ENTROPY_HERALD
            3    -> EnemyType.COSMIC_GOD
            4    -> EnemyType.SINGULARITY_SPAWN
            else -> EnemyType.ELDRITCH_HORROR
        }

        val addons = when {
            wave > 10 -> listOf(
                EnemySpawnConfig(EnemyType.VOID_CRAWLER, 6, spawnDelayMs = 5000L),
                EnemySpawnConfig(EnemyType.COSMIC_SHADE, 4, spawnDelayMs = 10000L)
            )
            wave > 20 -> listOf(
                EnemySpawnConfig(EnemyType.NEBULA_WRAITH, 6, spawnDelayMs = 5000L),
                EnemySpawnConfig(EnemyType.RIFT_STALKER, 4, spawnDelayMs = 10000L),
                EnemySpawnConfig(EnemyType.ABYSS_KNIGHT, 3, spawnDelayMs = 15000L)
            )
            else -> emptyList()
        }

        val bossName = when (bossType) {
            EnemyType.VOID_TITAN        -> "THE VOID TITAN"
            EnemyType.ENTROPY_HERALD    -> "HERALD OF ENTROPY"
            EnemyType.COSMIC_GOD        -> "THE COSMIC GOD"
            EnemyType.SINGULARITY_SPAWN -> "SINGULARITY PRIME"
            EnemyType.ELDRITCH_HORROR   -> "ELDRITCH HORROR"
            else                        -> "BOSS ENTITY"
        }

        return WaveConfig(
            waveNumber = wave,
            isBossWave = true,
            enemies = listOf(EnemySpawnConfig(bossType, 1, isBoss = true)) + addons,
            durationMs = 120_000L,
            title = bossName
        )
    }

    fun startWave(wave: Int, currentTimeMs: Long) {
        currentWave = wave
        waveStartTimeMs = currentTimeMs
        isWaveActive = true
        spawnQueue.clear()
        val config = buildWave(wave)
        config.enemies.forEach { spawnConfig ->
            repeat(spawnConfig.count) { i ->
                spawnQueue.add(Pair(spawnConfig, i))
            }
        }
        enemiesRemainingToSpawn = spawnQueue.size
        lastSpawnMs = currentTimeMs
    }

    fun getEnemiesToSpawn(currentTimeMs: Long): List<Enemy> {
        val toSpawn = mutableListOf<Enemy>()
        while (spawnQueue.isNotEmpty()) {
            val (config, index) = spawnQueue.first()
            val delay = config.spawnDelayMs + index.toLong() * config.interval
            if (currentTimeMs - waveStartTimeMs >= delay) {
                spawnQueue.removeFirst()
                val spawnPos = getSpawnPosition()
                toSpawn.add(Enemy(config.type, config.isBoss, currentWave, spawnPos))
                enemiesRemainingToSpawn = spawnQueue.size
            } else break
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
        spawnQueue.isEmpty() && activeEnemies == 0

    fun getWaveProgress(currentTimeMs: Long): Float {
        val elapsed = currentTimeMs - waveStartTimeMs
        val config = buildWave(currentWave)
        return (elapsed.toFloat() / config.durationMs).coerceIn(0f, 1f)
    }
}
