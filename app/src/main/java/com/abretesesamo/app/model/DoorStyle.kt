package com.abretesesamo.app.model

import androidx.annotation.DrawableRes
import com.abretesesamo.app.R

/**
 * Puertas. Las de foto ([photoRes]) son los diseños de Geison recortados borde a borde;
 * [interiorRes] es lo que se ve detrás al abrirse (oscuridad, cielo, fuego, galaxia…).
 * Las demás se dibujan por código.
 */
enum class DoorStyle(
    val displayName: String,
    @DrawableRes val photoRes: Int? = null,
    @DrawableRes val interiorRes: Int? = null,
) {
    /** La foto de puerta que mandó Geison, recortada borde a borde. */
    FOTO_MODELO("Tu modelo", R.drawable.puerta_modelo),
    HUESOS("Huesos", R.drawable.puerta_huesos, R.drawable.fondo_huesos),
    ANGELES_CIELO("Ángeles · cielo", R.drawable.puerta_angeles_cielo, R.drawable.fondo_angeles_cielo),
    ANGELES_NOCHE("Ángeles · noche", R.drawable.puerta_angeles_noche, R.drawable.fondo_angeles_noche),
    LAVA_FUEGO("Lava · fuego", R.drawable.puerta_lava_fuego, R.drawable.fondo_lava_fuego),
    LAVA_BRASAS("Lava · brasas", R.drawable.puerta_lava_brasas, R.drawable.fondo_lava_brasas),
    HIELO("Hielo", R.drawable.puerta_hielo, R.drawable.fondo_hielo),
    GALAXIA("Galaxia", R.drawable.puerta_galaxia, R.drawable.fondo_galaxia),
    COPOS("Copos cósmicos", R.drawable.puerta_copos, R.drawable.fondo_copos),
    ROJA_LUZ("Roja · luz", R.drawable.puerta_roja_luz, R.drawable.fondo_roja_luz),
    ROJA_OSCURA("Roja · oscuridad", R.drawable.puerta_roja_oscura, R.drawable.fondo_roja_oscura),
    METAL_LUZ("Metal · luz", R.drawable.puerta_metal_luz, R.drawable.fondo_metal_luz),
    METAL_OSCURO("Metal · oscuridad", R.drawable.puerta_metal_oscuro, R.drawable.fondo_metal_oscuro),
    BLANCA("Blanca", R.drawable.puerta_blanca, R.drawable.fondo_blanca),
    BLANCA_DORADA("Blanca dorada", R.drawable.puerta_blanca_dorada, R.drawable.fondo_blanca_dorada),
    MADERA_RUSTICA("Madera rústica"),
    METAL_INDUSTRIAL("Metal industrial"),
    MEDIEVAL("Medieval"),
    MODERNA("Moderna blanca");

    val isPhoto: Boolean get() = photoRes != null

    /** Las puertas de metal suenan a chapa de acero (toques y bisagra). */
    val isMetal: Boolean get() = this == METAL_LUZ || this == METAL_OSCURO || this == METAL_INDUSTRIAL

    /** Color del canto (grosor) de la hoja. */
    val edgeColor: Long get() = when (this) {
        HUESOS -> 0xFFCDBFA3
        ANGELES_CIELO, ANGELES_NOCHE -> 0xFFB8902F
        LAVA_FUEGO, LAVA_BRASAS -> 0xFF2B231F
        HIELO -> 0xFFCFE4F2
        GALAXIA, COPOS -> 0xFF4A4A57
        ROJA_LUZ -> 0xFF8C1414
        ROJA_OSCURA -> 0xFF6E1010
        METAL_LUZ, METAL_INDUSTRIAL -> 0xFF8D9196
        METAL_OSCURO -> 0xFF6F7378
        BLANCA, BLANCA_DORADA, MODERNA -> 0xFFE8E4DC
        else -> 0xFFB8935F
    }

    companion object {
        fun fromId(id: String?): DoorStyle = entries.firstOrNull { it.name == id } ?: FOTO_MODELO
    }
}
