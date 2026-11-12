package com.abretesesamo.app.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.abretesesamo.app.audio.KnockPlayer
import com.abretesesamo.core.rhythm.Rhythm
import com.abretesesamo.core.rhythm.RhythmMatcher
import com.abretesesamo.core.unlock.UnlockSession
import com.abretesesamo.core.unlock.UnlockSession.Event
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Conecta la lógica pura del desbloqueo (UnlockSession) con la pantalla:
 * temporizador de "dejó de tocar", mensajes, sacudida de la puerta y la pista sonora.
 */
class LockViewModel(
    secret: Rhythm,
    matcher: RhythmMatcher,
    private val player: KnockPlayer = KnockPlayer(),
) : ViewModel() {

    enum class Phase { WAITING, LISTENING, WRONG, PLAYING_HINT, HINT_DONE, OPENING }

    data class UiState(
        val phase: Phase = Phase.WAITING,
        val failures: Int = 0,
        /** Cambia en cada fallo para disparar la sacudida de la puerta. */
        val shakeCount: Int = 0,
        val message: String = MSG_START,
    )

    private val session = UnlockSession(secret, matcher, FAILURES_BEFORE_HINT)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var idleJob: Job? = null
    private var hintJob: Job? = null

    /** Cada toque sobre la puerta llega aquí con su marca de tiempo (ms, reloj monotónico). */
    fun onKnock(uptimeMs: Long) {
        val phase = _state.value.phase
        if (phase == Phase.PLAYING_HINT || phase == Phase.OPENING) return

        when (val event = session.onTap(uptimeMs)) {
            is Event.TapRegistered -> {
                _state.update { it.copy(phase = Phase.LISTENING) }
                restartIdleTimer()
            }
            Event.Unlocked -> {
                idleJob?.cancel()
                _state.update { it.copy(phase = Phase.OPENING, message = "¡Ábrete, sésamo!") }
            }
            is Event.Failed -> onFailed(event)
        }
    }

    private fun restartIdleTimer() {
        idleJob?.cancel()
        idleJob = viewModelScope.launch {
            delay(session.idleTimeoutMs)
            when (val event = session.onIdle()) {
                is Event.Failed -> onFailed(event)
                else -> Unit
            }
        }
    }

    private fun onFailed(event: Event.Failed) {
        idleJob?.cancel()
        val hint = event.hint
        if (hint == null) {
            val left = event.attemptsUntilHint(FAILURES_BEFORE_HINT)
            val text = if (left == 1) "Ritmo incorrecto. 1 intento más y la puerta te dará una pista"
            else "Ritmo incorrecto. Te quedan $left intentos antes de la pista"
            _state.update {
                it.copy(phase = Phase.WRONG, failures = event.consecutiveFailures, shakeCount = it.shakeCount + 1, message = text)
            }
            return
        }

        // 3 fallos seguidos: la puerta toca la mitad del ritmo desde dentro.
        _state.update {
            it.copy(
                phase = Phase.PLAYING_HINT,
                failures = event.consecutiveFailures,
                shakeCount = it.shakeCount + 1,
                message = "Shhh… alguien llama desde dentro. Escucha",
            )
        }
        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            delay(900) // deja terminar la sacudida antes de que suene la pista
            val knockTimes = player.play(hint)
            session.onHintPlayed(knockTimes)
            _state.update {
                it.copy(phase = Phase.HINT_DONE, message = "Ahora sigue tú: completa el resto del ritmo (o tócalo entero)")
            }
        }
    }

    /** Vuelve a la puerta cerrada (p. ej. si el usuario canceló el PIN del sistema). */
    fun reset() {
        idleJob?.cancel()
        hintJob?.cancel()
        player.stop()
        session.reset()
        _state.value = UiState()
    }

    override fun onCleared() {
        player.stop()
    }

    companion object {
        const val FAILURES_BEFORE_HINT = 3
        private const val MSG_START = "Toca tu ritmo secreto en la puerta"

        fun factory(secret: Rhythm, matcher: RhythmMatcher) = viewModelFactory {
            initializer { LockViewModel(secret, matcher) }
        }
    }
}
