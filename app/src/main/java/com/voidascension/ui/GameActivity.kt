package com.voidascension.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.lifecycleScope
import com.voidascension.R
import com.voidascension.data.SaveManager
import com.voidascension.databinding.ActivityGameBinding
import com.voidascension.engine.*
import com.voidascension.systems.UpgradeOption
import com.voidascension.utils.UIUtils
import com.voidascension.utils.Vector2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class GameActivity : AppCompatActivity() {

    @Inject lateinit var saveManager: SaveManager
    @Inject lateinit var cheatManager: com.voidascension.data.CheatManager
    @Inject lateinit var audioManager: com.voidascension.utils.AudioManager
    private lateinit var binding: ActivityGameBinding
    private var engine: GameEngine? = null
    private var lastSnapshotState = GameState.PLAYING
    private var shardsSavedThisRun = 0

    private fun saveShardsPermanently() {
        val currentShards = engine?.player?.collectedVoidShards ?: 0
        val newShards = currentShards - shardsSavedThisRun
        if (newShards > 0) {
            saveManager.addVoidShards(newShards)
            shardsSavedThisRun = currentShards
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adjustLayoutForOrientation(resources.configuration.orientation)

        binding.gameRenderer.post { initGame() }
        binding.btnPause.setOnClickListener {
            audioManager.playMenuClick()
            togglePause()
        }
        binding.btnResume.setOnClickListener {
            audioManager.playMenuClick()
            resumeGame()
        }
        binding.btnSettings.setOnClickListener {
            audioManager.playMenuClick()
            showSettingsDialog()
        }
        binding.btnQuit.setOnClickListener {
            audioManager.playMenuClick()
            finish()
        }
        binding.btnOrientation.setOnClickListener {
            audioManager.playMenuClick()
            toggleOrientation()
        }
    }

    private fun toggleOrientation() {
        val currentOrientation = resources.configuration.orientation
        requestedOrientation = if (currentOrientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun initGame() {
        val w = binding.gameRenderer.width.toFloat()
        val h = binding.gameRenderer.height.toFloat()

        engine = GameEngine(
            screenWidth = w,
            screenHeight = h,
            avatarIndex = saveManager.getSelectedAvatar(),
            audioManager = audioManager,
            onSnapshot = { snap -> handleSnapshot(snap) },
            onGameOver = { score, wave, kills -> handleGameOver(score, wave, kills) },
            onLevelUp = { runOnUiThread { showLevelUpToast() } }
        )

        setupJoysticks()
        setupUpgradeTouchHandler()
        
        // Apply permanent upgrades
        val permanentUpgrades = saveManager.getActiveUpgrades()
        engine!!.player.applyPermanentUpgrades(permanentUpgrades)

        applyCheats()

        engine!!.start(lifecycleScope)
    }

    private fun applyCheats() {
        val p = engine!!.player
        if (cheatManager.isLaFierceActive) {
            p.extraLives = 25
        }
        if (cheatManager.isMauragPoAkoActive) {
            // Max stats & all mutations
            p.maxHp += 1000f
            p.currentHp = p.maxHp
            p.damageMultiplier += 5f
            p.fireRateMultiplier += 2f
            p.speedMultiplier += 1f
            MutationType.entries.forEach { p.applyMutation(it) }
            AuraType.entries.forEach { p.addAura(it) }
            p.auras.forEach { it.level = 10 }
        }
        if (cheatManager.isAikiActive) {
            p.isAikiActive = true
        }
        if (cheatManager.isMochiMochiActive) {
            p.isMochiMochiActive = true
        }
        if (cheatManager.isMotomemashitaActive) {
            p.isMotomemashitaActive = true
        }
        
        // Reset them for next run as they are "one gameplay only"
        cheatManager.resetTemporaryCheats()
    }

    private fun setupJoysticks() {
        binding.joystickMove.onMove = { delta ->
            engine?.joystickDelta = delta
        }
        binding.joystickFire.onMove = { delta ->
            engine?.fireJoystickDelta = delta
        }
    }

    private fun setupUpgradeTouchHandler() {
        // No longer needed, handled by View.OnClickListener
    }

    private fun showUpgradeSelection(options: List<UpgradeOption>) {
        binding.upgradeOverlay.visibility = View.VISIBLE
        binding.upgradeContainer.removeAllViews()

        val inflater = LayoutInflater.from(this)
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        binding.upgradeContainer.orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        
        options.forEach { option ->
            val cardView = inflater.inflate(R.layout.item_upgrade_card, binding.upgradeContainer, false)
            
            val params = cardView.layoutParams as LinearLayout.LayoutParams
            if (isLandscape) {
                params.width = 0
                params.height = LinearLayout.LayoutParams.WRAP_CONTENT
                params.weight = 1f
                params.marginEnd = (resources.displayMetrics.density * 16).toInt()
                params.bottomMargin = 0
            } else {
                params.width = LinearLayout.LayoutParams.MATCH_PARENT
                params.height = LinearLayout.LayoutParams.WRAP_CONTENT
                params.weight = 0f
                params.marginEnd = 0
                params.bottomMargin = (resources.displayMetrics.density * 16).toInt()
            }
            cardView.layoutParams = params

            val tvRarity: TextView = cardView.findViewById(R.id.tvRarity)
            val tvName: TextView = cardView.findViewById(R.id.tvName)
            val tvDescription: TextView = cardView.findViewById(R.id.tvDescription)
            val tvType: TextView = cardView.findViewById(R.id.tvType)

            tvRarity.text = option.rarity.label.uppercase()
            tvRarity.setBackgroundColor(option.rarity.color)
            tvName.text = option.name
            tvDescription.text = option.description
            tvType.text = "[${option.type.name}]"

            cardView.setOnClickListener {
                engine?.selectUpgrade(option)
                binding.upgradeOverlay.visibility = View.GONE
            }

            binding.upgradeContainer.addView(cardView)
        }
    }

    private var lastUpgradeOptions: List<UpgradeOption> = emptyList()

    private fun handleSnapshot(snap: GameSnapshot) {
        binding.gameRenderer.render(snap)

        if (snap.gameState != lastSnapshotState) {
            runOnUiThread {
                if (snap.gameState != lastSnapshotState) {
                    lastSnapshotState = snap.gameState
                    lastUpgradeOptions = snap.upgradeOptions
                    when (snap.gameState) {
                        GameState.UPGRADE_SELECT -> {
                            showUpgradeSelection(snap.upgradeOptions)
                        }
                        GameState.PLAYING -> {
                            binding.upgradeOverlay.visibility = View.GONE
                            binding.pauseMenu.visibility = View.GONE
                        }
                        GameState.PAUSED -> {
                            binding.pauseMenu.visibility = View.VISIBLE
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun handleGameOver(score: Int, wave: Int, kills: Int) {
        runOnUiThread {
            engine?.stop()
            val intent = android.content.Intent(this, MainActivity::class.java).apply {
                putExtra("GAME_OVER", true)
                putExtra("SCORE", score)
                putExtra("WAVE", wave)
                putExtra("KILLS", kills)
                putExtra("SHARDS", engine?.player?.collectedVoidShards ?: 0)
                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun showLevelUpToast() {
        Toast.makeText(this, "⬆ LEVEL UP!", Toast.LENGTH_SHORT).show()
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val sbBgm = dialogView.findViewById<android.widget.SeekBar>(R.id.sbBgm)
        val sbSfx = dialogView.findViewById<android.widget.SeekBar>(R.id.sbSfx)
        val swMute = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swMute)
        val btnClose = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClose)

        sbBgm.progress = (audioManager.getBgmVolume() * 100).toInt()
        sbSfx.progress = (audioManager.getSfxVolume() * 100).toInt()
        swMute.isChecked = audioManager.isMuted()

        val dialog = android.app.Dialog(this)
        dialog.setContentView(dialogView)
        dialog.window?.let { window ->
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.setLayout(
                (resources.displayMetrics.density * 320).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window.setGravity(android.view.Gravity.CENTER)
        }

        sbBgm.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: android.widget.SeekBar?, p1: Int, p2: Boolean) {
                audioManager.setBgmVolume(p1 / 100f)
            }
            override fun onStartTrackingTouch(p0: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(p0: android.widget.SeekBar?) {}
        })

        sbSfx.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: android.widget.SeekBar?, p1: Int, p2: Boolean) {
                audioManager.setSfxVolume(p1 / 100f)
            }
            override fun onStartTrackingTouch(p0: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(p0: android.widget.SeekBar?) {
                audioManager.playRandomKillSound()
            }
        })

        swMute.setOnCheckedChangeListener { _, isChecked ->
            audioManager.setMuted(isChecked)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun togglePause() {
        val eng = engine ?: return
        if (eng.gameState == GameState.PLAYING) {
            eng.pause()
            binding.pauseMenu.visibility = View.VISIBLE
        }
    }

    private fun resumeGame() {
        engine?.resume()
        binding.pauseMenu.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        engine?.resume()
        audioManager.startBgm("game")
    }

    override fun onPause() {
        super.onPause()
        engine?.pause()
        saveShardsPermanently()
        audioManager.stopBgm()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustLayoutForOrientation(newConfig.orientation)
        binding.gameRenderer.post {
            val w = binding.gameRenderer.width.toFloat()
            val h = binding.gameRenderer.height.toFloat()
            engine?.updateScreenSize(w, h)
        }
    }

    private fun adjustLayoutForOrientation(orientation: Int) {
        val isLandscape = orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.root as ConstraintLayout)

        if (isLandscape) {
            // Pull HUD elements in slightly more in landscape to keep them reachable
            constraintSet.setGuidelinePercent(R.id.glLeft, 0.08f)
            constraintSet.setGuidelinePercent(R.id.glRight, 0.92f)
            constraintSet.setGuidelinePercent(R.id.glBottom, 0.92f)
            
            // Adjust Joysticks for landscape
            constraintSet.constrainPercentWidth(R.id.joystickMove, 0.15f)
            constraintSet.constrainPercentWidth(R.id.joystickFire, 0.15f)
        } else {
            // Standard portrait guidelines
            constraintSet.setGuidelinePercent(R.id.glLeft, 0.05f)
            constraintSet.setGuidelinePercent(R.id.glRight, 0.95f)
            constraintSet.setGuidelinePercent(R.id.glBottom, 0.95f)
            
            constraintSet.constrainPercentWidth(R.id.joystickMove, 0.18f)
            constraintSet.constrainPercentWidth(R.id.joystickFire, 0.18f)
        }
        
        constraintSet.applyTo(binding.root as androidx.constraintlayout.widget.ConstraintLayout)
        
        // Update upgrade container if it's currently showing
        if (binding.upgradeOverlay.visibility == View.VISIBLE) {
            showUpgradeSelection(lastUpgradeOptions)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        saveShardsPermanently()
        engine?.stop()
    }
}
