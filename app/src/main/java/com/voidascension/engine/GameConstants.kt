package com.voidascension.engine

object GameConstants {
    // Display
    const val TARGET_FPS = 60L
    const val FRAME_TIME_MS = 1000L / TARGET_FPS

    // Player
    const val PLAYER_BASE_HP = 100f
    const val PLAYER_BASE_SPEED = 200f
    const val PLAYER_BASE_DAMAGE = 15f
    const val PLAYER_BASE_FIRE_RATE = 1.2f
    const val PLAYER_RADIUS = 9f
    const val PLAYER_INVINCIBILITY_MS = 800L

    // XP & Leveling
    const val XP_BASE = 100
    const val XP_MULTIPLIER = 1.35f
    const val MAX_LEVEL = 99

    // Wave
    const val WAVE_BASE_ENEMIES = 8
    const val WAVE_ENEMY_SCALE = 1.18f
    const val WAVE_DURATION_SEC = 30
    const val BOSS_WAVE_INTERVAL = 5

    // Enemy
    const val ENEMY_BASE_HP = 30f
    const val ENEMY_BASE_DAMAGE = 8f
    const val ENEMY_BASE_SPEED = 90f
    const val ENEMY_HP_SCALE = 1.12f
    const val ENEMY_DAMAGE_SCALE = 1.08f
    const val ENEMY_RADIUS = 22f

    // Loot
    val LOOT_DROP_RATES = mapOf(
        LootRarity.COMMON    to 0.55f,
        LootRarity.UNCOMMON  to 0.25f,
        LootRarity.RARE      to 0.12f,
        LootRarity.EPIC      to 0.06f,
        LootRarity.LEGENDARY to 0.02f
    )

    // Particle
    const val MAX_PARTICLES = 400
    const val PARTICLE_LIFETIME_MS = 800L

    // Screen shake
    const val SHAKE_DURATION_MS = 300L
    const val SHAKE_AMPLITUDE = 18f

    // Aura
    const val AURA_RADIUS = 120f
    const val AURA_TICK_MS = 200L

    // Auto-attack
    const val PROJECTILE_SPEED = 480f
    const val PROJECTILE_RADIUS = 8f

    // Save
    const val SAVE_SLOT_COUNT = 3
}

enum class LootRarity(val color: Int, val label: String) {
    COMMON   (0xFFAAAAAA.toInt(), "Common"),
    UNCOMMON (0xFF55FF55.toInt(), "Uncommon"),
    RARE     (0xFF5599FF.toInt(), "Rare"),
    EPIC     (0xFFAA44FF.toInt(), "Epic"),
    LEGENDARY(0xFFFFAA00.toInt(), "Legendary")
}

enum class EnemyType {
    VOID_CRAWLER, COSMIC_SHADE, NEBULA_WRAITH,
    STAR_DEVOURER, RIFT_STALKER, VOID_TITAN,
    COSMIC_GOD, ENTROPY_HERALD, SINGULARITY_SPAWN,
    ABYSS_KNIGHT, PHASE_PHANTOM, ELDRITCH_HORROR,
    // New Enemies
    GRAVITY_WELLER, SHADOW_STALKER, PLASMA_REAPAR,
    NEURAL_HIVE, DIMENSION_RIPPER, QUARK_GLUTTON,
    PHOTON_WRAITH, CHRONO_EATER, PULSE_WITCH,
    ANTIMATTER_CORE, NEUTRON_BEAST, SOLAR_FLARE,
    VOID_EEL, COMET_CRASH, ASTEROID_GOLEM,
    GALAXY_EATER, NEBULA_DRAGON, DARK_ENERGY_WISP,
    QUANTUM_GOLEM, SINGULARITY_SEED,
    // New Bosses/Titans
    PRIME_SINGULARITY, NEBULA_QUEEN, VOID_ARCHON,
    STAR_EATER_TITAN, CHRONOS_LORD, DIMENSIONAL_GOD,
    OMEGA_PHANTOM, GALAXY_TITAN, COSMIC_SERPENT,
    ENTROPY_KING
}

enum class WeaponType {
    PLASMA_RIFLE, VOID_CANNON, NEURAL_LANCE,
    QUANTUM_BURST, COSMIC_SCYTHE, ENTROPY_BEAM,
    PHASE_BLADE, SINGULARITY_GUN, NEUROTOXIN_SPRAYER
}

enum class MutationType {
    IRON_SKIN, NEURAL_BOOST, VOID_STEP,
    QUANTUM_HEART, CYBER_REFLEXES, PLASMA_VEINS,
    PHASE_SHIFT, ENTROPY_TOUCH, COSMIC_SIGHT,
    TWIN_BARRELS, EXPLOSIVE_ROUNDS, LIFE_STEAL
}

enum class AuraType {
    PLASMA_SHIELD, VOID_DRAIN, STATIC_FIELD,
    QUANTUM_ECHO, ENTROPIC_PULSE, NEUROSTATIC_WAVE,
    // New Player Auras
    REGEN_CORE, GRAVITY_FIELD, BLAZING_AURA, HOLY_BARRIER,
    // New Attack Auras
    FROST_AURA, CHAIN_LIGHTNING, CORROSIVE_AURA, TITAN_SLAYER
}

enum class GameState {
    MENU, PLAYING, PAUSED, UPGRADE_SELECT,
    BOSS_INTRO, GAME_OVER, VICTORY
}
