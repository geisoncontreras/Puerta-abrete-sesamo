package com.abretesesamo.app.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.abretesesamo.app.PuertaApp
import com.abretesesamo.app.data.SettingsRepository
import com.abretesesamo.app.ui.theme.PuertaTheme

/** Pantalla principal (ajustes). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = SettingsRepository(this)
        // Si la última vez la app se cerró por un error, lo mostramos una vez.
        val crashFile = PuertaApp.errorFile(this)
        val lastCrash = crashFile.takeIf { it.exists() }?.readText()?.also { crashFile.delete() }
        setContent {
            PuertaTheme {
                SettingsScreen(settings)
                var crash by remember { mutableStateOf(lastCrash) }
                crash?.let { text ->
                    AlertDialog(
                        onDismissRequest = { crash = null },
                        confirmButton = { TextButton(onClick = { crash = null }) { Text("Entendido") } },
                        title = { Text("La app se cerró la última vez") },
                        text = {
                            Text(
                                "Haz una captura de pantalla de esto y mándasela a Claude:\n\n$text",
                                fontSize = 11.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                            )
                        },
                    )
                }
            }
        }
    }
}
