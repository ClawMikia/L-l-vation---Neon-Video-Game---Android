package com.voidascension.engine

import android.graphics.Color

enum class AvatarShape {
    VOID_CORE,      // Was DIAMOND
    NEBULA_EYE,     // Was CIRCLE
    RIFT_PRISM,     // Was SQUARE
    QUANTUM_CELL,   // Was HEXAGON
    STELLAR_BLADE,  // Was TRIANGLE
    ENTROPY_NODE,   // Was STAR
    CHRONO_DISK,    // Was CROSS
    PLASMA_GHOST,   // Was OCTAGON
    COSMIC_SHELL,   // Was SHIELD
    VECTOR_SOUL,    // Was GEAR
    SINGULARITY     // Was CAPSULE
}

data class AvatarDef(
    val color: Int,
    val shape: AvatarShape
)

object AvatarDefinitions {
    val AVATARS = listOf(
        AvatarDef(Color.parseColor("#00FFFF"), AvatarShape.VOID_CORE),      // Cyan
        AvatarDef(Color.parseColor("#FF00FF"), AvatarShape.NEBULA_EYE),     // Magenta
        AvatarDef(Color.parseColor("#39FF14"), AvatarShape.RIFT_PRISM),     // Neon Green
        AvatarDef(Color.parseColor("#7DF9FF"), AvatarShape.QUANTUM_CELL),   // Electric Blue
        AvatarDef(Color.parseColor("#FF4444"), AvatarShape.STELLAR_BLADE),  // Red
        AvatarDef(Color.parseColor("#FFFF00"), AvatarShape.ENTROPY_NODE),   // Yellow
        AvatarDef(Color.parseColor("#BF00FF"), AvatarShape.CHRONO_DISK),    // Electric Purple
        AvatarDef(Color.parseColor("#FF7F00"), AvatarShape.PLASMA_GHOST),   // Orange
        AvatarDef(Color.parseColor("#E0E0E0"), AvatarShape.COSMIC_SHELL),   // Platinum
        AvatarDef(Color.parseColor("#00FF88"), AvatarShape.VECTOR_SOUL),    // Spring Green
        AvatarDef(Color.parseColor("#4466FF"), AvatarShape.SINGULARITY)     // Deep Blue
    )
    
    fun getAvatar(index: Int): AvatarDef {
        return AVATARS.getOrElse(index) { AVATARS[0] }
    }
}
