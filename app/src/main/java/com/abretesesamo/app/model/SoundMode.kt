package com.abretesesamo.app.model

/**
 * Qué golpe suena al tocar la puerta.
 * Los tres primeros son las grabaciones reales; los demás son tipos de puerta nuevos
 * ([bank] = prefijo de sus sonidos en res/raw: `<bank>_f_N` fuerte y `<bank>_s_N` suave).
 * Todos mezclan fuerte (tras una pausa) y suave (toques rápidos), menos Puño y Nudillos.
 */
enum class SoundMode(val displayName: String, val bank: String? = null) {
    /** El primer toque tras una pausa suena fuerte (puño); los rápidos, con nudillos. */
    MEZCLA("Mezcla natural"),
    PUNO("Puño"),
    NUDILLO("Nudillos"),
    PESADA("Puerta pesada", "pesada"),
    HUECA("Madera hueca", "hueca"),
    CASTILLO("Eco de castillo", "castillo"),
    VIDRIO("Vidrio", "vidrio"),
    FANTASMA("Fantasma", "fantasma");

    companion object {
        fun fromId(id: String?): SoundMode = entries.firstOrNull { it.name == id } ?: MEZCLA
    }
}
