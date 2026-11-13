package com.abretesesamo.app.model

import androidx.annotation.DrawableRes
import com.abretesesamo.app.R

/**
 * Manos. Las de foto ([spriteRes]) están recortadas de las imágenes de Geison:
 * [srcW]/[srcH] es su tamaño en esas imágenes (donde la puerta medía ~300 px de ancho)
 * y [grip] la altura del agarre dentro del recorte (0 arriba, 1 abajo).
 */
enum class HandStyle(
    val displayName: String,
    val description: String,
    @DrawableRes val spriteRes: Int? = null,
    val srcW: Float = 0f,
    val srcH: Float = 0f,
    val grip: Float = 0.5f,
) {
    BRAZO_REAL("Brazo real", "El brazo de tu modelo sale de la oscuridad y empuja la puerta"),
    GUANTE_NEGRO("Guante negro", "Un guante de cuero negro agarra el pomo", R.drawable.mano_guante_negro, 148f, 125f, 0.7f),
    ESQUELETO_REAL("Esqueleto real", "Un brazo de huesos sale de la oscuridad", R.drawable.mano_esqueleto, 154f, 130f, 0.658f),
    PLUMAS("Guante de plumas", "Elegante, con plumas en la muñeca", R.drawable.mano_plumas, 152f, 130f, 0.696f),
    GARRA("Garra de demonio", "Uñas largas y piel roja", R.drawable.mano_garra, 148f, 140f, 0.639f),
    DIVINA("Mano divina", "Con túnica bordada", R.drawable.mano_divina, 108f, 230f, 0.374f),
    ROBOT("Robot", "Brazo mecánico de metal", R.drawable.mano_robot, 159f, 135f, 0.633f),
    FUEGO("Fuego", "Un brazo en llamas", R.drawable.mano_fuego, 152f, 275f, 0.524f),
    HIELO("Hielo", "Un brazo de cristal helado", R.drawable.mano_hielo, 154f, 125f, 0.588f),
    MORADA("Morada escamosa", "Un brazo morado con escamas", R.drawable.mano_morada, 146f, 179f, 0.685f),
    UNAS_ROSAS("Uñas rosadas", "Una mano con las uñas pintadas", R.drawable.mano_unas_rosas, 141f, 179f, 0.682f),
    PULSERA("Con pulsera", "Una mano con pulsera dorada", R.drawable.mano_pulsera, 118f, 160f, 0.806f),
    INFANTIL("Mano pequeña", "Una mano pequeña y suave", R.drawable.mano_infantil, 106f, 155f, 0.761f),
    GARRA_VERDE("Garra verde", "Una garra de monstruo verde con anillo", R.drawable.mano_garra_verde, 214f, 152f, 0.419f);

    /** Brazos de foto: salen de la oscuridad, empujan, sueltan y la puerta sigue sola. */
    val isPhoto: Boolean get() = this == BRAZO_REAL || spriteRes != null

    companion object {
        /** Las manos dibujadas que se quitaron (Esqueleto, Cartoon...) vuelven al brazo real. */
        fun fromId(id: String?): HandStyle = entries.firstOrNull { it.name == id } ?: BRAZO_REAL
    }
}
