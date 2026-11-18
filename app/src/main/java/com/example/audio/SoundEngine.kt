package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine(context: Context) {
    private val sampleRate = 22050
    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private var audioThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    @Volatile var musicVolume: Float = 0.65f
    @Volatile var sfxVolume: Float = 0.85f
    @Volatile var vibrationEnabled: Boolean = true

    @Volatile var isRacing: Boolean = false
    @Volatile var speedNorm: Float = 0f // 0..1.3
    @Volatile var isDrifting: Boolean = false
    @Volatile var isNitroActive: Boolean = false
    @Volatile var isAccelerating: Boolean = false

    // One-shot triggers
    @Volatile private var countdownFreq: Float = 0f
    @Volatile private var countdownSamplesLeft: Int = 0
    @Volatile private var collisionSamplesLeft: Int = 0
    @Volatile private var victorySamplesLeft: Int = 0
    @Volatile private var victoryTotalSamples: Int = 1
    @Volatile private var uiClickSamplesLeft: Int = 0

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        mgr?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun start() {
        if (running.getAndSet(true)) return
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        audioThread = Thread({
            val chunk = ShortArray(512)
            var enginePhase = 0.0
            var subPhase = 0.0
            var musicTime = 0.0
            var beepPhase = 0.0
            val dt = 1.0 / sampleRate
            // Futuristic D-minor / Phrygian synth bass & pad chords
            val bassNotes = doubleArrayOf(73.42, 73.42, 87.31, 98.00, 65.41, 65.41, 82.41, 98.00)
            val leadNotes = doubleArrayOf(293.66, 349.23, 440.00, 392.00, 261.63, 329.63, 392.00, 440.00)

            while (running.get()) {
                if (paused.get()) {
                    try {
                        Thread.sleep(40)
                    } catch (_: InterruptedException) {
                        break
                    }
                    continue
                }

                val mVol = musicVolume.coerceIn(0f, 1f)
                val sVol = sfxVolume.coerceIn(0f, 1f)
                val racing = isRacing
                val spd = speedNorm.coerceIn(0f, 1.35f)
                val drift = isDrifting
                val nitro = isNitroActive
                val accel = isAccelerating

                for (i in chunk.indices) {
                    var mix = 0.0

                    // 1. Background Synthwave Music (plays smoothly in menus & softer during race)
                    if (mVol > 0.01f) {
                        val step = ((musicTime * 2.0).toInt()) % bassNotes.size
                        val stepFrac = (musicTime * 2.0) % 1.0
                        val bassEnv = (1.0 - stepFrac * 0.75)
                        val bFreq = bassNotes[step]
                        val lFreq = leadNotes[step]
                        val bassWave = (sin(2.0 * PI * bFreq * musicTime) * 0.6 +
                            sin(4.0 * PI * bFreq * musicTime) * 0.25) * bassEnv
                        val padWave = sin(2.0 * PI * lFreq * musicTime) * 0.22 *
                            (0.5 + 0.5 * sin(2.0 * PI * 0.5 * musicTime))
                        val musicScale = if (racing) 0.16 else 0.26
                        mix += (bassWave + padWave) * mVol * musicScale
                        musicTime += dt
                    }

                    // 2. Real-time Car Engine + Drift + Nitro SFX
                    if (racing && sVol > 0.01f) {
                        val baseHz = 46.0 + spd * 135.0 + (if (nitro) 45.0 else 0.0) + (if (accel) 12.0 else 0.0)
                        enginePhase += baseHz * dt
                        if (enginePhase > 1.0) enginePhase -= 1.0
                        subPhase += (baseHz * 2.01) * dt
                        if (subPhase > 1.0) subPhase -= 1.0

                        // Sawtooth-ish + harmonic growl for sports car exhaust
                        val saw = (enginePhase * 2.0 - 1.0) * 0.45
                        val harmonic = sin(2.0 * PI * subPhase) * 0.35
                        val engineGain = (0.14 + spd * 0.16) * (if (accel) 1.15 else 0.75)
                        mix += (saw + harmonic) * engineGain * sVol

                        if (drift && spd > 0.18f) {
                            val screech = (Random.nextDouble() * 2.0 - 1.0) * 0.14 +
                                sin(2.0 * PI * (1280.0 + spd * 300.0) * musicTime) * 0.08
                            mix += screech * sVol
                        }

                        if (nitro) {
                            val jet = (Random.nextDouble() * 2.0 - 1.0) * 0.18 +
                                sin(2.0 * PI * 190.0 * musicTime) * 0.12
                            mix += jet * sVol
                        }
                    }

                    // 3. One-shot SFX
                    if (countdownSamplesLeft > 0) {
                        beepPhase += countdownFreq * dt
                        val env = (countdownSamplesLeft.toDouble() / (sampleRate * 0.25)).coerceIn(0.0, 1.0)
                        mix += sin(2.0 * PI * beepPhase) * 0.38 * env * sVol
                        countdownSamplesLeft--
                    }

                    if (collisionSamplesLeft > 0) {
                        val env = collisionSamplesLeft.toDouble() / (sampleRate * 0.22)
                        val thud = sin(2.0 * PI * 85.0 * env) * 0.45 + (Random.nextDouble() * 2.0 - 1.0) * 0.35
                        mix += thud * env * sVol
                        collisionSamplesLeft--
                    }

                    if (victorySamplesLeft > 0) {
                        val progress = 1.0 - (victorySamplesLeft.toDouble() / victoryTotalSamples)
                        val noteFreq = when {
                            progress < 0.25 -> 523.25 // C5
                            progress < 0.50 -> 659.25 // E5
                            progress < 0.75 -> 783.99 // G5
                            else -> 1046.50           // C6
                        }
                        val env = (1.0 - (progress * 4.0) % 1.0 * 0.4) * (1.0 - progress * 0.3)
                        mix += (sin(2.0 * PI * noteFreq * progress * 1.4) * 0.34 +
                            sin(2.0 * PI * noteFreq * 0.5 * progress * 1.4) * 0.18) * env * sVol
                        victorySamplesLeft--
                    }

                    if (uiClickSamplesLeft > 0) {
                        val env = uiClickSamplesLeft.toDouble() / (sampleRate * 0.04)
                        mix += sin(2.0 * PI * 980.0 * env) * 0.22 * env * sVol
                        uiClickSamplesLeft--
                    }

                    val clamped = mix.coerceIn(-0.92, 0.92)
                    chunk[i] = (clamped * 32767.0).toInt().toShort()
                }

                try {
                    track.write(chunk, 0, chunk.size)
                } catch (_: IllegalStateException) {
                    break
                }
            }
        }, "ShadowRacerAudio").apply {
            isDaemon = true
            start()
        }
    }

    fun playCountdownBeep(isGo: Boolean) {
        countdownFreq = if (isGo) 880f else 440f
        countdownSamplesLeft = (sampleRate * if (isGo) 0.38f else 0.20f).toInt()
    }

    fun playCollision(heavy: Boolean = false) {
        collisionSamplesLeft = (sampleRate * 0.22f).toInt()
        vibrate(if (heavy) 75L else 35L)
    }

    fun playVictoryFanfare() {
        victoryTotalSamples = (sampleRate * 1.35f).toInt()
        victorySamplesLeft = victoryTotalSamples
        vibrate(60L)
    }

    fun playUiClick() {
        uiClickSamplesLeft = (sampleRate * 0.04f).toInt()
    }

    fun vibrate(durationMs: Long) {
        if (!vibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    fun pause() {
        paused.set(true)
        try {
            audioTrack?.pause()
        } catch (_: IllegalStateException) {}
    }

    fun resume() {
        paused.set(false)
        try {
            audioTrack?.play()
        } catch (_: IllegalStateException) {}
    }

    fun release() {
        running.set(false)
        audioThread?.interrupt()
        audioThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: IllegalStateException) {}
        audioTrack = null
    }
}
