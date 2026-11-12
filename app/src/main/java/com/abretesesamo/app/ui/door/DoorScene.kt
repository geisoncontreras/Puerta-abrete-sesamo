package com.abretesesamo.app.ui.door

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.abretesesamo.app.model.DoorStyle
import com.abretesesamo.app.model.HandStyle
import com.abretesesamo.app.ui.hand.HandMotion
import com.abretesesamo.app.ui.hand.drawHand
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Ángulo máximo de apertura de la hoja (grados). */
private const val DOOR_MAX_ANGLE = 88f
private val FRAME_SIDE = 14.dp
private val FRAME_TOP = 18.dp

/**
 * La escena completa: interior iluminado, hoja de la puerta (que gira sobre su bisagra
 * izquierda con perspectiva), marco, ondas de cada toque y la mano que empuja.
 *
 * Capas, de atrás hacia delante:
 *   1. Interior (luz cálida que aparece al abrir)
 *   2. Hoja de la puerta  -> graphicsLayer { rotationY, cameraDistance }
 *   3. Marco / pared (tapa los bordes de la hoja, y el arco en la medieval)
 *   4. Ondas de los toques
 *   5. Mano (por delante de todo: sale del hueco hacia el usuario)
 */
@Composable
fun DoorScene(
    doorStyle: DoorStyle,
    handStyle: HandStyle,
    opening: Boolean,
    shakeCount: Int,
    onKnock: (uptimeMs: Long) -> Unit,
    onOpenFinished: () -> Unit,
    modifier: Modifier = Modifier,
    knockEnabled: Boolean = true,
) {
    val door = remember { Animatable(0f) }      // 0 cerrada · 1 abierta
    val hand = remember { Animatable(0f) }      // 0 oculta dentro · 1 empujando (puede pasarse con rebote)
    val doorShake = remember { Animatable(1f) } // sacudida por ritmo incorrecto
    val sceneShake = remember { Animatable(1f) } // portazo del monstruo
    val wave = remember { Animatable(0f) }      // saludo final del guante
    val clock = remember { mutableFloatStateOf(0f) }
    val ripples = remember { mutableStateListOf<Pair<Long, Offset>>() }
    val rippleId = remember { mutableLongStateOf(0L) }
    val motion = remember(handStyle) { HandMotion.forStyle(handStyle) }

    // Sacudida de "no" cuando el ritmo es incorrecto
    LaunchedEffect(shakeCount) {
        if (shakeCount > 0) {
            doorShake.snapTo(0f)
            doorShake.animateTo(1f, tween(450))
        }
    }

    // Reloj para efectos continuos (aura, temblor de huesos)
    LaunchedEffect(opening) {
        if (!opening) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { clock.floatValue = (it - start) / 1_000_000_000f }
    }

    // Coreografía de apertura
    LaunchedEffect(opening) {
        if (!opening) {
            door.snapTo(0f); hand.snapTo(0f); wave.snapTo(0f)
            return@LaunchedEffect
        }
        coroutineScope {
            repeat(motion.preRattles) {
                door.animateTo(0.035f, tween(70))
                door.animateTo(0f, tween(90))
            }
            door.animateTo(0.1f, tween(220, easing = FastOutSlowInEasing)) // se entreabre
            val handJob = launch { hand.animateTo(1f, motion.handSpec) }    // aparece la mano
            delay(motion.doorDelayMs)
            door.animateTo(1f, motion.doorSpec)                              // empuja hasta abrir
            if (motion.slamShake) {
                sceneShake.snapTo(0f)
                sceneShake.animateTo(1f, tween(500))
            }
            handJob.join()
            if (motion.waveAtEnd) wave.animateTo(1f, tween(800))
            delay(450)
        }
        onOpenFinished()
    }

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { translationX = shakeOffset(sceneShake.value) * size.width * 0.04f }
            .pointerInput(knockEnabled) {
                if (!knockEnabled) return@pointerInput
                // Cada dedo que baja sobre la puerta es un toque; usamos la marca de tiempo
                // del propio evento táctil (más precisa que leer el reloj después).
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onKnock(down.uptimeMillis)
                    rippleId.longValue += 1
                    ripples += rippleId.longValue to down.position
                }
            },
    ) {
        // 1. Interior
        Canvas(Modifier.fillMaxSize().padding(start = FRAME_SIDE, end = FRAME_SIDE, top = FRAME_TOP)) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xFFFFE6A8), Color(0xFFC07A36), Color(0xFF1A0E06)),
                    center = Offset(size.width * 0.5f, size.height * 0.45f),
                    radius = size.height * 0.75f,
                )
            )
            // Oscuro mientras está cerrada; se ilumina al abrir
            drawRect(Color.Black.copy(alpha = (1f - door.value * 1.6f).coerceIn(0f, 1f)))
        }

        // 2. Hoja de la puerta con perspectiva
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(start = FRAME_SIDE, end = FRAME_SIDE, top = FRAME_TOP)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0.5f) // bisagra a la izquierda
                    rotationY = -DOOR_MAX_ANGLE * door.value
                    cameraDistance = 36f * density
                    translationX = shakeOffset(doorShake.value) * size.width * 0.03f
                }
        ) {
            drawDoor(doorStyle)
            // La cara se oscurece al girar (se aleja de la luz)
            drawRect(Color.Black.copy(alpha = 0.45f * door.value))
        }

        // 3. Marco y pared
        Canvas(Modifier.fillMaxSize()) {
            val side = FRAME_SIDE.toPx()
            val top = FRAME_TOP.toPx()
            val wall = doorStyle.wallColor()
            drawRect(wall, Offset.Zero, Size(size.width, top))
            drawRect(wall, Offset.Zero, Size(side, size.height))
            drawRect(wall, Offset(size.width - side, 0f), Size(side, size.height))
            drawRect(Color(0x55000000), Offset(side, top), Size(size.width - 2 * side, size.height - top), style = Stroke(width = side * 0.3f))
            if (doorStyle == DoorStyle.MEDIEVAL) {
                val path = archSpandrelsPath(size.width - 2 * side)
                path.translate(Offset(side, top))
                drawPath(path, wall)
            }
        }

        // 4. Ondas de cada toque
        ripples.forEach { (id, pos) ->
            key(id) { KnockRipple(pos) { ripples.removeAll { it.first == id } } }
        }

        // 5. Mano
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (hand.value * 2.5f).coerceIn(0f, 1f) }
        ) {
            if (hand.value <= 0f) return@Canvas
            val p = hand.value
            val w = size.width
            val h = size.height
            val scale = 0.55f + 0.45f * p
            val wrist = Offset(lerp(w * 0.86f, w * 0.52f, p), lerp(h * 0.8f, h * 0.66f, p))
            val t = clock.floatValue
            var rotation = lerp(-18f, -6f, p)
            if (motion.jitterDeg > 0f) rotation += motion.jitterDeg * sin(t * 40f) * (1.2f - p.coerceAtMost(1f))
            if (wave.value > 0f) rotation += 14f * sin(wave.value * 4f * PI.toFloat()) * (1f - wave.value)
            drawHand(handStyle, wrist, palmW = w * 0.34f * scale, rotationDeg = rotation, time = if (motion.aura) t else 0f)
        }
    }
}

@Composable
private fun KnockRipple(center: Offset, onDone: () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        p.animateTo(1f, tween(450))
        onDone()
    }
    Canvas(Modifier.fillMaxSize()) {
        val m = size.minDimension
        drawCircle(
            Color.White.copy(alpha = (1f - p.value) * 0.55f),
            radius = m * 0.04f + p.value * m * 0.16f,
            center = center,
            style = Stroke(width = m * 0.012f * (1f - p.value) + 1f),
        )
    }
}

/** Sacudida amortiguada: 0 al empezar y al terminar (p = 0..1). */
private fun shakeOffset(p: Float): Float =
    if (p >= 1f) 0f else sin(p * PI.toFloat() * 6f) * (1f - p)

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
