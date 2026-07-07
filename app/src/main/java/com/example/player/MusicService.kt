package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var equalizer: Equalizer? = null

    override fun onCreate() {
        super.onCreate()
        
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
            
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50000,
                100000,
                2500,
                5000
            )
            .build()
            
        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setEnableDecoderFallback(true)
            
        val player = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            
        player.setWakeMode(C.WAKE_MODE_LOCAL)
        
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                super.onAudioSessionIdChanged(audioSessionId)
                try {
                    releaseAudioEffects()
                    
                    // Add slight loudness enhancement
                    loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                        setTargetGain(500) // 500mB gain to make it punchier
                        enabled = true
                    }
                    
                    // Add slight bass boost
                    bassBoost = BassBoost(0, audioSessionId).apply {
                        if (strengthSupported) {
                            enabled = true
                            setStrength(300) // 0-1000 scale
                        }
                    }
                    
                    // Try to boost higher frequencies to make it sound clearer (often lost in low bitrate)
                    equalizer = Equalizer(0, audioSessionId).apply {
                        enabled = true
                        val numBands = numberOfBands
                        // Boost the highest frequency bands slightly to restore some "air" and clarity
                        if (numBands > 0) {
                            val maxEqLevel = bandLevelRange[1]
                            val highestBand = (numBands - 1).toShort()
                            setBandLevel(highestBand, (maxEqLevel * 0.3f).toInt().toShort()) // 30% of max boost
                            
                            if (numBands > 1) {
                                val secondHighestBand = (numBands - 2).toShort()
                                setBandLevel(secondHighestBand, (maxEqLevel * 0.2f).toInt().toShort()) // 20% of max boost
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
            
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun releaseAudioEffects() {
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            bassBoost?.release()
            bassBoost = null
            equalizer?.release()
            equalizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        releaseAudioEffects()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
