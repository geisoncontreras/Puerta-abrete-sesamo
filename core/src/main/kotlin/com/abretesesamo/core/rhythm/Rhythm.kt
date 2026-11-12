package com.abretesesamo.core.rhythm

/**
 * Un ritmo secreto expresado como los intervalos (en milisegundos) entre toques consecutivos.
 *
 * N toques sobre la puerta producen N-1 intervalos. Guardamos solo los intervalos (no los
 * instantes absolutos) porque lo que identifica al ritmo es la separación entre golpes,
 * no el momento en que empezó.
 */
data class Rhythm(val intervalsMs: List<Long>) {

    init {
        require(intervalsMs.isNotEmpty()) { "Un ritmo necesita al menos 2 toques" }
        require(intervalsMs.all { it > 0 }) { "Los intervalos deben ser positivos" }
    }

    /** Número de toques que forman el ritmo. */
    val knockCount: Int get() = intervalsMs.size + 1

    /** Duración total desde el primer al último toque. */
    val totalDurationMs: Long get() = intervalsMs.sum()

    /** Intervalo más largo; sirve para calcular cuánto esperar antes de dar un intento por terminado. */
    val longestIntervalMs: Long get() = intervalsMs.max()

    /**
     * La primera mitad del ritmo: los primeros ceil(N/2) toques.
     * Es lo que la puerta "toca desde dentro" como pista tras 3 fallos.
     */
    fun firstHalf(): Rhythm {
        val knocks = ((knockCount + 1) / 2).coerceAtLeast(2)
        return Rhythm(intervalsMs.take(knocks - 1))
    }

    /** Momento de cada toque contado desde el primero: [0, d1, d1+d2, ...]. */
    fun knockOffsetsMs(): List<Long> = intervalsMs.runningFold(0L) { acc, d -> acc + d }

    fun serialize(): String = intervalsMs.joinToString(",")

    companion object {
        const val MIN_KNOCKS = 3
        const val MAX_KNOCKS = 16

        /** Convierte marcas de tiempo de toques (ms) en un ritmo calculando los deltas. */
        fun fromTimestamps(timestampsMs: List<Long>): Rhythm =
            Rhythm(timestampsMs.zipWithNext { a, b -> (b - a).coerceAtLeast(1L) })

        fun deserialize(raw: String?): Rhythm? {
            if (raw.isNullOrBlank()) return null
            val values = raw.split(",").map { it.trim().toLongOrNull() ?: return null }
            if (values.isEmpty() || values.any { it <= 0 }) return null
            return Rhythm(values)
        }
    }
}
