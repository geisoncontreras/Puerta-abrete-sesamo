package com.abretesesamo.app.ui.settings

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.abretesesamo.app.R
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.abretesesamo.app.audio.KnockPlayer
import com.abretesesamo.app.model.DoorStyle
import com.abretesesamo.app.ui.door.drawDoor
import com.abretesesamo.core.rhythm.Rhythm
import com.abretesesamo.core.rhythm.RhythmEnroller
import com.abretesesamo.core.rhythm.RhythmMatcher
import kotlinx.coroutines.delay

/** Pausa que marca el final de una toma mientras se graba el ritmo. */
private const val END_OF_TAKE_MS = 2_000L

/**
 * Grabación del ritmo secreto directamente sobre la puerta: el usuario lo toca, espera
 * 2 segundos, lo repite para confirmar y se guarda el promedio.
 */
@Composable
fun RecordRhythmDialog(
    matcher: RhythmMatcher,
    doorStyle: DoorStyle,
    player: KnockPlayer,
    onSaved: (Rhythm) -> Unit,
    onDismiss: () -> Unit,
) {
    val enroller = remember { RhythmEnroller(matcher) }
    val taps = remember { mutableStateListOf<Long>() }
    var message by remember { mutableStateOf("Toca tu ritmo secreto sobre la puerta (mínimo 3 toques).\nCuando termines, espera 2 segundos.") }
    var done by remember { mutableStateOf(false) }
    val view = LocalView.current
    val photo = ImageBitmap.imageResource(doorStyle.photoRes ?: R.drawable.puerta_modelo)

    // Fin de la toma: 2 s sin tocar
    LaunchedEffect(taps.size) {
        if (taps.isEmpty() || done) return@LaunchedEffect
        delay(END_OF_TAKE_MS)
        val step = enroller.submit(taps.toList())
        taps.clear()
        message = when (step) {
            RhythmEnroller.Step.NeedConfirmation -> "¡Bien! Ahora repítelo igual para confirmarlo."
            is RhythmEnroller.Step.Done -> {
                done = true
                onSaved(step.rhythm)
                "¡Ritmo guardado! Tiene ${step.rhythm.knockCount} toques."
            }
            is RhythmEnroller.Step.Rejected -> when (step.reason) {
                RhythmEnroller.Reason.TOO_FEW_KNOCKS -> "Muy corto: usa al menos ${Rhythm.MIN_KNOCKS} toques. Empieza otra vez."
                RhythmEnroller.Reason.TOO_MANY_KNOCKS -> "Muy largo: como máximo ${Rhythm.MAX_KNOCKS} toques. Empieza otra vez."
                RhythmEnroller.Reason.TAKES_DONT_MATCH -> "Las dos veces no se parecen. Empecemos de nuevo: tócalo otra vez."
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(done) {
                        if (done) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            player.tap(down.uptimeMillis)
                            taps += down.uptimeMillis
                        }
                    }
            ) { drawDoor(doorStyle, photo) }

            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(24.dp)
                    .fillMaxWidth()
                    .background(Color(0xCC000000), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(message, color = Color.White, fontSize = 17.sp, textAlign = TextAlign.Center)
                Text(
                    if (enroller.waitingForConfirmation) "Paso 2 de 2 · toques: ${taps.size}" else "Paso 1 de 2 · toques: ${taps.size}",
                    color = Color(0xFFE0A458), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp),
                )
            }

            Row(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (done) {
                    Button(onClick = onDismiss) { Text("Listo") }
                } else {
                    OutlinedButton(onClick = onDismiss) { Text("Cancelar", color = Color.White) }
                    OutlinedButton(onClick = { enroller.reset(); taps.clear(); message = "Empecemos de nuevo: toca tu ritmo." }) {
                        Text("Reiniciar", color = Color.White)
                    }
                }
            }
        }
    }
}
