package com.voidascension.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.voidascension.R
import com.voidascension.databinding.ActivityMainBinding
import com.voidascension.utils.UIUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var saveManager: com.voidascension.data.SaveManager

    @Inject
    lateinit var cheatManager: com.voidascension.data.CheatManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UIUtils.setupRotatedBackground(binding.ivBackground)

        val isGameOver = intent.getBooleanExtra("GAME_OVER", false)
        if (isGameOver) {
            showGameOverStats()
        }

        setupUI()
        applyAnimations()
        loadBestScore()
    }

    private fun setupUI() {
        binding.btnPlay.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        binding.btnSelectAvatar.setOnClickListener {
            startActivity(Intent(this, AvatarSelectActivity::class.java))
        }
        binding.btnUpgrades.setOnClickListener {
            startActivity(Intent(this, UpgradeActivity::class.java))
        }
        binding.btnPrestige.setOnClickListener {
            showPrestigeConfirmation()
        }
        binding.btnQuit.setOnClickListener { finishAffinity() }
        binding.btnCheats.setOnClickListener { showCheatDialog() }
    }

    private fun showCheatDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cheat, null)
        val etCheatCode = dialogView.findViewById<android.widget.EditText>(R.id.etCheatCode)
        val btnAuth = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAuthenticate)
        val btnAbort = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAbort)

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

        btnAuth.setOnClickListener {
            val code = etCheatCode.text.toString().trim()
            if (code.isNotEmpty()) {
                val response = cheatManager.activateCheat(code, saveManager)
                Toast.makeText(this, response, Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }

        btnAbort.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showPrestigeConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("⚠ NEURAL RESET ⚠")
            .setMessage("All mutations, biological progress, and high scores will be permanently purged. Proceed with reset?")
            .setPositiveButton("CONFIRM") { _, _ ->
                saveManager.prestige()
                Toast.makeText(this, "SYSTEM PURGED", Toast.LENGTH_SHORT).show()
                recreate()
            }
            .setNegativeButton("ABORT", null)
            .show()
    }

    private fun applyAnimations() {
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse)
        binding.tvTitle.startAnimation(pulse)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.cardMenu.startAnimation(slideUp)
    }

    private fun showGameOverStats() {
        val score  = intent.getIntExtra("SCORE", 0)
        val wave   = intent.getIntExtra("WAVE", 1)
        val kills  = intent.getIntExtra("KILLS", 0)
        val shards = intent.getIntExtra("SHARDS", 0)
        
        binding.gameOverPanel.visibility = View.VISIBLE
        binding.tvGameOverScore.text = "SCORE: $score"
        binding.tvGameOverWave.text  = "REACHED WAVE $wave"
        binding.tvGameOverKills.text = "KILLS: $kills"
        
        saveManager.saveHighScore(score, wave, kills)
        if (shards > 0) {
            saveManager.addVoidShards(shards)
            Toast.makeText(this, "Collected $shards Void Shards!", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadBestScore() {
        val best = saveManager.loadHighScore()
        binding.tvBestScore.text = "BEST: ${best.score}"
    }
}
