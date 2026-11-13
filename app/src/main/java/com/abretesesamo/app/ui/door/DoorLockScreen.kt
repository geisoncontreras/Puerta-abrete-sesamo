package com.abretesesamo.app.ui.door

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abretesesamo.app.lock.LockViewModel
import com.abretesesamo.app.lock.LockViewModel.Phase
import com.abretesesamo.app.model.DoorStyle
import com.abretesesamo.app.model.HandStyle

/** Pantalla de bloqueo: la puerta, el mensaje de estado y la salida al PIN del sistema. */
@Composable
fun DoorLockScreen(
    viewModel: LockViewModel,
    doorStyle: DoorStyle,
    handStyle: HandStyle,
    showSystemUnlock: Boolean,
    onDoorOpened: () -> Unit,
    onUseSystemUnlock: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current

    LaunchedEffect(state.shakeCount) {
        if (state.shakeCount > 0) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        DoorScene(
            doorStyle = doorStyle,
            handStyle = handStyle,
            opening = state.phase == Phase.OPENING,
            shakeCount = state.shakeCount,
            knockEnabled = state.phase != Phase.OPENING,
            onKnock = { t ->
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                viewModel.onKnock(t)
            },
            onOpenFinished = onDoorOpened,
            onDoorSwing = viewModel::onDoorSwing,
        )

        Text(
            text = state.message,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 36.dp, start = 28.dp, end = 28.dp)
                .fillMaxWidth()
                .background(Color(0x99000000), RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )

        if (showSystemUnlock && state.phase != Phase.OPENING) {
            SystemUnlockButton(
                onConfirm = onUseSystemUnlock,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp),
            )
        }
    }
}

/**
 * Salida de emergencia: lleva al PIN/huella del sistema. Necesita dos toques seguidos para
 * no activarse por accidente mientras se toca el ritmo. Solo se muestra si el teléfono tiene
 * un bloqueo seguro, porque entonces saltarse la puerta sigue exigiendo el PIN.
 */
@Composable
private fun SystemUnlockButton(onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            kotlinx.coroutines.delay(2500)
            armed = false
        }
    }
    TextButton(
        onClick = { if (armed) onConfirm() else armed = true },
        modifier = modifier.background(Color(0x88000000), RoundedCornerShape(50)),
    ) {
        Text(
            if (armed) "Toca otra vez para usar el PIN" else "Usar PIN del teléfono",
            color = Color(0xFFE8E0D4),
            fontSize = 13.sp,
        )
    }
}
