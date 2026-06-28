package com.voidascension.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheatManager @Inject constructor() {
    
    var isLaFierceActive = false
    var isMauragPoAkoActive = false
    var isAikiActive = false
    var isMochiMochiActive = false
    var isMotomemashitaActive = false

    fun activateCheat(code: String, saveManager: SaveManager): String {
        return when (code.trim()) {
            "Christopher Lee Cajes" -> {
                saveManager.addVoidShards(9999)
                "SYSTEM OVERRIDE: +9999 SHARDS ACQUIRED"
            }
            "La-Fierce" -> {
                isLaFierceActive = true
                "SYSTEM OVERRIDE: 25 LIVES GRANTED"
            }
            "Maurag po ako" -> {
                isMauragPoAkoActive = true
                "SYSTEM OVERRIDE: ULTIMATE BIOMASS ACTIVATED"
            }
            "Aiki" -> {
                isAikiActive = true
                "SYSTEM OVERRIDE: ACCELERATED EVOLUTION"
            }
            "Mochi-Mochi" -> {
                isMochiMochiActive = true
                "SYSTEM OVERRIDE: CHAIN REACTION ENABLED"
            }
            "motomemashita" -> {
                isMotomemashitaActive = true
                "SYSTEM OVERRIDE: REVENGE PROTOCOL ONLINE"
            }
            else -> "UNACCEPTED AUTHENTICATION CODE"
        }
    }

    fun resetTemporaryCheats() {
        isLaFierceActive = false
        isMauragPoAkoActive = false
        isAikiActive = false
        isMochiMochiActive = false
        isMotomemashitaActive = false
    }
}
