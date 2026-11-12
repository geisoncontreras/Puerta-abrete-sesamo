package com.abretesesamo.core.rhythm

/**
 * Registro del ritmo secreto en dos pasos: el usuario lo toca una vez y luego lo repite.
 * Si ambas tomas coinciden (con la tolerancia actual), se guarda el promedio de las dos,
 * que representa mejor el ritmo "natural" del usuario que cualquiera de las tomas sola.
 */
class RhythmEnroller(private val matcher: RhythmMatcher) {

    sealed interface Step {
        /** Primera toma aceptada; hay que repetir el ritmo para confirmarlo. */
        data object NeedConfirmation : Step
        data class Done(val rhythm: Rhythm) : Step
        data class Rejected(val reason: Reason) : Step
    }

    enum class Reason { TOO_FEW_KNOCKS, TOO_MANY_KNOCKS, TAKES_DONT_MATCH }

    private var firstTake: Rhythm? = null

    val waitingForConfirmation: Boolean get() = firstTake != null

    fun submit(timestampsMs: List<Long>): Step {
        if (timestampsMs.size < Rhythm.MIN_KNOCKS) return Step.Rejected(Reason.TOO_FEW_KNOCKS)
        if (timestampsMs.size > Rhythm.MAX_KNOCKS) return Step.Rejected(Reason.TOO_MANY_KNOCKS)

        val take = Rhythm.fromTimestamps(timestampsMs)
        val first = firstTake
        if (first == null) {
            firstTake = take
            return Step.NeedConfirmation
        }
        firstTake = null
        if (!matcher.match(first, take).matches) return Step.Rejected(Reason.TAKES_DONT_MATCH)

        val averaged = first.intervalsMs.zip(take.intervalsMs) { a, b -> (a + b) / 2 }
        return Step.Done(Rhythm(averaged))
    }

    fun reset() {
        firstTake = null
    }
}
