package com.voidascension.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioManager @Inject constructor(private val context: Context) {
    private val soundPool: SoundPool
    private val categoryMap = mutableMapOf<String, MutableList<Int>>()
    
    private var sfxVolume = 1.0f
    private var bgmVolume = 1.0f
    private var isMuted = false

    private val bgmQueue = mutableListOf<String>()
    private var currentBgmIndex = -1
    private var currentBgmFolder: String? = null
    private var mediaPlayer: MediaPlayer? = null

    private val prefs = context.getSharedPreferences("audio_prefs", Context.MODE_PRIVATE)

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(15)
            .setAudioAttributes(audioAttributes)
            .build()
            
        soundPool.setOnLoadCompleteListener { _, soundId, status ->
            if (status == 0) {
                Log.d("AudioManager", "Sound $soundId loaded successfully")
            } else {
                Log.e("AudioManager", "Failed to load sound $soundId, status: $status")
            }
        }

        sfxVolume = prefs.getFloat("sfx_volume", 1.0f)
        bgmVolume = prefs.getFloat("bgm_volume", 1.0f)
        isMuted = prefs.getBoolean("is_muted", false)

        loadAllAssets("sounds")
    }

    private fun loadAllAssets(path: String) {
        try {
            val assets = context.assets.list(path) ?: return
            for (asset in assets) {
                val fullPath = "$path/$asset"
                val subAssets = context.assets.list(fullPath)
                
                if (subAssets.isNullOrEmpty()) {
                    if (isSfxFile(fullPath)) {
                        loadSfx(fullPath)
                    }
                } else {
                    loadAllAssets(fullPath)
                }
            }
        } catch (e: IOException) {
            Log.e("AudioManager", "Error scanning assets at $path", e)
        }
    }

    private fun isSfxFile(path: String): Boolean {
        if (path.contains("background-music")) return false
        return path.endsWith(".wav") || path.endsWith(".mp3") || path.endsWith(".ogg")
    }

    private fun loadSfx(path: String) {
        try {
            val fd = context.assets.openFd(path)
            val soundId = soundPool.load(fd, 1)
            // fd.close() // Temporarily removed to check if immediate close causes loading issues
            
            val fileName = path.substringAfterLast("/").substringBeforeLast(".")
            val parts = path.split("/")
            
            registerInCategory(fileName, soundId)
            for (i in 1 until parts.size - 1) {
                registerInCategory(parts[i], soundId)
            }
            Log.d("AudioManager", "Loading SFX: $path as $fileName")
        } catch (e: Exception) {
            Log.e("AudioManager", "Failed to load SFX: $path", e)
        }
    }

    private fun registerInCategory(category: String, soundId: Int) {
        categoryMap.getOrPut(category) { mutableListOf() }.add(soundId)
    }

    fun playSfx(tag: String) {
        if (isMuted) return
        val soundIds = categoryMap[tag]
        if (soundIds.isNullOrEmpty()) {
            Log.w("AudioManager", "No sounds found for tag: $tag")
            return
        }
        
        val soundId = soundIds.random()
        val result = soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1f)
        if (result == 0) {
            Log.w("AudioManager", "Failed to play sound $soundId for tag: $tag (might not be loaded yet)")
        } else {
            Log.d("AudioManager", "Playing SFX for tag: $tag, soundId: $soundId")
        }
    }

    fun playRandomKillSound() = playSfx("kills")
    fun playPlayerShoot() = playSfx("player")
    fun playEnemyShoot() = playSfx("enemies")
    fun playLootSound() = playSfx("loot")
    fun playMenuClick() = playSfx("menu-sounds")

    fun startBgm(type: String) {
        val folder = "sounds/background-music/$type"
        if (currentBgmFolder == folder && mediaPlayer?.isPlaying == true) return
        
        stopBgm()
        currentBgmFolder = folder
        bgmQueue.clear()
        
        try {
            context.assets.list(folder)?.forEach {
                bgmQueue.add("$folder/$it")
            }
            if (bgmQueue.isNotEmpty()) {
                bgmQueue.shuffle()
                currentBgmIndex = 0
                playNextBgm()
            }
        } catch (e: IOException) {
            Log.e("AudioManager", "Error loading BGM from $folder", e)
        }
    }

    private fun playNextBgm() {
        if (bgmQueue.isEmpty()) return
        
        val assetPath = bgmQueue[currentBgmIndex]
        mediaPlayer?.release()
        
        try {
            mediaPlayer = MediaPlayer().apply {
                val fd = context.assets.openFd(assetPath)
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                fd.close()
                setVolume(if (isMuted) 0f else bgmVolume, if (isMuted) 0f else bgmVolume)
                prepare()
                start()
                setOnCompletionListener {
                    currentBgmIndex = (currentBgmIndex + 1) % bgmQueue.size
                    playNextBgm()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioManager", "Error playing BGM: $assetPath", e)
            if (bgmQueue.size > 1) {
                currentBgmIndex = (currentBgmIndex + 1) % bgmQueue.size
                playNextBgm()
            }
        }
    }

    fun stopBgm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentBgmFolder = null
    }

    fun setBgmVolume(volume: Float) {
        bgmVolume = volume.coerceIn(0f, 1f)
        prefs.edit().putFloat("bgm_volume", bgmVolume).apply()
        if (!isMuted) {
            mediaPlayer?.setVolume(bgmVolume, bgmVolume)
        }
    }

    fun setSfxVolume(volume: Float) {
        sfxVolume = volume.coerceIn(0f, 1f)
        prefs.edit().putFloat("sfx_volume", sfxVolume).apply()
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        prefs.edit().putBoolean("is_muted", muted).apply()
        val vol = if (muted) 0f else bgmVolume
        mediaPlayer?.setVolume(vol, vol)
    }
    
    fun getBgmVolume() = bgmVolume
    fun getSfxVolume() = sfxVolume
    fun isMuted() = isMuted

    fun release() {
        soundPool.release()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
