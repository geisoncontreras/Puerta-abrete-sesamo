package com.abretesesamo.app.model

/**
 * Manos originales (no son personajes con derechos de autor); cada una tiene su propia
 * forma de abrir la puerta, definida en ui/hand/HandMotion.kt.
 */
enum class HandStyle(val displayName: String, val description: String) {
    ESQUELETO("Esqueleto", "Huesos que tiemblan al empujar"),
    CARTOON_AMARILLA("Cartoon amarilla", "Rebota como un dibujo animado"),
    PULSERA_ANIME("Pulsera azul anime", "Aparece con un aura de energía"),
    MONSTRUO_VERDE("Monstruo verde", "Abre de un golpe y todo tiembla"),
    GUANTE_BLANCO("Guante blanco", "Empuja con elegancia y saluda");

    companion object {
        fun fromId(id: String?): HandStyle = entries.firstOrNull { it.name == id } ?: GUANTE_BLANCO
    }
}
