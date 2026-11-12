package com.abretesesamo.app.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.abretesesamo.app.data.SettingsRepository
import com.abretesesamo.app.ui.theme.PuertaTheme

/** Pantalla principal (ajustes). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = SettingsRepository(this)
        setContent {
            PuertaTheme { SettingsScreen(settings) }
        }
    }
}
