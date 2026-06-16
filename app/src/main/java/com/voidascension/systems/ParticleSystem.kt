package com.voidascension.systems

import com.voidascension.engine.GameConstants
import com.voidascension.entities.Particle
import com.voidascension.entities.ParticleType
import kotlin.math.*
import kotlin.random.Random

class ParticleSystem {
    // Optimization: Use a fixed-size array and index pointer instead of ArrayList removeAt(0)
    private val particleArray = Array<Particle?>(GameConstants.MAX_PARTICLES) { null }
    private var writeIndex = 0

    val particles: List<Particle> get() = particleArray.filterNotNull()

    fun update(dt: Float) {
        for (i in 0 until GameConstants.MAX_PARTICLES) {
            val p = particleArray[i] ?: continue
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 150f * dt * when (p.type) {
                ParticleType.SPARK, ParticleType.EXPLOSION -> 1f
                ParticleType.SMOKE -> 0.3f
                ParticleType.VOID_PARTICLE, ParticleType.COSMIC_DUST -> -0.5f
                else -> 0f
            }
            p.vx *= 0.96f
            p.vy *= if (p.type == ParticleType.SPARK) 0.98f else 0.96f
            p.life -= dt / (p.maxLife / 1000f)
            p.alpha = p.life.coerceIn(0f, 1f)
            if (p.life <= 0f) {
                particleArray[i] = null
            }
        }
    }

    private fun emit(particle: Particle) {
        particleArray[writeIndex] = particle
        writeIndex = (writeIndex + 1) % GameConstants.MAX_PARTICLES
    }

    fun emitHit(x: Float, y: Float, color: Int, isCrit: Boolean = false) {
        val count = if (isCrit) 16 else 8
        repeat(count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = (80f + Random.nextFloat() * 200f) * (if (isCrit) 1.5f else 1f)
            emit(Particle(
                x = x, y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 0.8f + Random.nextFloat() * 0.4f,
                maxLife = 800f,
                size = (3f + Random.nextFloat() * (if (isCrit) 8f else 5f)),
                color = if (isCrit) 0xFFFFFF00.toInt() else color,
                type = ParticleType.SPARK
            ))
        }
    }

    fun emitExplosion(x: Float, y: Float, radius: Float) {
        repeat(30) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 100f + Random.nextFloat() * radius * 2f
            val color = listOf(
                0xFFFF8800.toInt(), 0xFFFF4400.toInt(),
                0xFFFFFF00.toInt(), 0xFFFF2200.toInt()
            ).random()
            emit(Particle(
                x = x, y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 1f, maxLife = 1000f,
                size = 4f + Random.nextFloat() * 10f,
                color = color,
                type = ParticleType.EXPLOSION
            ))
        }
        // Smoke trail
        repeat(15) {
            emit(Particle(
                x = x + (Random.nextFloat() - 0.5f) * radius,
                y = y + (Random.nextFloat() - 0.5f) * radius,
                vx = (Random.nextFloat() - 0.5f) * 40f,
                vy = -20f - Random.nextFloat() * 60f,
                life = 1f, maxLife = 1500f,
                size = 8f + Random.nextFloat() * 20f,
                color = 0x88333333.toInt(),
                type = ParticleType.SMOKE
            ))
        }
    }

    fun emitDeath(x: Float, y: Float, isBoss: Boolean = false) {
        val count = if (isBoss) 60 else 20
        repeat(count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 80f + Random.nextFloat() * (if (isBoss) 400f else 200f)
            val colors = listOf(
                0xFF8800FF.toInt(), 0xFF00FFFF.toInt(),
                0xFFFF00FF.toInt(), 0xFF0088FF.toInt()
            )
            emit(Particle(
                x = x, y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 1f,
                maxLife = if (isBoss) 1500f else 800f,
                size = if (isBoss) 8f + Random.nextFloat() * 20f else 4f + Random.nextFloat() * 8f,
                color = colors.random(),
                type = ParticleType.VOID_PARTICLE
            ))
        }
        if (isBoss) emitExplosion(x, y, 80f)
    }

    fun emitXpOrb(x: Float, y: Float) {
        repeat(5) {
            emit(Particle(
                x = x + (Random.nextFloat() - 0.5f) * 20f,
                y = y + (Random.nextFloat() - 0.5f) * 20f,
                vx = (Random.nextFloat() - 0.5f) * 60f,
                vy = -40f - Random.nextFloat() * 60f,
                life = 1f, maxLife = 1000f,
                size = 6f + Random.nextFloat() * 8f,
                color = 0xFF00FF88.toInt(),
                type = ParticleType.XP_ORB
            ))
        }
    }

    fun emitPlayerDamage(x: Float, y: Float) {
        repeat(12) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 60f + Random.nextFloat() * 150f
            emit(Particle(
                x = x, y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 0.6f, maxLife = 600f,
                size = 5f + Random.nextFloat() * 10f,
                color = 0xFFFF0000.toInt(),
                type = ParticleType.SPARK
            ))
        }
    }

    fun emitAura(cx: Float, cy: Float, radius: Float, color: Int) {
        repeat(3) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val r = radius * (0.7f + Random.nextFloat() * 0.3f)
            emit(Particle(
                x = cx + cos(angle) * r,
                y = cy + sin(angle) * r,
                vx = (Random.nextFloat() - 0.5f) * 20f,
                vy = (Random.nextFloat() - 0.5f) * 20f,
                life = 0.5f, maxLife = 500f,
                size = 4f + Random.nextFloat() * 8f,
                color = color,
                type = ParticleType.AURA_RING
            ))
        }
    }

    fun emitCosmicDust(x: Float, y: Float, count: Int = 6) {
        repeat(count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 20f + Random.nextFloat() * 80f
            emit(Particle(
                x = x, y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 1f, maxLife = 2000f,
                size = 2f + Random.nextFloat() * 5f,
                color = 0xFF4488FF.toInt(),
                type = ParticleType.COSMIC_DUST
            ))
        }
    }

    fun emitTrail(x: Float, y: Float, color: Int) {
        if (Random.nextFloat() > 0.4f) return
        emit(Particle(
            x = x + (Random.nextFloat() - 0.5f) * 8f,
            y = y + (Random.nextFloat() - 0.5f) * 8f,
            vx = (Random.nextFloat() - 0.5f) * 20f,
            vy = (Random.nextFloat() - 0.5f) * 20f,
            life = 0.4f, maxLife = 400f,
            size = 3f + Random.nextFloat() * 5f,
            color = color,
            type = ParticleType.SPARK
        ))
    }

    fun clear() {
        for (i in 0 until GameConstants.MAX_PARTICLES) {
            particleArray[i] = null
        }
    }
}
