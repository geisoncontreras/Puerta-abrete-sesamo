package com.abretesesamo.app.ui.door

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntSize
import com.abretesesamo.app.model.DoorStyle
import kotlin.random.Random

/*
 * Puertas dibujadas por código (sin imágenes): escalan a cualquier pantalla y la app pesa poco.
 * Para usar fotos/texturas reales basta con cambiar drawDoor() por un drawImage().
 */

/**
 * Dibuja la hoja de la puerta ocupando todo el DrawScope (borde a borde, sin marco).
 * Todas tienen las bisagras a la derecha, como el modelo: las dibujadas se reflejan.
 */
fun DrawScope.drawDoor(style: DoorStyle, photo: ImageBitmap? = null) {
    if (style == DoorStyle.FOTO_MODELO) {
        if (photo != null) {
            drawImage(photo, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
        } else {
            drawRect(Color(0xFFB8936A))
        }
        return
    }
    scale(scaleX = -1f, scaleY = 1f) {
        when (style) {
            DoorStyle.MADERA_RUSTICA -> drawRusticWood()
            DoorStyle.METAL_INDUSTRIAL -> drawIndustrialMetal()
            DoorStyle.MEDIEVAL -> drawMedieval()
            DoorStyle.MODERNA -> drawModern()
            DoorStyle.FOTO_MODELO -> Unit
        }
    }
}

/** Color de la pared/marco alrededor de cada puerta. */
fun DoorStyle.wallColor(): Color = when (this) {
    DoorStyle.FOTO_MODELO -> Color(0xFF6E7B6A)
    DoorStyle.MADERA_RUSTICA -> Color(0xFF3B2414)
    DoorStyle.METAL_INDUSTRIAL -> Color(0xFF4A4F55)
    DoorStyle.MEDIEVAL -> Color(0xFF6B655C)
    DoorStyle.MODERNA -> Color(0xFFD9D4CC)
}

/** Forma de la puerta medieval: rectángulo con arco de medio punto arriba. */
fun archPath(w: Float, h: Float): Path = Path().apply {
    moveTo(0f, h)
    lineTo(0f, w / 2f)
    arcTo(Rect(0f, 0f, w, w), 180f, 180f, false)
    lineTo(w, h)
    close()
}

/** Las "esquinas" de piedra por encima del arco medieval. */
fun archSpandrelsPath(w: Float): Path = Path().apply {
    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, w / 2f)
    arcTo(Rect(0f, 0f, w, w), 0f, -180f, false)
    close()
}

private fun DrawScope.drawRusticWood() {
    val w = size.width
    val h = size.height
    val planks = 5
    val pw = w / planks
    val colors = listOf(Color(0xFF8B5A2B), Color(0xFF7A4E25), Color(0xFF946235), Color(0xFF6F4520), Color(0xFF85552A))
    for (i in 0 until planks) {
        val left = i * pw
        drawRect(colors[i], Offset(left, 0f), Size(pw, h))
        // Vetas de la madera
        val rnd = Random(i * 31 + 7)
        repeat(8) {
            var x = left + rnd.nextFloat() * pw
            val grain = Path().apply {
                moveTo(x, 0f)
                var y = 0f
                while (y < h) {
                    y += h / 14f
                    x = (x + (rnd.nextFloat() - 0.5f) * pw * 0.18f).coerceIn(left + 2f, left + pw - 2f)
                    lineTo(x, y)
                }
            }
            drawPath(grain, Color(0x2E000000), style = Stroke(width = w * 0.004f))
        }
        // Nudo
        val knot = Offset(left + pw * (0.3f + rnd.nextFloat() * 0.4f), h * (0.3f + rnd.nextFloat() * 0.4f))
        drawOval(Color(0x553A200C), Offset(knot.x - pw * 0.08f, knot.y - pw * 0.13f), Size(pw * 0.16f, pw * 0.26f))
        // Separación entre tablas
        drawLine(Color(0xAA2A160A), Offset(left, 0f), Offset(left, h), strokeWidth = w * 0.008f)
    }
    // Travesaños y diagonal en Z
    val brace = Color(0xFF5C3A1A)
    val braceH = h * 0.06f
    val bolt = Color(0xFF2E2E2E)
    drawLine(brace, Offset(w * 0.06f, h * 0.76f), Offset(w * 0.94f, h * 0.24f), strokeWidth = braceH * 0.85f)
    listOf(h * 0.18f, h * 0.76f).forEach { y ->
        drawRect(brace, Offset(0f, y), Size(w, braceH))
        drawLine(Color(0x55000000), Offset(0f, y + braceH), Offset(w, y + braceH), strokeWidth = w * 0.01f)
        listOf(0.08f, 0.5f, 0.92f).forEach { fx -> drawCircle(bolt, w * 0.016f, Offset(w * fx, y + braceH / 2)) }
    }
    // Aldaba de hierro
    val ring = Offset(w * 0.8f, h * 0.52f)
    drawCircle(Color(0xFF242424), w * 0.035f, Offset(ring.x, ring.y - w * 0.06f))
    drawCircle(Color(0xFF2B2B2B), w * 0.065f, ring, style = Stroke(width = w * 0.016f))
    // Sombreado
    drawRect(Brush.horizontalGradient(listOf(Color(0x33000000), Color.Transparent, Color(0x55000000))))
}

private fun DrawScope.drawIndustrialMetal() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF858D97), Color(0xFF5A626B), Color(0xFF707881))))
    drawRect(Color(0xFF3E444B), style = Stroke(width = w * 0.05f))
    // Nervios horizontales
    val ribs = 8
    for (k in 1 until ribs) {
        val y = h * 0.08f + k * (h * 0.38f / ribs)
        drawLine(Color(0x66FFFFFF), Offset(w * 0.1f, y), Offset(w * 0.9f, y), strokeWidth = w * 0.006f)
        drawLine(Color(0x88000000), Offset(w * 0.1f, y + w * 0.008f), Offset(w * 0.9f, y + w * 0.008f), strokeWidth = w * 0.006f)
    }
    // Remaches en los bordes
    var y = h * 0.04f
    while (y < h * 0.97f) {
        listOf(w * 0.05f, w * 0.95f).forEach { x ->
            drawCircle(Color(0xFF9AA3AD), w * 0.013f, Offset(x, y))
            drawCircle(Color(0xFFD5DBE1), w * 0.005f, Offset(x - w * 0.004f, y - w * 0.004f))
        }
        y += h * 0.06f
    }
    // Barra antipánico
    drawRoundRect(Color(0xFF2F3439), Offset(w * 0.1f, h * 0.555f), Size(w * 0.8f, h * 0.045f), CornerRadius(h * 0.02f))
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFE3E7EB), Color(0xFF9EA5AD)), startY = h * 0.56f, endY = h * 0.595f),
        Offset(w * 0.12f, h * 0.56f), Size(w * 0.76f, h * 0.035f), CornerRadius(h * 0.0175f),
    )
    // Franja de peligro amarilla y negra
    val top = h * 0.86f
    val bottom = h * 0.94f
    clipRect(0f, top, w, bottom) {
        drawRect(Color(0xFFF2C230), Offset(0f, top), Size(w, bottom - top))
        val stripe = (bottom - top)
        var x = -stripe * 2
        while (x < w + stripe) {
            drawLine(Color(0xFF1C1C1C), Offset(x, bottom), Offset(x + stripe, top), strokeWidth = stripe * 0.5f)
            x += stripe * 1.4f
        }
    }
    drawRect(Brush.horizontalGradient(listOf(Color(0x22FFFFFF), Color.Transparent, Color(0x44000000))))
}

private fun DrawScope.drawMedieval() {
    val w = size.width
    val h = size.height
    clipPath(archPath(w, h)) {
        val planks = 6
        val pw = w / planks
        for (i in 0 until planks) {
            val shade = if (i % 2 == 0) Color(0xFF4A2F1B) else Color(0xFF553620)
            drawRect(shade, Offset(i * pw, 0f), Size(pw, h))
            drawLine(Color(0xFF24160C), Offset(i * pw, 0f), Offset(i * pw, h), strokeWidth = w * 0.01f)
        }
        // Flejes de hierro con clavos
        val iron = Color(0xFF2A2A2C)
        listOf(0.32f, 0.58f, 0.84f).forEach { fy ->
            val y = h * fy
            drawRect(iron, Offset(0f, y), Size(w, h * 0.035f))
            var x = w * 0.05f
            while (x < w) {
                drawCircle(Color(0xFF4D4D52), w * 0.012f, Offset(x, y + h * 0.0175f))
                x += w * 0.11f
            }
        }
        // Ventanuco con rejas
        val win = Rect(w * 0.38f, h * 0.17f, w * 0.62f, h * 0.25f)
        drawRect(Color(0xFF0F0A06), win.topLeft, win.size)
        for (k in 1..3) {
            val x = win.left + win.width * k / 4f
            drawLine(iron, Offset(x, win.top), Offset(x, win.bottom), strokeWidth = w * 0.012f)
        }
        drawRect(iron, win.topLeft, win.size, style = Stroke(width = w * 0.014f))
        // Gran argolla
        val c = Offset(w * 0.5f, h * 0.47f)
        drawCircle(iron, w * 0.045f, c)
        drawCircle(Color(0xFF3A3A3E), w * 0.1f, Offset(c.x, c.y + w * 0.1f), style = Stroke(width = w * 0.022f))
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0x66000000)), center = Offset(w / 2, h / 2), radius = h * 0.7f))
    }
}

private fun DrawScope.drawModern() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFFF7F6F3), Color(0xFFE4E1DB))))
    // Paneles en relieve
    val panels = listOf(
        Rect(w * 0.12f, h * 0.06f, w * 0.88f, h * 0.44f),
        Rect(w * 0.12f, h * 0.52f, w * 0.88f, h * 0.94f),
    )
    panels.forEach { r ->
        drawRect(Color(0xFFEDEAE4), r.topLeft, r.size)
        drawLine(Color(0xFFFFFFFF), r.topLeft, Offset(r.right, r.top), strokeWidth = w * 0.01f)
        drawLine(Color(0xFFFFFFFF), r.topLeft, Offset(r.left, r.bottom), strokeWidth = w * 0.01f)
        drawLine(Color(0xFFBDB8AF), Offset(r.left, r.bottom), r.bottomRight, strokeWidth = w * 0.01f)
        drawLine(Color(0xFFBDB8AF), Offset(r.right, r.top), r.bottomRight, strokeWidth = w * 0.01f)
    }
    // Mirilla
    drawCircle(Color(0xFFB8BCC2), w * 0.022f, Offset(w * 0.5f, h * 0.3f))
    drawCircle(Color(0xFF222222), w * 0.01f, Offset(w * 0.5f, h * 0.3f))
    // Manilla cromada
    val rose = Offset(w * 0.84f, h * 0.48f)
    drawCircle(Color(0xFFA9AFB6), w * 0.04f, rose)
    drawLine(Color(0xFF6E747B), Offset(rose.x, rose.y + w * 0.012f), Offset(rose.x - w * 0.2f, rose.y + w * 0.012f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
    drawLine(Color(0xFFD6DADF), rose, Offset(rose.x - w * 0.2f, rose.y), strokeWidth = w * 0.035f, cap = StrokeCap.Round)
    drawRect(Brush.horizontalGradient(listOf(Color(0x11000000), Color.Transparent, Color(0x22000000))))
}
