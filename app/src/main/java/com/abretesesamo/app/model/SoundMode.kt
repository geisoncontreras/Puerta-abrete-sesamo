package com.abretesesamo.app.model

/** Qué golpe suena al tocar la puerta (grabaciones reales de toques). */
enum class SoundMode(val displayName: String) {
    /** El primer toque tras una pausa suena fuerte (puño); los rápidos, con nudillos. */
    MEZCLA("Mezcla natural"),
    PUNO("Puño"),
    NUDILLO("Nudillos");

    companion object {
        fun fromId(id: String?): SoundMode = entries.firstOrNull { it.name == id } ?: MEZCLA
    }
}
