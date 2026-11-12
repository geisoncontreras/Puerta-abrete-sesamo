package com.abretesesamo.core.rhythm

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Compara un ritmo tocado con el ritmo secreto, intervalo por intervalo.
 *
 * Cada intervalo tocado puede desviarse del original hasta `tolerance` (0.20 = ±20 %).
 * Como el 20 % de un intervalo muy corto (p. ej. 150 ms -> 30 ms) es más preciso de lo que
 * un dedo humano puede lograr, se garantiza además un margen mínimo absoluto de `minSlackMs`.
 */
class RhythmMatcher(
    tolerance: Double = DEFAULT_TOLERANCE,
    val minSlackMs: Long = DEFAULT_MIN_SLACK_MS,
) {
    val tolerance: Double = tolerance.coerceIn(MIN_TOLERANCE, MAX_TOLERANCE)

    enum class Reason { OK, WRONG_KNOCK_COUNT, OUT_OF_TOLERANCE }

    /**
     * @param worstDeviation desviación del peor intervalo, normalizada: 0 = perfecto,
     *                       1 = justo en el límite permitido, >1 = fuera de tolerancia.
     */
    data class Result(val matches: Boolean, val worstDeviation: Double, val reason: Reason)

    /** Margen permitido (ms) para un intervalo esperado. */
    fun allowedSlackMs(expectedMs: Long): Long =
        max((expectedMs * tolerance).roundToLong(), minSlackMs)

    fun match(expected: Rhythm, actual: Rhythm): Result {
        if (expected.intervalsMs.size != actual.intervalsMs.size) {
            return Result(false, Double.POSITIVE_INFINITY, Reason.WRONG_KNOCK_COUNT)
        }
        var worst = 0.0
        expected.intervalsMs.zip(actual.intervalsMs).forEach { (e, a) ->
            val deviation = abs(a - e).toDouble() / allowedSlackMs(e)
            worst = max(worst, deviation)
        }
        val ok = worst <= 1.0
        return Result(ok, worst, if (ok) Reason.OK else Reason.OUT_OF_TOLERANCE)
    }

    fun matchesTimestamps(expected: Rhythm, timestampsMs: List<Long>): Boolean =
        timestampsMs.size == expected.knockCount &&
            match(expected, Rhythm.fromTimestamps(timestampsMs)).matches

    companion object {
        const val DEFAULT_TOLERANCE = 0.20
        const val DEFAULT_MIN_SLACK_MS = 60L
        const val MIN_TOLERANCE = 0.05
        const val MAX_TOLERANCE = 0.60
    }
}
