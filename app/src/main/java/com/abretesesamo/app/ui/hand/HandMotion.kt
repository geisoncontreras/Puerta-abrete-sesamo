package com.abretesesamo.app.ui.hand

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.abretesesamo.app.model.HandStyle

/**
 * "Coreografía" de apertura de cada mano. Todas comparten el mismo guion
 * (la puerta se entreabre -> aparece la mano -> empuja -> la puerta se abre del todo),
 * pero cada estilo cambia el ritmo y añade su efecto propio.
 */
data class HandMotion(
    /** Cómo avanza la mano de 0 (dentro, lejos) a 1 (empujando, cerca). */
    val handSpec: AnimationSpec<Float>,
    /** Cómo gira la puerta hasta abrirse del todo. */
    val doorSpec: AnimationSpec<Float>,
    /** Retraso entre que aparece la mano y la puerta empieza a abrirse del todo. */
    val doorDelayMs: Long,
    /** Golpecitos de la puerta antes de abrirse (el monstruo "forcejea"). */
    val preRattles: Int = 0,
    /** Temblor de los huesos, en grados. */
    val jitterDeg: Float = 0f,
    /** Aura de energía alrededor de la mano. */
    val aura: Boolean = false,
    /** Sacudida de toda la pantalla al terminar de abrir. */
    val slamShake: Boolean = false,
    /** La mano saluda al final. */
    val waveAtEnd: Boolean = false,
) {
    companion object {
        fun forStyle(style: HandStyle): HandMotion = when (style) {
            HandStyle.ESQUELETO -> HandMotion(
                handSpec = tween(750, easing = FastOutSlowInEasing),
                doorSpec = tween(900, easing = FastOutSlowInEasing),
                doorDelayMs = 250,
                jitterDeg = 6f,
            )
            HandStyle.CARTOON_AMARILLA -> HandMotion(
                handSpec = spring(dampingRatio = 0.35f, stiffness = 180f),
                doorSpec = tween(750, easing = FastOutSlowInEasing),
                doorDelayMs = 200,
            )
            HandStyle.PULSERA_ANIME -> HandMotion(
                handSpec = tween(380, easing = FastOutSlowInEasing),
                doorSpec = tween(480, easing = LinearOutSlowInEasing),
                doorDelayMs = 120,
                aura = true,
            )
            HandStyle.MONSTRUO_VERDE -> HandMotion(
                handSpec = tween(420, easing = FastOutSlowInEasing),
                doorSpec = tween(260, easing = LinearOutSlowInEasing),
                doorDelayMs = 150,
                preRattles = 3,
                slamShake = true,
            )
            HandStyle.GUANTE_BLANCO -> HandMotion(
                handSpec = tween(1000, easing = FastOutSlowInEasing),
                doorSpec = tween(1000, easing = FastOutSlowInEasing),
                doorDelayMs = 300,
                waveAtEnd = true,
            )
        }
    }
}
