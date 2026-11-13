package com.abretesesamo.app.model

enum class DoorStyle(val displayName: String) {
    /** La foto de puerta que mandó Geison, recortada borde a borde. */
    FOTO_MODELO("Tu modelo"),
    MADERA_RUSTICA("Madera rústica"),
    METAL_INDUSTRIAL("Metal industrial"),
    MEDIEVAL("Medieval"),
    MODERNA("Moderna blanca");

    companion object {
        fun fromId(id: String?): DoorStyle = entries.firstOrNull { it.name == id } ?: FOTO_MODELO
    }
}
