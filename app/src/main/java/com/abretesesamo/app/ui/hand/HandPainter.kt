package com.abretesesamo.app.ui.hand

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.abretesesamo.app.model.HandStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dibuja una mano vista de frente (palma hacia el usuario, dedos hacia arriba), encuadrada
 * desde el antebrazo hasta los dedos.
 *
 * @param wrist   punto de la muñeca en pantalla.
 * @param palmW   ancho de la palma en px (todo lo demás es proporcional).
 * @param rotationDeg inclinación de la mano.
 * @param time    segundos desde que empezó la animación (para auras, temblores, etc.).
 *
 * El antebrazo se prolonga 4 palmas hacia abajo, de modo que sale por el borde inferior.
 */
fun DrawScope.drawHand(style: HandStyle, wrist: Offset, palmW: Float, rotationDeg: Float, time: Float = 0f) {
    translate(wrist.x, wrist.y) {
        rotate(rotationDeg, pivot = Offset.Zero) {
            when (style) {
                HandStyle.BRAZO_REAL -> Unit // es una foto: se dibuja aparte (DoorScene / ajustes)
                HandStyle.ESQUELETO -> skeletonHand(palmW)
                HandStyle.CARTOON_AMARILLA -> cartoonYellowHand(palmW)
                HandStyle.PULSERA_ANIME -> animeBraceletHand(palmW, time)
                HandStyle.MONSTRUO_VERDE -> greenMonsterHand(palmW)
                HandStyle.GUANTE_BLANCO -> whiteGloveHand(palmW)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Mano "de carne" genérica, parametrizada. Coordenadas locales: (0,0) = centro de la muñeca,
// y negativa hacia los dedos.
// ---------------------------------------------------------------------------------------------

private class FleshSpec(
    val skin: Color,
    val outline: Color,
    val fingerLengths: List<Float>, // en palmas; su número = dedos sin contar el pulgar
    val fingerWidth: Float,         // en palmas
    val forearmWidth: Float = 0.74f,
    val nails: Color? = null,
    val knuckles: Boolean = false,
)

private fun DrawScope.fleshHand(spec: FleshSpec, pw: Float, drawForearm: Boolean = true) {
    val stroke = pw * 0.035f
    val palmTop = -pw
    fun fillAndOutline(path: Path, color: Color = spec.skin) {
        drawPath(path, color)
        drawPath(path, spec.outline, style = Stroke(width = stroke))
    }

    // Antebrazo
    if (drawForearm) {
        val fw = pw * spec.forearmWidth
        val arm = Path().apply {
            moveTo(-fw / 2f, -pw * 0.1f)
            lineTo(-fw * 0.62f, pw * 4f)
            lineTo(fw * 0.62f, pw * 4f)
            lineTo(fw / 2f, -pw * 0.1f)
            close()
        }
        fillAndOutline(arm)
    }

    // Pulgar (cápsula inclinada hacia fuera, en el lado izquierdo de la palma)
    val thumbBase = Offset(-pw * 0.42f, -pw * 0.3f)
    rotate(-38f, pivot = thumbBase) {
        val tw = spec.fingerWidth * pw * 1.12f
        val tl = pw * 0.62f
        val thumb = Path().apply {
            addRoundRect(RoundRect(thumbBase.x - tw / 2, thumbBase.y - tl, thumbBase.x + tw / 2, thumbBase.y + tw * 0.4f, CornerRadius(tw / 2)))
        }
        fillAndOutline(thumb)
        spec.nails?.let { drawRoundRect(it, Offset(thumbBase.x - tw * 0.28f, thumbBase.y - tl + tw * 0.14f), Size(tw * 0.56f, tw * 0.45f), CornerRadius(tw * 0.22f)) }
    }

    // Dedos
    val count = spec.fingerLengths.size
    val spacing = pw * 0.92f / count
    spec.fingerLengths.forEachIndexed { i, lenPalms ->
        val cx = -pw * 0.46f + spacing * (i + 0.5f)
        val fw = spec.fingerWidth * pw
        val len = lenPalms * pw
        val top = palmTop - len
        val finger = Path().apply {
            addRoundRect(RoundRect(cx - fw / 2, top, cx + fw / 2, palmTop + pw * 0.3f, CornerRadius(fw / 2)))
        }
        fillAndOutline(finger)
        // Pliegue de la articulación
        drawLine(spec.outline.copy(alpha = 0.6f), Offset(cx - fw * 0.22f, top + len * 0.45f), Offset(cx + fw * 0.22f, top + len * 0.45f), strokeWidth = stroke * 0.7f, cap = StrokeCap.Round)
        spec.nails?.let { drawRoundRect(it, Offset(cx - fw * 0.3f, top + fw * 0.12f), Size(fw * 0.6f, fw * 0.5f), CornerRadius(fw * 0.25f)) }
    }

    // Palma (encima de las bases de los dedos)
    val palm = Path().apply {
        addRoundRect(RoundRect(Rect(-pw / 2, palmTop, pw / 2, pw * 0.05f), CornerRadius(pw * 0.24f)))
    }
    fillAndOutline(palm)
    if (spec.knuckles) {
        spec.fingerLengths.indices.forEach { i ->
            val cx = -pw * 0.46f + spacing * (i + 0.5f)
            drawCircle(spec.outline.copy(alpha = 0.35f), pw * 0.05f, Offset(cx, palmTop + pw * 0.1f))
        }
    }
    // Líneas de la palma
    drawArc(spec.outline.copy(alpha = 0.45f), 200f, 110f, false, Offset(-pw * 0.3f, -pw * 0.7f), Size(pw * 0.6f, pw * 0.5f), style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round))
    drawArc(spec.outline.copy(alpha = 0.45f), 210f, 100f, false, Offset(-pw * 0.25f, -pw * 0.5f), Size(pw * 0.55f, pw * 0.6f), style = Stroke(width = stroke * 0.8f, cap = StrokeCap.Round))
}

// ---------------------------------------------------------------------------------------------
// Estilos
// ---------------------------------------------------------------------------------------------

/** Amarilla de dibujo animado: 3 dedos + pulgar, contorno negro grueso. */
private fun DrawScope.cartoonYellowHand(pw: Float) {
    fleshHand(
        FleshSpec(
            skin = Color(0xFFFFD521),
            outline = Color(0xFF1B1B1B),
            fingerLengths = listOf(0.62f, 0.7f, 0.6f),
            fingerWidth = 0.28f,
        ),
        pw,
    )
}

/** Mano humana con pulsera azul gruesa y aura de energía. */
private fun DrawScope.animeBraceletHand(pw: Float, time: Float) {
    // Aura pulsante detrás de la mano
    val pulse = 1f + 0.08f * sin(time * 12f)
    drawCircle(
        Brush.radialGradient(listOf(Color(0x9966CCFF), Color(0x3366CCFF), Color.Transparent), center = Offset(0f, -pw * 0.9f), radius = pw * 2.1f * pulse),
        radius = pw * 2.1f * pulse,
        center = Offset(0f, -pw * 0.9f),
    )
    // Líneas de velocidad
    for (k in 0 until 10) {
        val a = (k / 10f) * 2 * PI.toFloat() + time * 1.5f
        val r1 = pw * 1.5f
        val r2 = pw * (2.1f + 0.3f * sin(time * 9f + k))
        val c = Offset(0f, -pw * 0.9f)
        drawLine(Color(0x88BFE6FF), Offset(c.x + cos(a) * r1, c.y + sin(a) * r1), Offset(c.x + cos(a) * r2, c.y + sin(a) * r2), strokeWidth = pw * 0.025f, cap = StrokeCap.Round)
    }
    fleshHand(
        FleshSpec(
            skin = Color(0xFFF6C79E),
            outline = Color(0xFF3A2416),
            fingerLengths = listOf(0.72f, 0.82f, 0.76f, 0.58f),
            fingerWidth = 0.2f,
        ),
        pw,
    )
    // Pulsera azul
    val band = Rect(-pw * 0.47f, pw * 0.2f, pw * 0.47f, pw * 0.75f)
    drawRoundRect(Color(0xFF1F5FE0), band.topLeft, band.size, CornerRadius(pw * 0.08f))
    drawRoundRect(Color(0xFF7FB0FF), Offset(band.left + pw * 0.06f, band.top + pw * 0.08f), Size(band.width - pw * 0.12f, pw * 0.07f), CornerRadius(pw * 0.035f))
    drawRoundRect(Color(0xFF0D2A66), band.topLeft, band.size, CornerRadius(pw * 0.08f), style = Stroke(width = pw * 0.035f))
}

/** Mano de monstruo verde: dedos gruesos, nudillos, uñas oscuras, venas y manga rota. */
private fun DrawScope.greenMonsterHand(pw: Float) {
    val outline = Color(0xFF1E3A12)
    fleshHand(
        FleshSpec(
            skin = Color(0xFF5FA83A),
            outline = outline,
            fingerLengths = listOf(0.68f, 0.75f, 0.7f, 0.55f),
            fingerWidth = 0.25f,
            forearmWidth = 0.92f,
            nails = Color(0xFF2C2C2C),
            knuckles = true,
        ),
        pw,
    )
    // Venas
    val vein = Path().apply {
        moveTo(-pw * 0.12f, pw * 0.2f)
        quadraticTo(-pw * 0.25f, pw * 0.8f, -pw * 0.08f, pw * 1.3f)
        quadraticTo(pw * 0.05f, pw * 1.7f, -pw * 0.05f, pw * 2.2f)
        moveTo(pw * 0.15f, pw * 0.4f)
        quadraticTo(pw * 0.28f, pw * 0.9f, pw * 0.18f, pw * 1.5f)
    }
    drawPath(vein, Color(0xFF3B7A22), style = Stroke(width = pw * 0.045f, cap = StrokeCap.Round))
    // Manga rota
    val rag = Path().apply {
        moveTo(-pw * 0.7f, pw * 4f)
        lineTo(-pw * 0.66f, pw * 2.4f)
        lineTo(-pw * 0.45f, pw * 2.65f)
        lineTo(-pw * 0.3f, pw * 2.3f)
        lineTo(-pw * 0.1f, pw * 2.6f)
        lineTo(pw * 0.08f, pw * 2.25f)
        lineTo(pw * 0.28f, pw * 2.62f)
        lineTo(pw * 0.48f, pw * 2.35f)
        lineTo(pw * 0.68f, pw * 2.55f)
        lineTo(pw * 0.72f, pw * 4f)
        close()
    }
    drawPath(rag, Color(0xFF4B3A63))
    drawPath(rag, Color(0xFF221A2E), style = Stroke(width = pw * 0.03f))
}

/** Guante blanco clásico de dibujo animado: 3 dedos, costuras y puño acampanado. */
private fun DrawScope.whiteGloveHand(pw: Float) {
    val outline = Color(0xFF111111)
    // Manga negra
    val sleeve = Path().apply {
        moveTo(-pw * 0.36f, pw * 0.4f)
        lineTo(-pw * 0.46f, pw * 4f)
        lineTo(pw * 0.46f, pw * 4f)
        lineTo(pw * 0.36f, pw * 0.4f)
        close()
    }
    drawPath(sleeve, Color(0xFF151515))
    fleshHand(
        FleshSpec(
            skin = Color(0xFFFAFAFA),
            outline = outline,
            fingerLengths = listOf(0.6f, 0.68f, 0.6f),
            fingerWidth = 0.29f,
        ),
        pw,
        drawForearm = false,
    )
    // Costuras
    listOf(-0.16f, 0f, 0.16f).forEach { fx ->
        drawLine(outline, Offset(pw * fx, -pw * 0.62f), Offset(pw * fx, -pw * 0.2f), strokeWidth = pw * 0.03f, cap = StrokeCap.Round)
    }
    // Puño acampanado
    val cuff = Path().apply {
        moveTo(-pw * 0.4f, -pw * 0.02f)
        lineTo(pw * 0.4f, -pw * 0.02f)
        lineTo(pw * 0.58f, pw * 0.55f)
        quadraticTo(0f, pw * 0.68f, -pw * 0.58f, pw * 0.55f)
        close()
    }
    drawPath(cuff, Color(0xFFFAFAFA))
    drawPath(cuff, outline, style = Stroke(width = pw * 0.035f))
    listOf(-0.25f, 0f, 0.25f).forEach { fx ->
        drawLine(outline.copy(alpha = 0.5f), Offset(pw * fx * 0.9f, pw * 0.05f), Offset(pw * fx * 1.2f, pw * 0.5f), strokeWidth = pw * 0.02f)
    }
}

/** Mano de esqueleto: radio y cúbito, carpos, metacarpos y falanges. */
private fun DrawScope.skeletonHand(pw: Float) {
    val bone = Color(0xFFEDE6D3)
    val edge = Color(0xFF6B6253)
    fun boneSeg(a: Offset, b: Offset, width: Float) {
        drawLine(edge, a, b, strokeWidth = width * 1.3f, cap = StrokeCap.Round)
        drawLine(bone, a, b, strokeWidth = width, cap = StrokeCap.Round)
    }
    fun joint(c: Offset, r: Float) {
        drawCircle(edge, r * 1.3f, c)
        drawCircle(bone, r, c)
    }
    // Radio y cúbito
    boneSeg(Offset(-pw * 0.17f, pw * 0.3f), Offset(-pw * 0.2f, pw * 4f), pw * 0.15f)
    boneSeg(Offset(pw * 0.17f, pw * 0.3f), Offset(pw * 0.2f, pw * 4f), pw * 0.12f)
    joint(Offset(-pw * 0.17f, pw * 0.28f), pw * 0.11f)
    joint(Offset(pw * 0.17f, pw * 0.28f), pw * 0.09f)
    // Carpos (huesitos de la muñeca)
    listOf(
        Offset(-0.22f, 0.05f), Offset(0f, 0.08f), Offset(0.2f, 0.05f),
        Offset(-0.26f, -0.14f), Offset(-0.08f, -0.12f), Offset(0.1f, -0.13f), Offset(0.27f, -0.12f),
    ).forEach { joint(Offset(it.x * pw, it.y * pw), pw * 0.075f) }
    // Dedos: metacarpo + 3 falanges
    val lengths = listOf(0.72f, 0.82f, 0.76f, 0.6f)
    lengths.forEachIndexed { i, l ->
        val baseX = -pw * 0.18f + i * pw * 0.12f
        val knuckleX = -pw * 0.36f + i * pw * 0.24f
        val base = Offset(baseX, -pw * 0.25f)
        val knuckle = Offset(knuckleX, -pw * 0.92f)
        boneSeg(base, knuckle, pw * 0.08f)
        joint(knuckle, pw * 0.055f)
        var p = knuckle
        val dx = (knuckleX - baseX) * 0.12f
        listOf(0.42f, 0.32f, 0.26f).forEach { f ->
            val next = Offset(p.x + dx, p.y - l * pw * f)
            boneSeg(Offset(p.x, p.y - pw * 0.05f), next, pw * 0.07f)
            joint(next, pw * 0.042f)
            p = Offset(next.x, next.y - pw * 0.03f)
        }
    }
    // Pulgar
    val t0 = Offset(-pw * 0.26f, -pw * 0.2f)
    val t1 = Offset(-pw * 0.5f, -pw * 0.52f)
    val t2 = Offset(-pw * 0.66f, -pw * 0.76f)
    val t3 = Offset(-pw * 0.76f, -pw * 0.95f)
    boneSeg(t0, t1, pw * 0.085f)
    joint(t1, pw * 0.05f)
    boneSeg(t1, t2, pw * 0.075f)
    joint(t2, pw * 0.045f)
    boneSeg(t2, t3, pw * 0.065f)
}
