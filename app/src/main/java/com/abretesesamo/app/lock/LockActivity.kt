package com.abretesesamo.app.lock

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.abretesesamo.app.audio.KnockPlayer
import com.abretesesamo.app.data.SettingsRepository
import com.abretesesamo.app.ui.door.DoorLockScreen
import com.abretesesamo.app.ui.theme.PuertaTheme

/**
 * La pantalla de la puerta. Se dibuja ENCIMA de la pantalla de bloqueo del sistema
 * (setShowWhenLocked) sin desactivarla: si el teléfono tiene PIN/huella, después de abrir
 * la puerta Android lo sigue pidiendo. Así la puerta es una capa divertida extra y la
 * seguridad real la mantiene el sistema.
 */
class LockActivity : ComponentActivity() {

    private val keyguard by lazy { getSystemService(KeyguardManager::class.java) }
    private lateinit var viewModel: LockViewModel
    private var testMode = false

    /** Ajustes con los que se armó esta pantalla; si cambian, se rehace con los nuevos. */
    private var loadedSettings: String? = null

    private fun settingsSignature(s: SettingsRepository) =
        listOf(s.rhythm?.serialize(), s.doorStyle.name, s.handStyle.name, s.soundMode.name, s.matcher().tolerance).joinToString("|")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testMode = intent.getBooleanExtra(EXTRA_TEST, false)
        showOverLockScreen()
        hideSystemBars()

        val settings = SettingsRepository(this)
        val rhythm = settings.rhythm
        if (rhythm == null) {
            finish()
            return
        }
        loadedSettings = settingsSignature(settings)
        viewModel = ViewModelProvider(this, LockViewModel.factory(rhythm, settings.matcher(), { KnockPlayer(applicationContext, settings.soundMode).apply { metal = settings.doorStyle.isMetal } }))[LockViewModel::class.java]

        // El botón "atrás" no abre la puerta (en modo prueba sí sale).
        onBackPressedDispatcher.addCallback(this) { if (testMode) finish() }

        val doorStyle = settings.doorStyle
        val handStyle = settings.handStyle
        setContent {
            PuertaTheme {
                DoorLockScreen(
                    viewModel = viewModel,
                    doorStyle = doorStyle,
                    handStyle = handStyle,
                    showSystemUnlock = testMode || keyguard.isKeyguardSecure,
                    onDoorOpened = ::passThroughToSystem,
                    onUseSystemUnlock = ::passThroughToSystem,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        testMode = intent.getBooleanExtra(EXTRA_TEST, false)
        // Si se grabó un ritmo nuevo (o cambió la puerta/sonido) desde la última vez,
        // la pantalla vieja seguiría usando el ritmo antiguo: se rehace desde cero.
        if (settingsSignature(SettingsRepository(this)) != loadedSettings) {
            viewModelStore.clear()
            recreate()
            return
        }
        if (::viewModel.isInitialized) viewModel.reset()
    }

    /** Tras abrir la puerta, deja paso al bloqueo del sistema (PIN/huella si lo hay). */
    private fun passThroughToSystem() {
        if (!testMode && keyguard.isKeyguardLocked) {
            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = finish()
                override fun onDismissCancelled() = viewModel.reset()
                override fun onDismissError() = finish()
            })
        } else {
            finish()
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(false)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        private const val EXTRA_TEST = "modo_prueba"

        /** Llamado por LockService al apagarse la pantalla. */
        fun show(context: Context) {
            val settings = SettingsRepository(context)
            if (!settings.lockEnabled || settings.rhythm == null) return
            // Sin el permiso "mostrar sobre otras apps", Android 10+ bloquea abrir pantallas desde segundo plano.
            if (!Settings.canDrawOverlays(context)) return
            context.startActivity(intent(context, test = false))
        }

        /** Botón "Probar la puerta" de los ajustes. */
        fun test(context: Context) = context.startActivity(intent(context, test = true))

        private fun intent(context: Context, test: Boolean) =
            Intent(context, LockActivity::class.java)
                .putExtra(EXTRA_TEST, test)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}
