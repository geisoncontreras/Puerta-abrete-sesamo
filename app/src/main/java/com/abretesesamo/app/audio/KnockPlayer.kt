package com.abretesesamo.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.SystemClock
import com.abretesesamo.core.rhythm.Rhythm
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Reproduce un ritmo de golpes "desde dentro de la puerta".
 *
 * En vez de disparar un sonido por golpe (lo que acumula retrasos), se genera UNA pista de
 * audio con todos los golpes ya colocados en su milisegundo exacto y se reproduce de una vez.
 * El sonido del golpe se sintetiza (no hace falta ningún archivo de audio).
 */
class KnockPlayer {

    private val knock: ShortArray = synthesizeKnock()
    private var track: AudioTrack? = null

    /**
     * Reproduce el ritmo y espera a que termine.
     * @return el instante (SystemClock.uptimeMillis) en que sonó cada golpe, para que el
     *         usuario pueda continuar el ritmo justo donde lo dejó la puerta.
     */
    suspend fun play(rhythm: Rhythm): List<Long> {
        stop()
        val offsets = rhythm.knockOffsetsMs()
        val buffer = ShortArray(msToSamples(offsets.last()) + knock.size)
        for (offset in offsets) {
            val start = msToSamples(offset)
            for (i in knock.indices) {
                val mixed = buffer[start + i] + knock[i]
                buffer[start + i] = mixed.coerceIn(-32767, 32767).toShort()
            }
        }

        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(buffer.size * 2)
            .build()
        t.write(buffer, 0, buffer.size)
        track = t

        var firstKnockAt = SystemClock.uptimeMillis()
        t.play()
        try {
            // Al poco de empezar preguntamos al sistema cuándo sonó de verdad la muestra 0,
            // para descontar la latencia del altavoz.
            delay(150)
            firstKnockAt = measuredStart(t) ?: firstKnockAt
            delay((offsets.last() + KNOCK_MS - 150).coerceAtLeast(0L))
        } finally {
            stop()
        }
        return offsets.map { firstKnockAt + it }
    }

    fun stop() {
        track?.let {
            runCatching { it.stop() }
            it.release()
        }
        track = null
    }

    private fun measuredStart(t: AudioTrack): Long? {
        val ts = AudioTimestamp()
        if (!t.getTimestamp(ts) || ts.framePosition <= 0) return null
        // AudioTimestamp.nanoTime usa el mismo reloj monotónico que uptimeMillis.
        val frame0Nanos = ts.nanoTime - ts.framePosition * 1_000_000_000L / SAMPLE_RATE
        return frame0Nanos / 1_000_000L
    }

    private fun msToSamples(ms: Long): Int = (ms * SAMPLE_RATE / 1000).toInt()

    private fun synthesizeKnock(): ShortArray {
        val n = SAMPLE_RATE * KNOCK_MS / 1000
        val random = Random(7)
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t / 0.022)
            // Cuerpo grave de madera + armónicos + "clic" inicial del nudillo.
            val body = 0.60 * sin(2 * PI * 140 * t) + 0.30 * sin(2 * PI * 310 * t) + 0.12 * sin(2 * PI * 760 * t)
            val click = if (t < 0.004) (random.nextDouble() * 2 - 1) * (1 - t / 0.004) * 0.7 else 0.0
            ((body * envelope + click) * 0.85 * Short.MAX_VALUE).coerceIn(-32767.0, 32767.0).toInt().toShort()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val KNOCK_MS = 130
    }
}
