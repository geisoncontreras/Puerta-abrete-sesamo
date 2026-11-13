package com.abretesesamo.core.unlock

import com.abretesesamo.core.rhythm.Rhythm
import com.abretesesamo.core.rhythm.RhythmMatcher

/**
 * Máquina de estados de un desbloqueo. No sabe nada de Android: recibe marcas de tiempo
 * (ms, reloj monotónico) y devuelve eventos. Así se puede probar con tests normales.
 *
 * Reglas:
 *  - Un intento NO se evalúa al llegar al número de toques: se espera una pausa corta
 *    ([completeWaitMs]) por si el usuario sigue tocando. Si toca de más, es fallo al instante.
 *  - Si deja de tocar durante [idleTimeoutMs] con un intento a medias, también es fallo.
 *  - Cada [failuresBeforeHint] fallos seguidos (3, 6, 9...), el evento de fallo trae una
 *    pista: la primera mitad del ritmo, para que la puerta la toque "desde dentro".
 *  - Después de la pista, el usuario puede **continuar** el ritmo: los golpes de la pista
 *    cuentan como los primeros toques y él solo completa el resto. También vale tocarlo entero.
 */
class UnlockSession(
    private val secret: Rhythm,
    private val matcher: RhythmMatcher,
    private val failuresBeforeHint: Int = 3,
) {

    sealed interface Event {
        data class TapRegistered(val tapsInAttempt: Int) : Event
        data object Unlocked : Event
        data class Failed(val consecutiveFailures: Int, val hint: Rhythm?) : Event {
            /** Fallos que quedan hasta la próxima pista. */
            fun attemptsUntilHint(failuresBeforeHint: Int = 3): Int =
                failuresBeforeHint - (consecutiveFailures % failuresBeforeHint)
        }
    }

    private val taps = mutableListOf<Long>()

    /** Instantes en que sonaron los golpes de la pista (si acaba de sonar una). */
    private var hintKnocks: List<Long>? = null

    var consecutiveFailures: Int = 0
        private set

    /** Pausa tras la cual un intento a medias se considera terminado. */
    val idleTimeoutMs: Long = (secret.longestIntervalMs * 2).coerceIn(1_200L, 4_000L)

    /**
     * Pausa tras el último toque antes de dar por bueno un intento completo: algo más que el
     * intervalo más largo del ritmo, para que un toque de más alcance a llegar y cuente como fallo.
     */
    val completeWaitMs: Long =
        (secret.longestIntervalMs * (1 + matcher.tolerance) + 250).toLong().coerceIn(700L, 2_500L)

    val tapsInAttempt: Int get() = taps.size

    /** El intento tiene justo los toques del ritmo (solo o completando la pista). */
    private fun hasFullCount(): Boolean =
        taps.size == secret.knockCount || (hintKnocks?.let { it.size + taps.size == secret.knockCount } ?: false)

    /** Cuánto esperar tras el último toque antes de llamar a [onIdle]. */
    fun nextCheckDelayMs(): Long = if (hasFullCount()) completeWaitMs else idleTimeoutMs

    fun onTap(uptimeMs: Long): Event {
        taps += uptimeMs
        // Más toques que el ritmo: fallo inmediato.
        if (taps.size > secret.knockCount) return failure()
        return Event.TapRegistered(taps.size)
    }

    /**
     * Llamar cuando pasa [nextCheckDelayMs] sin toques. Evalúa el intento: abre si el ritmo
     * coincide, falla si no, o devuelve null si no había nada que evaluar (un toque suelto).
     */
    fun onIdle(): Event? {
        if (taps.isEmpty()) return null
        hintKnocks?.let { prefix ->
            val combined = prefix + taps
            if (matcher.matchesTimestamps(secret, combined)) return success()
        }
        if (matcher.matchesTimestamps(secret, taps)) return success()
        val minTapsToCount = if (hintKnocks != null) 1 else 2
        if (taps.size < minTapsToCount) {
            taps.clear()
            return null
        }
        return failure()
    }

    /** La UI avisa de cuándo sonó realmente cada golpe de la pista. */
    fun onHintPlayed(knockUptimesMs: List<Long>) {
        taps.clear()
        hintKnocks = knockUptimesMs.takeIf { it.isNotEmpty() }
    }

    fun reset() {
        taps.clear()
        hintKnocks = null
        consecutiveFailures = 0
    }

    private fun success(): Event {
        reset()
        return Event.Unlocked
    }

    private fun failure(): Event {
        taps.clear()
        hintKnocks = null
        consecutiveFailures++
        val hint = if (consecutiveFailures % failuresBeforeHint == 0) secret.firstHalf() else null
        return Event.Failed(consecutiveFailures, hint)
    }
}
