package com.abretesesamo.app.data

import android.content.Context
import androidx.core.content.edit
import com.abretesesamo.app.model.DoorStyle
import com.abretesesamo.app.model.HandStyle
import com.abretesesamo.app.model.SoundMode
import com.abretesesamo.core.rhythm.Rhythm
import com.abretesesamo.core.rhythm.RhythmMatcher

/**
 * Ajustes guardados en el almacenamiento privado de la app (otras apps no pueden leerlo,
 * y allowBackup=false evita que el ritmo viaje en copias de seguridad).
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("puerta_ajustes", Context.MODE_PRIVATE)

    var rhythm: Rhythm?
        get() = Rhythm.deserialize(prefs.getString(KEY_RHYTHM, null))
        set(value) = prefs.edit {
            if (value == null) remove(KEY_RHYTHM) else putString(KEY_RHYTHM, value.serialize())
        }

    /** Tolerancia en porcentaje (10..40). 20 = ±20 %. */
    var tolerancePercent: Int
        get() = prefs.getInt(KEY_TOLERANCE, 20)
        set(value) = prefs.edit { putInt(KEY_TOLERANCE, value.coerceIn(10, 40)) }

    var doorStyle: DoorStyle
        get() = DoorStyle.fromId(prefs.getString(KEY_DOOR, null))
        set(value) = prefs.edit { putString(KEY_DOOR, value.name) }

    var handStyle: HandStyle
        get() = HandStyle.fromId(prefs.getString(KEY_HAND, null))
        set(value) = prefs.edit { putString(KEY_HAND, value.name) }

    var soundMode: SoundMode
        get() = SoundMode.fromId(prefs.getString(KEY_SOUND, null))
        set(value) = prefs.edit { putString(KEY_SOUND, value.name) }

    var lockEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    fun matcher(): RhythmMatcher = RhythmMatcher(tolerance = tolerancePercent / 100.0)

    private companion object {
        const val KEY_RHYTHM = "ritmo"
        const val KEY_TOLERANCE = "tolerancia"
        const val KEY_DOOR = "puerta"
        const val KEY_HAND = "mano"
        const val KEY_ENABLED = "activada"
        const val KEY_SOUND = "sonido"
    }
}
