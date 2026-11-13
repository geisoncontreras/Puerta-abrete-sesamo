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

    companion object {
        fun fromId(id: String?): DoorStyle = entries.firstOrNull { it.name == id } ?: FOTO_MODELO
    }
}
