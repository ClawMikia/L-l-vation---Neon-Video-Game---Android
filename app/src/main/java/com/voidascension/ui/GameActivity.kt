package com.voidascension.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var binding: ActivityGameBinding
    private var engine: GameEngine? = null
    private var lastSnapshotState = GameState.PLAYING

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

        binding.gameRenderer.post { initGame() }
        binding.btnPause.setOnClickListener { togglePause() }
        binding.btnResume.setOnClickListener { resumeGame() }
        binding.btnQuit.setOnClickListener { finish() }
        binding.btnOrientation.setOnClickListener { toggleOrientation() }
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
            onSnapshot = { snap -> handleSnapshot(snap) },
            onGameOver = { score, wave, kills -> handleGameOver(score, wave, kills) },
            onLevelUp = { runOnUiThread { showLevelUpToast() } }
        )

        setupJoysticks()
        setupUpgradeTouchHandler()
        
        // Apply permanent upgrades
        val permanentUpgrades = saveManager.getActiveUpgrades()
        engine!!.player.applyPermanentUpgrades(permanentUpgrades)

        engine!!.start(lifecycleScope)
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
        options.forEach { option ->
            val cardView = inflater.inflate(R.layout.item_upgrade_card, binding.upgradeContainer, false)
            
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
    }

    override fun onPause() {
        super.onPause()
        engine?.pause()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.gameRenderer.post {
            val w = binding.gameRenderer.width.toFloat()
            val h = binding.gameRenderer.height.toFloat()
            engine?.updateScreenSize(w, h)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine?.stop()
    }
}
