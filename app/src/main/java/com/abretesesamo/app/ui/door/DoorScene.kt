package com.abretesesamo.app.ui.door

import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.abretesesamo.app.R
import com.abretesesamo.app.model.DoorStyle
import com.abretesesamo.app.model.HandStyle
import com.abretesesamo.app.ui.hand.HandMotion
import com.abretesesamo.app.ui.hand.drawHand
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Ángulo máximo de apertura de la hoja (grados). */
private const val DOOR_MAX_ANGLE = 70f
/** Distancia de la "cámara", en anchos de puerta. */
private const val CAMERA_DISTANCE = 2.2f

/**
 * La escena completa. La puerta ocupa TODA la pantalla, borde a borde, sin pared ni marco,
 * para que el teléfono parezca la propia puerta.
 *
 * Capas, de atrás hacia delante:
 *   1. Interior oscuro (o iluminado en las puertas dibujadas)
 *   2. Brazo real (detrás de la hoja: asoma por el hueco que deja al abrirse)
 *   3. Hoja de la puerta, girando sobre las bisagras de la DERECHA con perspectiva.
 *      La perspectiva se calcula aquí (Matrix.setPolyToPoly) para saber exactamente
 *      dónde queda el borde libre y que el brazo empuje justo ahí.
 *   4. Ondas de los toques
 *   5. Manos dibujadas (por delante)
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
    val door = remember { Animatable(0f) }       // 0 cerrada · 1 abierta
    val hand = remember { Animatable(0f) }       // 0 oculta · 1 empujando
    val doorShake = remember { Animatable(1f) }  // sacudida por ritmo incorrecto
    val sceneShake = remember { Animatable(1f) } // portazo del monstruo
    val wave = remember { Animatable(0f) }       // saludo final del guante
    val retract = remember { Animatable(0f) }    // el brazo real vuelve a la oscuridad
    val openHand = remember { Animatable(0f) }   // el puño se abre (cuadro 5 del modelo)
    val releaseTip = remember { mutableStateOf<Float?>(null) } // dónde soltó la puerta
    val clock = remember { mutableFloatStateOf(0f) }
    val ripples = remember { mutableStateListOf<Pair<Long, Offset>>() }
    val rippleId = remember { mutableLongStateOf(0L) }
    val motion = remember(handStyle) { HandMotion.forStyle(handStyle) }

    val photo = ImageBitmap.imageResource(R.drawable.puerta_modelo)
    val arm = ImageBitmap.imageResource(R.drawable.brazo_real)
    val armOpen = ImageBitmap.imageResource(R.drawable.mano_abierta)
    // Las puertas dibujadas se pasan a imagen una vez, para poder deformarlas con perspectiva.
    val doorBitmap = remember { mutableStateOf<Pair<IntSize, ImageBitmap>?>(null) }

    LaunchedEffect(shakeCount) {
        if (shakeCount > 0) {
            doorShake.snapTo(0f)
            doorShake.animateTo(1f, tween(450))
        }
    }

    LaunchedEffect(opening) {
        if (!opening) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { clock.floatValue = (it - start) / 1_000_000_000f }
    }

    LaunchedEffect(opening) {
        if (!opening) {
            door.snapTo(0f); hand.snapTo(0f); wave.snapTo(0f); retract.snapTo(0f); openHand.snapTo(0f)
            releaseTip.value = null
            return@LaunchedEffect
        }
        if (handStyle == HandStyle.BRAZO_REAL) {
            // Igual que el modelo: puño que empuja → abre la mano → se retira a la oscuridad
            // mientras la puerta sigue abriéndose sola por el empujón.
            coroutineScope {
                door.animateTo(0.22f, tween(350, easing = FastOutSlowInEasing))   // se entreabre
                val handIn = launch { hand.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
                delay(250)
                door.animateTo(0.75f, tween(1200, easing = FastOutSlowInEasing))  // empuja con el puño
                handIn.join()
                launch { openHand.animateTo(1f, tween(220)) }                      // abre la mano
                door.animateTo(0.83f, tween(350, easing = FastOutSlowInEasing))   // último empujón
                releaseTip.value = Float.NaN                                       // la suelta
                val inertia = launch { door.animateTo(1f, tween(1500, easing = LinearOutSlowInEasing)) }
                delay(150)
                retract.animateTo(1f, tween(800, easing = FastOutSlowInEasing))   // el brazo se va
                inertia.join()
                delay(300)
            }
            onOpenFinished()
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
            if (motion.retractAtEnd) {
                delay(250)
                retract.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            }
            delay(400)
        }
        onOpenFinished()
    }

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { translationX = shakeOffset(sceneShake.value) * size.width * 0.04f }
            .pointerInput(knockEnabled) {
                if (!knockEnabled) return@pointerInput
                // Cada dedo que baja es un toque; usamos la marca de tiempo del propio evento.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onKnock(down.uptimeMillis)
                    rippleId.longValue += 1
                    ripples += rippleId.longValue to down.position
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val p = door.value

            // 1. Interior
            if (doorStyle == DoorStyle.FOTO_MODELO) {
                drawRect(Brush.radialGradient(listOf(Color(0xFF1C1712), Color(0xFF050403)), center = Offset(w * 0.4f, h * 0.45f), radius = h * 0.7f))
            } else {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFE6A8), Color(0xFFC07A36), Color(0xFF1A0E06)),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = h * 0.75f,
                    )
                )
                drawRect(Color.Black.copy(alpha = (1f - p * 1.6f).coerceIn(0f, 1f)))
            }

            // 2. Brazo real, detrás de la hoja
            if (handStyle == HandStyle.BRAZO_REAL && hand.value > 0f) {
                // Los dos recortes (puño y mano abierta) a la misma escala que en el modelo.
                val k = h * 0.0016f
                val baseTop = h * 0.5f - 0.8f * 135f * k
                val target = freeEdgeX(p, w) - w * 0.012f
                val released = releaseTip.value
                val tip = if (released != null) {
                    val start = if (released.isNaN()) min(target, w).also { releaseTip.value = it } else released
                    start - retract.value * w * 0.4f
                } else {
                    min(target, lerp(-0.05f * w, target, hand.value))
                }
                val a = (hand.value * 3f).coerceIn(0f, 1f) * (1f - retract.value)
                fun place(img: ImageBitmap, sw: Float, sh: Float, dy: Float, alpha: Float) {
                    if (alpha <= 0f) return
                    drawImage(
                        img,
                        dstOffset = IntOffset((tip - sw * 0.97f).toInt(), (baseTop + dy).toInt()),
                        dstSize = IntSize(sw.toInt(), sh.toInt()),
                        alpha = alpha.coerceIn(0f, 1f),
                    )
                }
                place(arm, 114f * k, 135f * k, 0f, a * (1f - openHand.value))
                place(armOpen, 128f * k, 155f * k, 5f * k, a * openHand.value)
            }

            // 3. Hoja de la puerta
            val source: ImageBitmap = if (doorStyle == DoorStyle.FOTO_MODELO) {
                photo
            } else {
                val key = IntSize((w / 2).toInt().coerceAtLeast(1), (h / 2).toInt().coerceAtLeast(1))
                doorBitmap.value?.takeIf { it.first == key }?.second
                    ?: renderDoor(doorStyle, key, density, layoutDirection).also { doorBitmap.value = key to it }
            }
            val shake = shakeOffset(doorShake.value) * w * 0.03f
            drawDoorLeaf(source, p, shake)

            // 5. Manos dibujadas, reflejadas para salir por la izquierda (donde se abre el hueco)
            if (handStyle != HandStyle.BRAZO_REAL && hand.value > 0f) {
                val hp = hand.value
                val scale = 0.55f + 0.45f * hp
                val t = clock.floatValue
                var rotation = lerp(-18f, -6f, hp)
                if (motion.jitterDeg > 0f) rotation += motion.jitterDeg * sin(t * 40f) * (1.2f - hp.coerceAtMost(1f))
                if (wave.value > 0f) rotation += 14f * sin(wave.value * 4f * PI.toFloat()) * (1f - wave.value)
                scale(scaleX = -1f, scaleY = 1f) {
                    drawHand(
                        handStyle,
                        Offset(lerp(w * 0.86f, w * 0.52f, hp), lerp(h * 0.8f, h * 0.66f, hp)),
                        palmW = w * 0.34f * scale,
                        rotationDeg = rotation,
                        time = if (motion.aura) t else 0f,
                    )
                }
            }
        }

        // 4. Ondas de cada toque
        ripples.forEach { (id, pos) ->
            key(id) { KnockRipple(pos) { ripples.removeAll { it.first == id } } }
        }
    }
}

/**
 * Proyecta el borde libre (izquierdo) de la hoja cuando gira [p] (0..1) hacia el usuario
 * sobre las bisagras de la derecha. Misma fórmula que la versión web.
 */
private fun freeEdge(p: Float, w: Float, h: Float): Triple<Float, Float, Float> {
    val th = DOOR_MAX_ANGLE * p * PI.toFloat() / 180f
    val camera = w * CAMERA_DISTANCE
    val x = w - w * cos(th)
    val z = w * sin(th)
    val f = camera / (camera - z)
    val cx = w / 2f
    val cy = h / 2f
    return Triple(cx + (x - cx) * f, cy + (0f - cy) * f, cy + (h - cy) * f)
}

private fun freeEdgeX(p: Float, w: Float): Float = freeEdge(p, w, 1f).first

/** Dibuja la imagen de la hoja deformada en perspectiva y la oscurece al girar. */
private fun DrawScope.drawDoorLeaf(image: ImageBitmap, p: Float, shakeX: Float) {
    val w = size.width
    val h = size.height
    val (ex, top, bottom) = freeEdge(p, w, h)
    val bmp = image.asAndroidBitmap()
    val bw = bmp.width.toFloat()
    val bh = bmp.height.toFloat()
    val matrix = Matrix()
    matrix.setPolyToPoly(
        floatArrayOf(0f, 0f, bw, 0f, bw, bh, 0f, bh), 0,
        floatArrayOf(ex + shakeX, top, w + shakeX, 0f, w + shakeX, h, ex + shakeX, bottom), 0,
        4,
    )
    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        native.save()
        native.concat(matrix)
        native.drawBitmap(bmp, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        native.restore()
    }
    if (p > 0f) {
        val leaf = Path().apply {
            moveTo(ex + shakeX, top); lineTo(w + shakeX, 0f); lineTo(w + shakeX, h); lineTo(ex + shakeX, bottom); close()
        }
        drawPath(leaf, Color.Black.copy(alpha = 0.4f * p))
    }
}

/** Pasa una puerta dibujada por código a imagen (una vez por tamaño). */
private fun renderDoor(style: DoorStyle, size: IntSize, density: Float, layoutDirection: LayoutDirection): ImageBitmap {
    val bitmap = ImageBitmap(size.width, size.height)
    CanvasDrawScope().draw(Density(density), layoutDirection, ComposeCanvas(bitmap), Size(size.width.toFloat(), size.height.toFloat())) {
        drawDoor(style)
    }
    return bitmap
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
