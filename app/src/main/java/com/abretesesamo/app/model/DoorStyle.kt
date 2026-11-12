package com.abretesesamo.app.model

enum class DoorStyle(val displayName: String) {
    MADERA_RUSTICA("Madera rústica"),
    METAL_INDUSTRIAL("Metal industrial"),
    MEDIEVAL("Medieval"),
    MODERNA("Moderna blanca");

    companion object {
        fun fromId(id: String?): DoorStyle = entries.firstOrNull { it.name == id } ?: MADERA_RUSTICA
    }
}
