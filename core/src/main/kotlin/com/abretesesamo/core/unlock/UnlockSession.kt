package com.abretesesamo.core.unlock

import com.abretesesamo.core.rhythm.Rhythm
import com.abretesesamo.core.rhythm.RhythmMatcher

/**
 * Máquina de estados de un desbloqueo. No sabe nada de Android: recibe marcas de tiempo
 * (ms, reloj monotónico) y devuelve eventos. Así se puede probar con tests normales.
 *
 * Reglas:
 *  - Un intento se evalúa en cuanto el usuario da tantos toques como tiene el ritmo,
 *    o cuando deja de tocar durante [idleTimeoutMs] (intento incompleto = fallo).
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

    /** Pausa tras la cual un intento se considera terminado. */
    val idleTimeoutMs: Long = (secret.longestIntervalMs * 2).coerceIn(1_200L, 4_000L)

    val tapsInAttempt: Int get() = taps.size

    fun onTap(uptimeMs: Long): Event {
        taps += uptimeMs

        // Modo "completar": los golpes de la pista + los del usuario forman el ritmo.
        hintKnocks?.let { prefix ->
            val combined = prefix + taps
            if (combined.size == secret.knockCount && matcher.matchesTimestamps(secret, combined)) {
                return success()
            }
        }

        if (taps.size >= secret.knockCount) {
            return if (matcher.matchesTimestamps(secret, taps)) success() else failure()
        }
        return Event.TapRegistered(taps.size)
    }

    /**
     * Llamar cuando pasa [idleTimeoutMs] sin toques. Devuelve un fallo si había un intento
     * a medias, o null si no había nada que evaluar (un único toque suelto se ignora).
     */
    fun onIdle(): Event? {
        if (taps.isEmpty()) return null
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
