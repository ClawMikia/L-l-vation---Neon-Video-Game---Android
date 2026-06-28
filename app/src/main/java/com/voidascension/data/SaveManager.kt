package com.voidascension.data

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// ─── Entities ─────────────────────────────────────────────────────────────────
@Entity(tableName = "high_scores")
data class HighScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val wave: Int,
    val kills: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "permanent_upgrades")
data class PermanentUpgradeEntity(
    @PrimaryKey val upgradeId: String,
    val level: Int = 0
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────
@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 1")
    suspend fun getBest(): HighScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HighScoreEntity)

    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 10")
    suspend fun getTop10(): List<HighScoreEntity>
}

@Dao
interface PermanentUpgradeDao {
    @Query("SELECT * FROM permanent_upgrades")
    suspend fun getAll(): List<PermanentUpgradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PermanentUpgradeEntity)

    @Query("SELECT * FROM permanent_upgrades WHERE upgradeId = :id LIMIT 1")
    suspend fun get(id: String): PermanentUpgradeEntity?
}

// ─── Database ─────────────────────────────────────────────────────────────────
@Database(
    entities = [HighScoreEntity::class, PermanentUpgradeEntity::class],
    version = 2,
    exportSchema = false
)
abstract class VoidDatabase : RoomDatabase() {
    abstract fun highScoreDao(): HighScoreDao
    abstract fun permanentUpgradeDao(): PermanentUpgradeDao
}

// ─── Permanent Upgrades Data ──────────────────────────────────────────────────
data class PermanentUpgrade(
    val id: String,
    val name: String,
    val description: String,
    val baseCost: Int,
    val level: Int = 0,
    val maxLevel: Int = 10,
    val isEpic: Boolean = false
) {
    val cost: Int get() = if (isEpic) baseCost else (baseCost * Math.pow(1.6, level.toDouble())).toInt()
}

data class BestScore(val score: Int, val wave: Int, val kills: Int)

// ─── Save Manager (sync wrapper for UI) ───────────────────────────────────────
@Singleton
class SaveManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: VoidDatabase
) {
    private val prefs = context.getSharedPreferences("void_save", Context.MODE_PRIVATE)

    private val permanentUpgradesCatalog = listOf(
        PermanentUpgrade("start_hp",    "Reinforced Frame",    "+20 HP per level",          50),
        PermanentUpgrade("start_dmg",   "Neural Amplifier",    "+10% Damage per level",     80),
        PermanentUpgrade("start_spd",   "Phase Boots",         "+10% Speed per level",      60),
        PermanentUpgrade("xp_bonus",    "Cosmic Awareness",    "+20% XP gain per level",    100),
        PermanentUpgrade("boss_dmg",    "Titan Slayer",        "+15% Boss Damage per level",120),
        PermanentUpgrade("extra_proj",  "Twin Core",           "+1 Projectile (Lv.5, 10)",  500, maxLevel = 2),
        PermanentUpgrade("life_steal",  "Vampiric Nanites",    "+2% Life Steal per level",  150),
        PermanentUpgrade("crit_start",  "Precision Implant",   "+5% Crit Chance per level", 120),
        PermanentUpgrade("fire_rate",   "Overdrive Module",    "+10% Fire Rate per level",  180),

        // Epic Upgrades
        PermanentUpgrade("epic_kokey", "Kokey", "1 shot, 1 kill (except bosses)", 8000, maxLevel = 1, isEpic = true),
        PermanentUpgrade("epic_kekay", "Kekay", "Vaccum / Magnets all nearby loots", 8000, maxLevel = 1, isEpic = true),
        PermanentUpgrade("epic_umamay", "Umamay Kakay", "Extra life when killed", 8000, maxLevel = 1, isEpic = true),
        PermanentUpgrade("epic_kokoy", "Kokoy", "All enemies are confused where target is", 8000, maxLevel = 1, isEpic = true),
        PermanentUpgrade("epic_sukhoi", "Sukhoi SU-47 Berkut", "Homing / following shots", 8000, maxLevel = 1, isEpic = true),
        PermanentUpgrade("epic_king_cone", "King Cone", "Ultimate overall aura / mutation", 8000, maxLevel = 1, isEpic = true),
        PermanentUpgrade("epic_bebot", "Bebot & Babet", "Twin player look-alikes helping you", 8000, maxLevel = 1, isEpic = true),
        PermanentUpgrade("epic_korokoy", "Korokoy", "Shockwave destroys wave & jumps ahead", 8000, maxLevel = 1, isEpic = true),
        PermanentUpgrade("epic_maurag", "Maurag Supermacy", "All mutations are now acquired", 15000, maxLevel = 1, isEpic = true)
    )

    fun getVoidShards(): Int = prefs.getInt("void_shards_total", 0)

    fun addVoidShards(amount: Int) {
        val total = getVoidShards() + amount
        prefs.edit().putInt("void_shards_total", total).apply()
    }

    fun spendVoidShards(amount: Int): Boolean {
        val total = getVoidShards()
        if (total >= amount) {
            prefs.edit().putInt("void_shards_total", total - amount).apply()
            return true
        }
        return false
    }

    fun saveHighScore(score: Int, wave: Int, kills: Int) {
        val current = loadHighScore()
        if (score > current.score) {
            prefs.edit()
                .putInt("best_score", score)
                .putInt("best_wave", wave)
                .putInt("best_kills", kills)
                .apply()
        }
    }

    fun loadHighScore(): BestScore {
        return BestScore(
            score = prefs.getInt("best_score", 0),
            wave  = prefs.getInt("best_wave", 0),
            kills = prefs.getInt("best_kills", 0)
        )
    }

    fun purchaseUpgrade(id: String) {
        val level = getUpgradeLevel(id)
        prefs.edit().putInt("upg_lv_$id", level + 1).apply()
    }

    fun getUpgradeLevel(id: String): Int = prefs.getInt("upg_lv_$id", 0)

    fun loadPermanentUpgrades(): List<PermanentUpgrade> {
        return permanentUpgradesCatalog.map { upg ->
            upg.copy(level = getUpgradeLevel(upg.id))
        }
    }

    fun getActiveUpgrades(): List<PermanentUpgrade> {
        return permanentUpgradesCatalog.mapNotNull { upg ->
            val lv = getUpgradeLevel(upg.id)
            if (lv > 0) upg.copy(level = lv) else null
        }
    }

    fun saveGameState(playerJson: String) {
        prefs.edit().putString("saved_game", playerJson).apply()
    }

    fun loadGameState(): String? = prefs.getString("saved_game", null)
    fun clearGameState() { prefs.edit().remove("saved_game").apply() }

    fun getSelectedAvatar(): Int = prefs.getInt("selected_avatar", 0)
    fun setSelectedAvatar(index: Int) {
        prefs.edit().putInt("selected_avatar", index).apply()
    }

    fun prestige() {
        prefs.edit().clear().apply()
        // Run database clearing in a background thread if possible, or just use clearAllTables
        Thread {
            db.clearAllTables()
        }.start()
    }
}
