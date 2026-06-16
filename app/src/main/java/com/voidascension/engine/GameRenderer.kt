package com.voidascension.engine

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.voidascension.entities.*
import com.voidascension.systems.UpgradeOption
import com.voidascension.utils.Vector2
import kotlin.math.*

class GameRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val enemyPaths = mutableMapOf<EnemyType, Path>()
    private val avatarPaths = mutableMapOf<AvatarShape, Path>()
    private var backgroundBitmap: Bitmap? = null

    // Cyberpunk color palette
    private val COLOR_CYAN    = 0xFF00FFFF.toInt()
    private val COLOR_MAGENTA = 0xFFFF00FF.toInt()
    private val COLOR_PURPLE  = 0xFF8800FF.toInt()
    private val COLOR_DARK_BG = 0xFF080B14.toInt()
    private val COLOR_PLAYER  = 0xFF00FFFF.toInt()
    private val COLOR_HP_BAR  = 0xFF00FF44.toInt()
    private val COLOR_XP_BAR  = 0xFF00AAFF.toInt()

    private var snapshot: GameSnapshot? = null
    private var time = 0f

    init {
        holder.addCallback(this)
        setZOrderOnTop(false)
        textPaint.typeface = Typeface.MONOSPACE
        precomputePaths()
        loadBackground()
    }

    private fun loadBackground() {
        try {
            backgroundBitmap = BitmapFactory.decodeResource(resources, com.voidascension.R.drawable.background)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun precomputePaths() {
        // Pre-compute normalized paths (size 1.0) for each enemy type
        EnemyType.entries.forEach { type ->
            val path = Path()
            when (type) {
                EnemyType.VOID_CRAWLER -> {
                    val spikes = 6
                    for (i in 0 until spikes * 2) {
                        val angle = i * PI.toFloat() / spikes
                        val rad = if (i % 2 == 0) 1f else 0.55f
                        val px = cos(angle) * rad
                        val py = sin(angle) * rad
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                }
                EnemyType.PHASE_PHANTOM -> {
                    path.moveTo(0f, -1f)
                    path.lineTo(0.7f, 0f)
                    path.lineTo(0f, 1f)
                    path.lineTo(-0.7f, 0f)
                    path.close()
                }
                EnemyType.VOID_TITAN, EnemyType.ELDRITCH_HORROR, EnemyType.COSMIC_GOD -> {
                    for (i in 0..5) {
                        val angle = i * PI.toFloat() / 3f
                        val px = cos(angle)
                        val py = sin(angle)
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                }
                else -> {
                    path.addCircle(0f, 0f, 1f, Path.Direction.CW)
                }
            }
            enemyPaths[type] = path
        }

        // Pre-compute avatar shapes
        AvatarShape.entries.forEach { shape ->
            val path = Path()
            when (shape) {
                AvatarShape.VOID_CORE -> {
                    // Spiky Diamond
                    path.moveTo(0f, -1f)
                    path.lineTo(0.2f, -0.2f)
                    path.lineTo(1f, 0f)
                    path.lineTo(0.2f, 0.2f)
                    path.lineTo(0f, 1f)
                    path.lineTo(-0.2f, 0.2f)
                    path.lineTo(-1f, 0f)
                    path.lineTo(-0.2f, -0.2f)
                    path.close()
                }
                AvatarShape.NEBULA_EYE -> {
                    // Eye with curved iris
                    path.addOval(-1f, -0.6f, 1f, 0.6f, Path.Direction.CW)
                    path.addCircle(0f, 0f, 0.35f, Path.Direction.CCW)
                }
                AvatarShape.RIFT_PRISM -> {
                    // Double Square / Octagram
                    for (i in 0..7) {
                        val angle = Math.toRadians(i * 45.0)
                        val r = if (i % 2 == 0) 1f else 0.7f
                        val x = cos(angle).toFloat() * r
                        val y = sin(angle).toFloat() * r
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                }
                AvatarShape.QUANTUM_CELL -> {
                    // Bio-mechanical Hexagon
                    for (i in 0..5) {
                        val a = Math.toRadians(i * 60.0)
                        val x1 = cos(a).toFloat(); val y1 = sin(a).toFloat()
                        if (i == 0) path.moveTo(x1, y1) else path.lineTo(x1, y1)
                        val a2 = Math.toRadians(i * 60.0 + 30.0)
                        path.quadTo(cos(a2).toFloat() * 0.8f, sin(a2).toFloat() * 0.8f, cos(a + Math.toRadians(60.0)).toFloat(), sin(a + Math.toRadians(60.0)).toFloat())
                    }
                    path.close()
                }
                AvatarShape.STELLAR_BLADE -> {
                    // Three-pointed glaive
                    for (i in 0..2) {
                        val a = Math.toRadians(i * 120.0 - 90.0)
                        val x = cos(a).toFloat(); val y = sin(a).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        val a2 = Math.toRadians(i * 120.0 - 30.0)
                        path.lineTo(cos(a2).toFloat() * 0.2f, sin(a2).toFloat() * 0.2f)
                    }
                    path.close()
                }
                AvatarShape.ENTROPY_NODE -> {
                    // Fractal Star
                    for (i in 0..11) {
                        val r = if (i % 2 == 0) 1f else 0.4f
                        val a = Math.toRadians(i * 30.0 - 90)
                        val px = r * cos(a).toFloat(); val py = r * sin(a).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                }
                AvatarShape.CHRONO_DISK -> {
                    // Cog-like disk
                    for (i in 0..15) {
                        val r = if (i % 4 < 2) 1f else 0.7f
                        val a = Math.toRadians(i * 22.5)
                        val x = cos(a).toFloat() * r
                        val y = sin(a).toFloat() * r
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                }
                AvatarShape.PLASMA_GHOST -> {
                    // Organic blob
                    for (i in 0..7) {
                        val a = Math.toRadians(i * 45.0)
                        val r = 0.8f + 0.2f * (i % 2).toFloat()
                        val x = cos(a).toFloat() * r
                        val y = sin(a).toFloat() * r
                        if (i == 0) path.moveTo(x, y) else path.cubicTo(x*1.2f, y*0.8f, x*0.8f, y*1.2f, x, y)
                    }
                    path.close()
                }
                AvatarShape.COSMIC_SHELL -> {
                    // Shield with inner detail
                    path.addArc(-1f, -1f, 1f, 1f, 20f, 140f)
                    path.lineTo(0f, 1f)
                    path.close()
                }
                AvatarShape.VECTOR_SOUL -> {
                    // Winged glyph
                    path.moveTo(0f, -1f)
                    path.lineTo(0.5f, -0.5f)
                    path.lineTo(1f, 0f)
                    path.lineTo(0.2f, 0.2f)
                    path.lineTo(0f, 0.8f)
                    path.lineTo(-0.2f, 0.2f)
                    path.lineTo(-1f, 0f)
                    path.lineTo(-0.5f, -0.5f)
                    path.close()
                }
                AvatarShape.SINGULARITY -> {
                    // Spiral-ish
                    path.addCircle(0f, 0f, 1f, Path.Direction.CW)
                    path.addCircle(0f, 0f, 0.6f, Path.Direction.CCW)
                    path.addCircle(0f, 0f, 0.2f, Path.Direction.CW)
                }
            }
            avatarPaths[shape] = path
        }
    }

    override fun surfaceCreated(h: SurfaceHolder) {}
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, h2: Int) {}
    override fun surfaceDestroyed(h: SurfaceHolder) {}

    fun render(snap: GameSnapshot) {
        snapshot = snap
        time += 0.016f

        val canvas = holder.lockCanvas() ?: return
        try {
            drawBackground(canvas)
            
            val shake = snap.screenShake
            if (shake != null) canvas.translate(shake.offsetX, shake.offsetY)

            drawLootItems(canvas, snap)
            drawAuras(canvas, snap)
            drawEnemies(canvas, snap)
            drawEnemyProjectiles(canvas, snap)
            drawProjectiles(canvas, snap)
            drawPlayer(canvas, snap)
            drawParticles(canvas, snap)
            drawFloatingTexts(canvas, snap)
            drawHUD(canvas, snap)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val bg = backgroundBitmap
        if (bg != null) {
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            val bmpWidth = bg.width.toFloat()
            val bmpHeight = bg.height.toFloat()

            val scale = max(viewWidth / bmpWidth, viewHeight / bmpHeight)
            val dx = (viewWidth - bmpWidth * scale) / 2f
            val dy = (viewHeight - bmpHeight * scale) / 2f

            val matrix = Matrix()
            matrix.postScale(scale, scale)
            matrix.postTranslate(dx, dy)
            canvas.drawBitmap(bg, matrix, paint)
        } else {
            canvas.drawColor(0xFF080B14.toInt())
        }
    }

    // ─── Auras ────────────────────────────────────────────────────────────────
    private fun drawAuras(canvas: Canvas, snap: GameSnapshot) {
        val player = snap.player
        val cx = player.position.x
        val cy = player.position.y
        for (aura in player.auras) {
            val auraRadius = GameConstants.AURA_RADIUS * player.auraMultiplier * (1f + aura.level * 0.15f)
            val color = when (aura.type) {
                com.voidascension.engine.AuraType.PLASMA_SHIELD    -> 0xFF0088FF.toInt()
                com.voidascension.engine.AuraType.VOID_DRAIN       -> 0xFF8800FF.toInt()
                com.voidascension.engine.AuraType.STATIC_FIELD     -> 0xFFFFFF00.toInt()
                com.voidascension.engine.AuraType.QUANTUM_ECHO     -> 0xFF00FFFF.toInt()
                com.voidascension.engine.AuraType.ENTROPIC_PULSE   -> 0xFFFF8800.toInt()
                com.voidascension.engine.AuraType.NEUROSTATIC_WAVE -> 0xFF00FF88.toInt()
            }
            val pulse = (sin(time * 3f + aura.level) * 0.15f + 0.85f)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = color
            paint.alpha = (80 * pulse).toInt()
            canvas.drawCircle(cx, cy, auraRadius * pulse, paint)
            paint.alpha = (40 * pulse).toInt()
            canvas.drawCircle(cx, cy, auraRadius * 0.7f * pulse, paint)
            paint.alpha = 255
        }
    }

    // ─── Loot ─────────────────────────────────────────────────────────────────
    private fun drawLootItems(canvas: Canvas, snap: GameSnapshot) {
        for (loot in snap.lootItems) {
            val pulse = (sin(time * 4f + loot.pulsePhase) * 0.2f + 0.8f)
            val r = 14f * pulse

            // Glow
            paint.style = Paint.Style.FILL
            paint.color = loot.rarity.color
            paint.alpha = 60
            canvas.drawCircle(loot.position.x, loot.position.y, r * 2.2f, paint)

            // Core
            paint.alpha = 220
            canvas.drawCircle(loot.position.x, loot.position.y, r, paint)

            // Rarity ring
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.alpha = 255
            canvas.drawCircle(loot.position.x, loot.position.y, r + 4f, paint)

            // Label
            textPaint.color = loot.rarity.color
            textPaint.textSize = 18f
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.alpha = (200 * pulse).toInt()
            canvas.drawText(loot.name.take(10), loot.position.x, loot.position.y - r - 8f, textPaint)
            textPaint.alpha = 255
        }
    }

    // ─── Enemies ──────────────────────────────────────────────────────────────
    private fun drawEnemies(canvas: Canvas, snap: GameSnapshot) {
        for (enemy in snap.enemies) {
            if (!enemy.isAlive) continue
            val alpha = if (enemy.aiState == EnemyAIState.SPAWNING)
                (enemy.spawnProgress * 255).toInt() else 255

            val cx = enemy.position.x
            val cy = enemy.position.y
            val r  = enemy.radius

            val baseColor = getEnemyColor(enemy)

            // Glow
            paint.style = Paint.Style.FILL
            paint.color = baseColor
            paint.alpha = (60 * (if (enemy.isEnraged) 1.5f else 1f)).toInt().coerceAtMost(120)
            canvas.drawCircle(cx, cy, r * 1.8f, paint)

            // Body
            paint.alpha = alpha
            canvas.save()
            canvas.rotate(enemy.rotationAngle, cx, cy)
            drawEnemyShape(canvas, enemy, cx, cy, r, baseColor, alpha)
            canvas.restore()

            // HP bar (above enemy)
            if (enemy.currentHp < enemy.maxHp) {
                val barW = r * 2.4f
                val barH = 5f
                val bx = cx - barW / 2f
                val by = cy - r - 14f
                paint.style = Paint.Style.FILL
                paint.color = 0xFF220000.toInt(); paint.alpha = 200
                canvas.drawRoundRect(bx, by, bx + barW, by + barH, 2f, 2f, paint)
                paint.color = if (enemy.isEnraged) 0xFFFF4400.toInt() else 0xFFFF0044.toInt()
                paint.alpha = 255
                canvas.drawRoundRect(bx, by, bx + barW * enemy.hpPercent(), by + barH, 2f, 2f, paint)
            }

            // Boss: phase & name label
            if (enemy.isBoss) {
                textPaint.color = 0xFFFF00FF.toInt()
                textPaint.textSize = 22f
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(enemy.type.name.replace('_', ' '), cx, cy - r - 22f, textPaint)
                textPaint.textSize = 18f
                textPaint.color = 0xFFAAAAAA.toInt()
                canvas.drawText("PHASE ${enemy.phase}", cx, cy - r - 40f, textPaint)
            }
        }
    }

    private fun drawEnemyShape(canvas: Canvas, enemy: Enemy, cx: Float, cy: Float, r: Float, color: Int, alpha: Int) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = alpha

        val path = enemyPaths[enemy.type] ?: enemyPaths[EnemyType.VOID_CRAWLER]!!
        
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(enemy.rotationAngle)
        canvas.scale(r, r)
        
        canvas.drawPath(path, paint)
        
        if (enemy.type == EnemyType.PHASE_PHANTOM) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f / r // Compensate for scale
            paint.alpha = alpha
            canvas.drawPath(path, paint)
        } else if (enemy.type == EnemyType.VOID_TITAN || enemy.type == EnemyType.ELDRITCH_HORROR) {
            paint.color = 0xFF000000.toInt(); paint.alpha = 150
            canvas.scale(0.5f, 0.5f)
            canvas.drawPath(path, paint)
            paint.color = color; paint.alpha = alpha
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f / (r * 0.5f)
            canvas.drawPath(path, paint)
        } else if (enemy.type == EnemyType.COSMIC_GOD) {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f / r; paint.color = 0xFFFF00FF.toInt()
            canvas.drawCircle(0f, 0f, 1f, paint)
            canvas.rotate(time * 60f)
            paint.color = 0xFF00FFFF.toInt(); paint.style = Paint.Style.FILL
            canvas.drawCircle(0f, -0.5f, 0.2f, paint)
            canvas.drawCircle(0f, 0.5f, 0.2f, paint)
        }
        
        canvas.restore()
    }

    private fun getEnemyColor(enemy: Enemy) = when {
        enemy.isEnraged -> 0xFFFF4400.toInt()
        else -> when (enemy.type) {
            EnemyType.VOID_CRAWLER      -> 0xFF8822FF.toInt()
            EnemyType.COSMIC_SHADE      -> 0xFF4400CC.toInt()
            EnemyType.NEBULA_WRAITH     -> 0xFF0055FF.toInt()
            EnemyType.STAR_DEVOURER     -> 0xFFFF8800.toInt()
            EnemyType.RIFT_STALKER      -> 0xFF00FFAA.toInt()
            EnemyType.VOID_TITAN        -> 0xFFFF0044.toInt()
            EnemyType.COSMIC_GOD        -> 0xFFFF00FF.toInt()
            EnemyType.ENTROPY_HERALD    -> 0xFFFF6600.toInt()
            EnemyType.SINGULARITY_SPAWN -> 0xFF44FFFF.toInt()
            EnemyType.ABYSS_KNIGHT      -> 0xFF6600AA.toInt()
            EnemyType.PHASE_PHANTOM     -> 0xFF88FFFF.toInt()
            EnemyType.ELDRITCH_HORROR   -> 0xFFAA00FF.toInt()
        }
    }

    // ─── Projectiles ──────────────────────────────────────────────────────────
    private fun drawProjectiles(canvas: Canvas, snap: GameSnapshot) {
        for (proj in snap.projectiles) {
            if (!proj.isAlive) continue
            // Glow
            paint.style = Paint.Style.FILL
            paint.color = proj.color
            paint.alpha = 60
            canvas.drawCircle(proj.position.x, proj.position.y, proj.radius * 2.5f, paint)
            // Core
            paint.alpha = 255
            canvas.drawCircle(proj.position.x, proj.position.y, proj.radius, paint)
            if (proj.isCrit) {
                paint.color = 0xFFFFFFFF.toInt()
                paint.alpha = 180
                canvas.drawCircle(proj.position.x, proj.position.y, proj.radius * 0.4f, paint)
            }
        }
    }

    private fun drawEnemyProjectiles(canvas: Canvas, snap: GameSnapshot) {
        for (proj in snap.enemyProjectiles) {
            if (!proj.isAlive) continue
            paint.style = Paint.Style.FILL
            paint.color = proj.color
            paint.alpha = 70
            canvas.drawCircle(proj.position.x, proj.position.y, 16f, paint)
            paint.alpha = 255
            canvas.drawCircle(proj.position.x, proj.position.y, 10f, paint)
        }
    }

    // ─── Player ───────────────────────────────────────────────────────────────
    private fun drawPlayer(canvas: Canvas, snap: GameSnapshot) {
        val p = snap.player
        val cx = p.position.x
        val cy = p.position.y
        val r  = p.radius
        
        val avatar = AvatarDefinitions.getAvatar(snap.avatarIndex)

        // 1. Outer Atmospheric Glow
        paint.style = Paint.Style.FILL
        paint.color = avatar.color
        val pulse = (sin(time * 5f) * 0.2f + 1.0f)
        paint.alpha = (if (p.isInvincible) (sin(time * 20f) * 40 + 60).toInt() else 30)
        canvas.drawCircle(cx, cy, r * 2.5f * pulse, paint)

        // 2. Rotating Energy Rings
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.alpha = 100
        canvas.save()
        canvas.rotate(time * 30f, cx, cy)
        canvas.drawOval(cx - r * 1.8f, cy - r * 0.5f, cx + r * 1.8f, cy + r * 0.5f, paint)
        canvas.rotate(120f, cx, cy)
        canvas.drawOval(cx - r * 1.8f, cy - r * 0.5f, cx + r * 1.8f, cy + r * 0.5f, paint)
        canvas.restore()

        // 3. Player Body (Multiple Layers)
        canvas.save()
        canvas.rotate(time * 20f, cx, cy)
        
        // Inner dark core
        paint.alpha = 230
        paint.style = Paint.Style.FILL
        paint.color = 0xFF080B14.toInt() 
        drawAvatarShape(canvas, avatar.shape, cx, cy, r, paint)

        // Outer neon shell
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = avatar.color
        paint.alpha = if (p.isInvincible) (sin(time * 20f) * 100 + 155).toInt() else 255
        drawAvatarShape(canvas, avatar.shape, cx, cy, r, paint)
        
        // Inner secondary shell
        canvas.rotate(-time * 40f, cx, cy)
        paint.strokeWidth = 1f
        paint.alpha = 150
        drawAvatarShape(canvas, avatar.shape, cx, cy, r * 0.6f, paint)
        
        canvas.restore()

        // 4. Central Singularity
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFFFFFF.toInt()
        paint.alpha = (200 + sin(time * 10f) * 55).toInt()
        canvas.drawCircle(cx, cy, r * 0.15f, paint)
        
        paint.color = avatar.color
        paint.alpha = 255
        canvas.drawCircle(cx, cy, r * 0.08f, paint)

        // 5. Weapon System indicator (Orbiting)
        val weaponAngle = time * 4f
        val wx = cx + cos(weaponAngle) * (r * 1.2f)
        val wy = cy + sin(weaponAngle) * (r * 1.2f)
        paint.color = getWeaponColor(p.weaponType)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(wx, wy, 5f, paint)
        paint.alpha = 100
        canvas.drawCircle(wx, wy, 10f, paint)
    }

    private fun drawAvatarShape(canvas: Canvas, shape: AvatarShape, cx: Float, cy: Float, r: Float, paint: Paint) {
        val path = avatarPaths[shape] ?: avatarPaths[AvatarShape.VOID_CORE]!!
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(r, r)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun getWeaponColor(w: com.voidascension.engine.WeaponType) = when (w) {
        com.voidascension.engine.WeaponType.PLASMA_RIFLE     -> 0xFF00FFFF.toInt()
        com.voidascension.engine.WeaponType.VOID_CANNON      -> 0xFF8800FF.toInt()
        com.voidascension.engine.WeaponType.NEURAL_LANCE     -> 0xFF00FF88.toInt()
        com.voidascension.engine.WeaponType.QUANTUM_BURST    -> 0xFFFFFF00.toInt()
        com.voidascension.engine.WeaponType.COSMIC_SCYTHE    -> 0xFFFF00FF.toInt()
        com.voidascension.engine.WeaponType.ENTROPY_BEAM     -> 0xFFFF8800.toInt()
        com.voidascension.engine.WeaponType.PHASE_BLADE      -> 0xFF44FFFF.toInt()
        com.voidascension.engine.WeaponType.SINGULARITY_GUN  -> 0xFFFF44AA.toInt()
        com.voidascension.engine.WeaponType.NEUROTOXIN_SPRAYER -> 0xFF88FF00.toInt()
    }

    // ─── Particles ────────────────────────────────────────────────────────────
    private fun drawParticles(canvas: Canvas, snap: GameSnapshot) {
        for (p in snap.particles) {
            paint.style = Paint.Style.FILL
            paint.color = p.color
            paint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
            when (p.type) {
                ParticleType.SMOKE -> {
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
                ParticleType.XP_ORB -> {
                    canvas.drawCircle(p.x, p.y, p.size * 0.6f, paint)
                    paint.alpha = (p.alpha * 100).toInt()
                    canvas.drawCircle(p.x, p.y, p.size * 1.4f, paint)
                }
                ParticleType.AURA_RING -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    canvas.drawCircle(p.x, p.y, p.size, paint)
                }
                else -> canvas.drawCircle(p.x, p.y, p.size, paint)
            }
        }
        paint.alpha = 255
    }

    // ─── Floating Texts ───────────────────────────────────────────────────────
    private fun drawFloatingTexts(canvas: Canvas, snap: GameSnapshot) {
        for (ft in snap.floatingTexts) {
            textPaint.color = ft.color
            textPaint.textSize = if (ft.isCrit) 36f else 26f
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.alpha = (ft.life * 255).toInt().coerceIn(0, 255)
            if (ft.isCrit) textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(ft.text, ft.x, ft.y, textPaint)
            textPaint.typeface = Typeface.MONOSPACE
        }
        textPaint.alpha = 255
    }

    // ─── HUD ──────────────────────────────────────────────────────────────────
    private fun drawHUD(canvas: Canvas, snap: GameSnapshot) {
        val p = snap.player
        val w = snap.screenWidth
        val margin = 24f

        // HP bar
        val hpBarW = w * 0.5f
        val hpBarH = 32f
        val hpX = margin
        val hpY = margin
        drawBar(canvas, hpX, hpY, hpBarW, hpBarH, p.hpPercent(), 0xFF003300.toInt(), COLOR_HP_BAR)
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.textSize = 22f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("HP  ${p.currentHp.toInt()}/${p.maxHp.toInt()}", hpX + 10f, hpY + hpBarH - 8f, textPaint)

        // XP bar
        val xpBarY = hpY + hpBarH + 12f
        drawBar(canvas, hpX, xpBarY, hpBarW, 18f, p.xpPercent(), 0xFF001133.toInt(), COLOR_XP_BAR)

        // HUD Info Row (LVL | WAVE | SCORE | KILLS)
        val infoY = xpBarY + 65f
        textPaint.textAlign = Paint.Align.LEFT
        var currentX = margin

        // LVL
        textPaint.color = COLOR_CYAN
        textPaint.textSize = 28f
        val lvlStr = "LVL ${p.level}"
        canvas.drawText(lvlStr, currentX, infoY, textPaint)
        currentX += textPaint.measureText(lvlStr) + 40f

        // WAVE
        textPaint.color = 0xFFFF00FF.toInt()
        textPaint.textSize = 28f
        val waveStr = snap.waveTitle
        canvas.drawText(waveStr, currentX, infoY, textPaint)
        currentX += textPaint.measureText(waveStr) + 40f

        // SCORE
        textPaint.color = 0xFFFFAA00.toInt()
        textPaint.textSize = 28f
        val scoreStr = "SCORE: ${p.score}"
        canvas.drawText(scoreStr, currentX, infoY, textPaint)
        currentX += textPaint.measureText(scoreStr) + 40f

        // KILLS
        textPaint.color = 0xFFAAAAAA.toInt()
        textPaint.textSize = 28f
        val killsStr = "☠ ${p.killCount}"
        canvas.drawText(killsStr, currentX, infoY, textPaint)

        // Mutation indicators (bottom-left row)
        if (p.mutations.isNotEmpty()) {
            val mutY = snap.screenHeight - 80f
            textPaint.textSize = 18f
            textPaint.color = 0xFF00FFAA.toInt()
            textPaint.textAlign = Paint.Align.LEFT
            val mutStr = p.mutations.takeLast(5).joinToString(" | ") {
                it.name.take(6)
            }
            canvas.drawText(mutStr, margin, mutY, textPaint)
        }

        // Aura icons (bottom-right)
        if (p.auras.isNotEmpty()) {
            val auraY = snap.screenHeight - 80f
            textPaint.textSize = 18f
            textPaint.textAlign = Paint.Align.RIGHT
            val auraStr = p.auras.joinToString(" ") { "◈${it.type.name.take(4)}" }
            textPaint.color = 0xFF8800FF.toInt()
            canvas.drawText(auraStr, w - margin, auraY, textPaint)
        }
    }

    private fun drawBar(
        canvas: Canvas, x: Float, y: Float, w: Float, h: Float,
        fill: Float, bgColor: Int, fgColor: Int
    ) {
        paint.style = Paint.Style.FILL
        paint.color = bgColor; paint.alpha = 200
        canvas.drawRoundRect(x, y, x + w, y + h, h / 2, h / 2, paint)
        paint.color = fgColor; paint.alpha = 255
        canvas.drawRoundRect(x, y, x + w * fill.coerceIn(0f, 1f), y + h, h / 2, h / 2, paint)
        paint.color = 0xFFFFFFFF.toInt(); paint.alpha = 40
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f
        canvas.drawRoundRect(x, y, x + w, y + h, h / 2, h / 2, paint)
        paint.alpha = 255; paint.style = Paint.Style.FILL
    }
}
