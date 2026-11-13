package com.abretesesamo.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.media.SoundPool
import android.os.SystemClock
import androidx.annotation.RawRes
import com.abretesesamo.app.R
import com.abretesesamo.app.model.SoundMode
import com.abretesesamo.core.rhythm.Rhythm
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

/**
 * Los toques de la puerta, con sonidos reales grabados (5 golpes de puño y 10 de nudillos,
 * recortados uno a uno de las grabaciones de Geison, en res/raw).
 *
 * - [tap]: suena al instante cuando el usuario toca la puerta (SoundPool, baja latencia).
 * - [play]: reproduce un ritmo entero (la pista tras 3 fallos o "Escucharlo"). Se mezcla en
 *   UNA pista de audio con cada golpe en su milisegundo exacto, para que no se acumulen retrasos.
 *
 * Cada golpe elige una grabación al azar (sin repetir la anterior) y varía un poco el tono,
 * así nunca suena idéntico. En [SoundMode.MEZCLA] el primer toque tras una pausa suena fuerte
 * (puño) y los rápidos suenan con nudillos: "¡TOC!… toc-toc-toc".
 */
class KnockPlayer(context: Context, var mode: SoundMode = SoundMode.MEZCLA) {

    private enum class Kind { PUNO, NUDILLO }

    private val res = context.applicationContext.resources
    private val samples: Map<Kind, List<ShortArray>> = mapOf(
        Kind.PUNO to PUNO.map { readWav(it) },
        Kind.NUDILLO to NUDILLO.map { readWav(it) },
    )

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val poolIds: Map<Kind, List<Int>> = mapOf(
        Kind.PUNO to PUNO.map { pool.load(context.applicationContext, it, 1) },
        Kind.NUDILLO to NUDILLO.map { pool.load(context.applicationContext, it, 1) },
    )

    private val creakIds: List<Int> = listOf(R.raw.rechinido_1, R.raw.rechinido_2).map { pool.load(context.applicationContext, it, 1) }

    private val lastPick = mutableMapOf<Kind, Int>()
    private var lastTapAt = 0L
    private var track: AudioTrack? = null

    /**
     * Suena un toque del usuario. [secret] (si se conoce) sirve para saber qué es una
     * "pausa larga" en su ritmo.
     */
    fun tap(uptimeMs: Long, secret: Rhythm? = null) {
        val gap = if (lastTapAt == 0L) null else uptimeMs - lastTapAt
        lastTapAt = uptimeMs
        val kind = kindFor(gap, secret?.let { median(it.intervalsMs) } ?: 400L)
        val ids = poolIds.getValue(kind)
        pool.play(ids[pick(kind, ids.size)], 1f, 1f, 1, 0, 0.97f + Random.nextFloat() * 0.06f)
    }

    /** Rechinido de bisagra (grabaciones de Geison, una al azar) mientras la hoja gira. */
    fun creak() {
        pool.play(creakIds.random(), 0.9f, 0.9f, 2, 0, 0.97f + Random.nextFloat() * 0.06f)
    }

    /** Un solo toque de muestra (al elegir el sonido en los ajustes). */
    fun preview() {
        lastTapAt = 0L
        tap(SystemClock.uptimeMillis())
        lastTapAt = 0L
    }

    /**
     * Reproduce el ritmo y espera a que termine.
     * @return el instante (SystemClock.uptimeMillis) en que sonó cada golpe, para que el
     *         usuario pueda continuar el ritmo justo donde lo dejó la puerta.
     */
    suspend fun play(rhythm: Rhythm): List<Long> {
        stop()
        val offsets = rhythm.knockOffsetsMs()
        val typical = median(rhythm.intervalsMs)
        val chosen = offsets.indices.map { i ->
            val kind = kindFor(if (i == 0) null else rhythm.intervalsMs[i - 1], typical)
            val bank = samples.getValue(kind)
            bank[pick(kind, bank.size)]
        }
        val longest = chosen.maxOf { it.size }
        val buffer = IntArray(msToSamples(offsets.last()) + longest)
        offsets.forEachIndexed { i, offset ->
            val start = msToSamples(offset)
            val knock = chosen[i]
            for (j in knock.indices) buffer[start + j] += knock[j]
        }
        val pcm = ShortArray(buffer.size) { buffer[it].coerceIn(-32767, 32767).toShort() }

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
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        t.write(pcm, 0, pcm.size)
        track = t

        var firstKnockAt = SystemClock.uptimeMillis()
        t.play()
        try {
            // Al poco de empezar preguntamos al sistema cuándo sonó de verdad la muestra 0,
            // para descontar la latencia del altavoz.
            delay(150)
            firstKnockAt = measuredStart(t) ?: firstKnockAt
            delay((offsets.last() + longest * 1000L / SAMPLE_RATE - 150).coerceAtLeast(0L))
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

    fun release() {
        stop()
        pool.release()
    }

    private fun kindFor(gapMs: Long?, typicalMs: Long): Kind = when (mode) {
        SoundMode.PUNO -> Kind.PUNO
        SoundMode.NUDILLO -> Kind.NUDILLO
        SoundMode.MEZCLA ->
            if (gapMs == null || gapMs > maxOf(550L, typicalMs * 14 / 10)) Kind.PUNO else Kind.NUDILLO
    }

    /** Índice al azar que no repite el anterior de ese tipo de golpe. */
    private fun pick(kind: Kind, size: Int): Int {
        var i = Random.nextInt(size)
        if (size > 1 && i == lastPick[kind]) i = (i + 1) % size
        lastPick[kind] = i
        return i
    }

    private fun median(values: List<Long>): Long =
        if (values.isEmpty()) 400L else values.sorted()[values.size / 2]

    private fun measuredStart(t: AudioTrack): Long? {
        val ts = AudioTimestamp()
        if (!t.getTimestamp(ts) || ts.framePosition <= 0) return null
        // AudioTimestamp.nanoTime usa el mismo reloj monotónico que uptimeMillis.
        val frame0Nanos = ts.nanoTime - ts.framePosition * 1_000_000_000L / SAMPLE_RATE
        return frame0Nanos / 1_000_000L
    }

    private fun msToSamples(ms: Long): Int = (ms * SAMPLE_RATE / 1000).toInt()

    /** Lee un WAV PCM 16 bits mono (44,1 kHz) de res/raw y devuelve sus muestras. */
    private fun readWav(@RawRes id: Int): ShortArray {
        val bytes = res.openRawResource(id).use { it.readBytes() }
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12 // tras "RIFF" + tamaño + "WAVE"
        while (pos + 8 <= bytes.size) {
            val chunk = String(bytes, pos, 4, Charsets.US_ASCII)
            val size = bb.getInt(pos + 4)
            if (chunk == "data") {
                val n = minOf(size, bytes.size - pos - 8) / 2
                return ShortArray(n) { bb.getShort(pos + 8 + it * 2) }
            }
            pos += 8 + size + (size and 1)
        }
        return ShortArray(0)
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        val PUNO = listOf(R.raw.puno_1, R.raw.puno_2, R.raw.puno_3, R.raw.puno_4, R.raw.puno_5)
        val NUDILLO = listOf(
            R.raw.nudillo_1, R.raw.nudillo_2, R.raw.nudillo_3, R.raw.nudillo_4, R.raw.nudillo_5,
            R.raw.nudillo_6, R.raw.nudillo_7, R.raw.nudillo_8, R.raw.nudillo_9, R.raw.nudillo_10,
        )
    }
}
