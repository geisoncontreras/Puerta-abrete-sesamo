package com.abretesesamo.app.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.abretesesamo.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.abretesesamo.app.audio.KnockPlayer
import com.abretesesamo.app.data.SettingsRepository
import com.abretesesamo.app.lock.LockActivity
import com.abretesesamo.app.lock.LockService
import com.abretesesamo.app.model.DoorStyle
import com.abretesesamo.app.model.HandStyle
import com.abretesesamo.app.ui.door.drawDoor
import com.abretesesamo.app.ui.hand.drawHand
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(settings: SettingsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember { KnockPlayer() }
    val doorPhoto = ImageBitmap.imageResource(R.drawable.puerta_modelo)
    val armPhoto = ImageBitmap.imageResource(R.drawable.brazo_real)

    var rhythm by remember { mutableStateOf(settings.rhythm) }
    var tolerance by remember { mutableIntStateOf(settings.tolerancePercent) }
    var door by remember { mutableStateOf(settings.doorStyle) }
    var hand by remember { mutableStateOf(settings.handStyle) }
    var enabled by remember { mutableStateOf(settings.lockEnabled) }
    var recording by remember { mutableStateOf(false) }

    // Los permisos se conceden fuera de la app: los volvemos a mirar al regresar.
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeTick++
        onPauseOrDispose { }
    }
    val canOverlay = remember(resumeTick) { Settings.canDrawOverlays(context) }
    val canNotify = remember(resumeTick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumeTick++ }

    fun setEnabled(on: Boolean) {
        enabled = on
        settings.lockEnabled = on
        if (on) LockService.start(context) else LockService.stop(context)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Puerta Ábrete Sésamo", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Desbloquea tu teléfono tocando un ritmo secreto en una puerta.", color = MaterialTheme.colorScheme.onBackground)

        Section("1. Tu ritmo secreto") {
            Text(rhythm?.let { "Ritmo guardado: ${it.knockCount} toques." } ?: "Todavía no has grabado tu ritmo.")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { recording = true }) { Text(if (rhythm == null) "Grabar ritmo" else "Grabar uno nuevo") }
                rhythm?.let { r ->
                    OutlinedButton(onClick = { scope.launch { player.play(r) } }) { Text("Escucharlo") }
                }
            }
        }

        Section("2. Margen de error: ±$tolerance %") {
            Slider(
                value = tolerance.toFloat(),
                onValueChange = { tolerance = it.toInt() },
                onValueChangeFinished = { settings.tolerancePercent = tolerance },
                valueRange = 10f..40f,
                steps = 5,
            )
            Text("Más alto = más fácil de abrir, pero también más fácil de adivinar.", fontSize = 13.sp)
        }

        Section("3. Elige tu puerta") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(DoorStyle.entries) { style ->
                    Choice(selected = style == door, label = style.displayName, onClick = { door = style; settings.doorStyle = style }) {
                        Canvas(Modifier.size(width = 84.dp, height = 140.dp)) { drawDoor(style, doorPhoto) }
                    }
                }
            }
        }

        Section("4. Elige la mano que abre") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(HandStyle.entries) { style ->
                    Choice(selected = style == hand, label = style.displayName, onClick = { hand = style; settings.handStyle = style }) {
                        val bg = if (style == HandStyle.BRAZO_REAL) Color(0xFF050403) else Color(0xFF3A2A1C)
                        Canvas(Modifier.size(width = 96.dp, height = 140.dp).clip(RoundedCornerShape(10.dp)).background(bg)) {
                            if (style == HandStyle.BRAZO_REAL) {
                                val aw = size.width * 0.95f
                                val ah = aw * armPhoto.height / armPhoto.width
                                drawImage(armPhoto, dstOffset = IntOffset(0, ((size.height - ah) / 2).toInt()), dstSize = IntSize(aw.toInt(), ah.toInt()))
                            } else {
                                drawHand(style, Offset(size.width / 2, size.height * 0.72f), palmW = size.width * 0.42f, rotationDeg = 0f, time = 0.3f)
                            }
                        }
                    }
                }
            }
            Text(hand.description, fontSize = 13.sp)
        }

        Section("5. Permisos") {
            PermissionRow(
                ok = canOverlay,
                text = "Mostrar sobre otras apps (para que la puerta aparezca al bloquear)",
                button = "Dar permiso",
            ) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
            }
            if (Build.VERSION.SDK_INT >= 33) {
                PermissionRow(ok = canNotify, text = "Notificaciones (aviso de que la puerta está activa)", button = "Permitir") {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            PermissionRow(ok = false, text = "Recomendado: quitar el ahorro de batería a esta app para que no la cierre", button = "Abrir ajustes", showStatus = false) {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        Section("6. Activar") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Poner la puerta al bloquear el teléfono", Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    enabled = rhythm != null && canOverlay,
                    onCheckedChange = { setEnabled(it) },
                )
            }
            if (rhythm == null || !canOverlay) {
                Text("Primero graba tu ritmo y da el permiso de \"mostrar sobre otras apps\".", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
            OutlinedButton(onClick = { LockActivity.test(context) }, enabled = rhythm != null) { Text("Probar la puerta ahora") }
        }

        Card(Modifier.fillMaxWidth()) {
            Text(
                "Importante: deja puesto tu PIN o huella del teléfono. La puerta aparece por encima del bloqueo de Android; " +
                    "después de abrirla, Android te pedirá tu PIN como siempre. Si olvidas el ritmo, toca dos veces " +
                    "\"Usar PIN del teléfono\" abajo en la puerta.",
                Modifier.padding(14.dp),
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (recording) {
        RecordRhythmDialog(
            matcher = settings.matcher(),
            doorStyle = door,
            onSaved = { r -> settings.rhythm = r; rhythm = r },
            onDismiss = { recording = false },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            content()
        }
    }
}

@Composable
private fun Choice(selected: Boolean, label: String, onClick: () -> Unit, preview: @Composable () -> Unit) {
    Column(
        Modifier
            .width(110.dp)
            .border(
                BorderStroke(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0x33FFFFFF)),
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        preview()
        Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun PermissionRow(ok: Boolean, text: String, button: String, showStatus: Boolean = true, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text((if (showStatus) (if (ok) "✅ " else "⚠️ ") else "") + text, Modifier.weight(1f), fontSize = 14.sp)
        if (!ok || !showStatus) {
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onClick) { Text(button, fontSize = 13.sp) }
        }
    }
}
