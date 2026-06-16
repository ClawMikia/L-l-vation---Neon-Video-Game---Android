package com.voidascension.ui

import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.voidascension.R
import com.voidascension.data.SaveManager
import com.voidascension.databinding.ActivityAvatarSelectBinding
import com.voidascension.engine.AvatarDefinitions
import com.voidascension.engine.AvatarShape
import com.voidascension.utils.UIUtils
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class AvatarSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvatarSelectBinding

    @Inject
    lateinit var saveManager: SaveManager

    private val prefixes = listOf("Xeno", "Void", "Neuro", "Cyber", "Astro", "Nova", "Proto", "Echo", "Flux")
    private val suffixes = listOf("Alpha", "Omega", "Prime", "Sigma", "X", "Zero", "Core", "Node", "Vector")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAvatarSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        UIUtils.setupRotatedBackground(binding.ivBackground)
        
        setupAvatarGrid()
        
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupAvatarGrid() {
        binding.avatarGrid.removeAllViews()
        val selectedIdx = saveManager.getSelectedAvatar()
        val inflater = LayoutInflater.from(this)
        
        AvatarDefinitions.AVATARS.forEachIndexed { index, avatarDef ->
            val cardView = inflater.inflate(R.layout.item_avatar_card, binding.avatarGrid, false) as MaterialCardView
            
            val iconContainer = cardView.findViewById<FrameLayout>(R.id.avatarIconContainer)
            val tvCodename = cardView.findViewById<TextView>(R.id.tvCodename)
            val tvShapeName = cardView.findViewById<TextView>(R.id.tvShapeName)
            
            // Random codename based on index
            val rng = Random(index * 1337)
            val codename = "${prefixes[rng.nextInt(prefixes.size)]}-${suffixes[rng.nextInt(suffixes.size)]} ${rng.nextInt(10, 99)}"
            
            tvCodename.text = codename
            tvShapeName.text = avatarDef.shape.name.replace('_', ' ')
            
            val avatarView = AvatarDrawingView(this, avatarDef.shape, avatarDef.color)
            iconContainer.addView(avatarView)
            
            if (index == selectedIdx) {
                cardView.strokeColor = Color.CYAN
                cardView.strokeWidth = (3 * resources.displayMetrics.density).toInt()
                cardView.cardElevation = 20f
            } else {
                cardView.strokeColor = Color.parseColor("#444444")
                cardView.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                cardView.cardElevation = 0f
            }
            
            cardView.setOnClickListener {
                saveManager.setSelectedAvatar(index)
                setupAvatarGrid() // Refresh
            }
            
            binding.avatarGrid.addView(cardView)
        }
    }

    private class AvatarDrawingView(context: android.content.Context, val shape: AvatarShape, val color: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()

        override fun onDraw(canvas: Canvas) {
            val r = width * 0.45f
            val cx = width / 2f
            val cy = height / 2f
            
            paint.color = color
            
            path.reset()
            when (shape) {
                AvatarShape.VOID_CORE -> {
                    path.moveTo(cx, cy - r); path.lineTo(cx + r * 0.2f, cy - r * 0.2f)
                    path.lineTo(cx + r, cy); path.lineTo(cx + r * 0.2f, cy + r * 0.2f)
                    path.lineTo(cx, cy + r); path.lineTo(cx - r * 0.2f, cy + r * 0.2f)
                    path.lineTo(cx - r, cy); path.lineTo(cx - r * 0.2f, cy - r * 0.2f); path.close()
                }
                AvatarShape.NEBULA_EYE -> {
                    path.addOval(cx - r, cy - r * 0.6f, cx + r, cy + r * 0.6f, Path.Direction.CW)
                    path.addCircle(cx, cy, r * 0.35f, Path.Direction.CCW)
                }
                AvatarShape.RIFT_PRISM -> {
                    for (i in 0..7) {
                        val angle = Math.toRadians(i * 45.0)
                        val radius = if (i % 2 == 0) r else r * 0.7f
                        val x = cx + radius * kotlin.math.cos(angle).toFloat()
                        val y = cy + radius * kotlin.math.sin(angle).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                }
                AvatarShape.QUANTUM_CELL -> {
                    for (i in 0..5) {
                        val a = Math.toRadians(i * 60.0)
                        val x = cx + r * kotlin.math.cos(a).toFloat()
                        val y = cy + r * kotlin.math.sin(a).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        val a2 = Math.toRadians(i * 60.0 + 30.0)
                        path.quadTo(cx + r * 0.8f * kotlin.math.cos(a2).toFloat(), cy + r * 0.8f * kotlin.math.sin(a2).toFloat(), cx + r * kotlin.math.cos(a + Math.toRadians(60.0)).toFloat(), cy + r * kotlin.math.sin(a + Math.toRadians(60.0)).toFloat())
                    }
                    path.close()
                }
                AvatarShape.STELLAR_BLADE -> {
                    for (i in 0..2) {
                        val a = Math.toRadians(i * 120.0 - 90.0)
                        val x = cx + r * kotlin.math.cos(a).toFloat()
                        val y = cy + r * kotlin.math.sin(a).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        val a2 = Math.toRadians(i * 120.0 - 30.0)
                        path.lineTo(cx + r * 0.2f * kotlin.math.cos(a2).toFloat(), cy + r * 0.2f * kotlin.math.sin(a2).toFloat())
                    }
                    path.close()
                }
                AvatarShape.ENTROPY_NODE -> {
                    for (i in 0..11) {
                        val radius = if (i % 2 == 0) r else r * 0.4f
                        val a = Math.toRadians(i * 30.0 - 90)
                        val x = cx + radius * kotlin.math.cos(a).toFloat()
                        val y = cy + radius * kotlin.math.sin(a).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                }
                AvatarShape.CHRONO_DISK -> {
                    for (i in 0..15) {
                        val radius = if (i % 4 < 2) r else r * 0.7f
                        val a = Math.toRadians(i * 22.5)
                        val x = cx + radius * kotlin.math.cos(a).toFloat()
                        val y = cy + radius * kotlin.math.sin(a).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                }
                AvatarShape.PLASMA_GHOST -> {
                    for (i in 0..7) {
                        val a = Math.toRadians(i * 45.0)
                        val rad = r * (0.8f + 0.2f * (i % 2).toFloat())
                        val x = cx + rad * kotlin.math.cos(a).toFloat()
                        val y = cy + rad * kotlin.math.sin(a).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.cubicTo(x*1.1f, y*0.9f, x*0.9f, y*1.1f, x, y)
                    }
                    path.close()
                }
                AvatarShape.COSMIC_SHELL -> {
                    path.addArc(cx - r, cy - r, cx + r, cy + r, 20f, 140f)
                    path.lineTo(cx, cy + r)
                    path.close()
                }
                AvatarShape.VECTOR_SOUL -> {
                    path.moveTo(cx, cy - r); path.lineTo(cx + r * 0.5f, cy - r * 0.5f)
                    path.lineTo(cx + r, cy); path.lineTo(cx + r * 0.2f, cy + r * 0.2f)
                    path.lineTo(cx, cy + r * 0.8f); path.lineTo(cx - r * 0.2f, cy + r * 0.2f)
                    path.lineTo(cx - r, cy); path.lineTo(cx - r * 0.5f, cy - r * 0.5f); path.close()
                }
                AvatarShape.SINGULARITY -> {
                    path.addCircle(cx, cy, r, Path.Direction.CW)
                    path.addCircle(cx, cy, r * 0.6f, Path.Direction.CCW)
                    path.addCircle(cx, cy, r * 0.2f, Path.Direction.CW)
                }
            }
            
            // Draw glow
            paint.style = Paint.Style.FILL
            paint.alpha = 50
            canvas.drawCircle(cx, cy, r * 1.3f, paint)
            
            // Draw shell
            paint.alpha = 255
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawPath(path, paint)
            
            // Draw core fill
            paint.style = Paint.Style.FILL
            paint.alpha = 100
            canvas.drawPath(path, paint)
            
            // Central dot
            paint.alpha = 255
            canvas.drawCircle(cx, cy, r * 0.15f, paint)
        }
    }
}
