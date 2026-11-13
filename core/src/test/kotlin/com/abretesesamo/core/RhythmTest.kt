package com.abretesesamo.core

import com.abretesesamo.core.rhythm.Rhythm
import com.abretesesamo.core.rhythm.RhythmEnroller
import com.abretesesamo.core.rhythm.RhythmMatcher
import com.abretesesamo.core.unlock.UnlockSession
import com.abretesesamo.core.unlock.UnlockSession.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RhythmTest {

    // "Toc, toc... toc-toc-toc": 5 toques
    private val secret = Rhythm(listOf(400, 800, 200, 200))
    private val matcher = RhythmMatcher(tolerance = 0.20)

    private fun stamps(start: Long, vararg intervals: Long): List<Long> =
        intervals.toList().runningFold(start) { acc, d -> acc + d }

    @Test
    fun `calcula los deltas desde marcas de tiempo`() {
        assertEquals(listOf(400L, 800L, 200L, 200L), Rhythm.fromTimestamps(stamps(1000, 400, 800, 200, 200)).intervalsMs)
    }

    @Test
    fun `acepta variaciones humanas dentro de la tolerancia`() {
        assertTrue(matcher.match(secret, Rhythm(listOf(450, 700, 240, 170))).matches)
    }

    @Test
    fun `rechaza fuera de tolerancia o con otro numero de toques`() {
        assertFalse(matcher.match(secret, Rhythm(listOf(400, 400, 200, 200))).matches)
        assertEquals(RhythmMatcher.Reason.WRONG_KNOCK_COUNT, matcher.match(secret, Rhythm(listOf(400, 800, 200))).reason)
    }

    @Test
    fun `margen minimo para intervalos muy cortos`() {
        // 20% de 200 = 40 ms, pero se garantizan 60 ms
        assertTrue(matcher.match(secret, Rhythm(listOf(400, 800, 255, 145))).matches)
    }

    @Test
    fun `la mitad del ritmo son los primeros toques`() {
        assertEquals(listOf(400L, 800L), secret.firstHalf().intervalsMs) // 3 de 5 toques
        assertEquals(listOf(300L), Rhythm(listOf(300, 300, 300)).firstHalf().intervalsMs) // 2 de 4
    }

    @Test
    fun `desbloquea con el ritmo correcto`() {
        val s = UnlockSession(secret, matcher)
        val ts = stamps(0, 410, 790, 210, 190)
        ts.forEach { assertIs<Event.TapRegistered>(s.onTap(it)) }
        // No abre al instante: espera una pausa por si el usuario sigue tocando.
        assertEquals(s.completeWaitMs, s.nextCheckDelayMs())
        assertEquals(Event.Unlocked, s.onIdle())
    }

    @Test
    fun `tocar de mas es fallo aunque los primeros toques sean correctos`() {
        val s = UnlockSession(secret, matcher)
        stamps(0, 410, 790, 210, 190).forEach { s.onTap(it) }
        assertIs<Event.Failed>(s.onTap(1_800))
    }

    @Test
    fun `un ritmo de tres toques no abre con tres toques cualquiera`() {
        val three = Rhythm(listOf(300, 900))
        val s = UnlockSession(three, matcher)
        stamps(0, 600, 600).forEach { s.onTap(it) }
        assertIs<Event.Failed>(s.onIdle())
        stamps(10_000, 300, 900).forEach { s.onTap(it) }
        assertEquals(Event.Unlocked, s.onIdle())
    }

    @Test
    fun `tres fallos seguidos dan la pista de media cancion`() {
        val s = UnlockSession(secret, matcher)
        repeat(3) { attempt ->
            stamps(attempt * 10_000L, 400, 400, 400, 400).forEach { s.onTap(it) }
            val failed = assertIs<Event.Failed>(s.onIdle())
            if (attempt < 2) assertNull(failed.hint) else assertEquals(secret.firstHalf(), failed.hint)
        }
    }

    @Test
    fun `tras la pista basta con completar el resto`() {
        val s = UnlockSession(secret, matcher)
        repeat(3) { a -> stamps(a * 10_000L, 400, 400, 400, 400).forEach { s.onTap(it) }; s.onIdle() }
        // La puerta toca los 3 primeros golpes (0, 400, 1200) y el usuario sigue con 2 más
        s.onHintPlayed(listOf(50_000L, 50_400L, 51_200L))
        assertIs<Event.TapRegistered>(s.onTap(51_420L))
        assertIs<Event.TapRegistered>(s.onTap(51_610L))
        assertEquals(s.completeWaitMs, s.nextCheckDelayMs())
        assertEquals(Event.Unlocked, s.onIdle())
    }

    @Test
    fun `tras la pista tambien vale tocar el ritmo entero`() {
        val s = UnlockSession(secret, matcher)
        repeat(3) { a -> stamps(a * 10_000L, 400, 400, 400, 400).forEach { s.onTap(it) }; s.onIdle() }
        s.onHintPlayed(listOf(50_000L, 50_400L, 51_200L))
        stamps(60_000L, 400, 800, 200, 200).forEach { s.onTap(it) }
        assertEquals(Event.Unlocked, s.onIdle())
    }

    @Test
    fun `un intento incompleto cuenta como fallo al dejar de tocar`() {
        val s = UnlockSession(secret, matcher)
        s.onTap(0); s.onTap(400)
        assertIs<Event.Failed>(s.onIdle())
        s.onTap(5_000)
        assertNull(s.onIdle()) // un toque suelto no cuenta
        assertEquals(1, s.consecutiveFailures)
    }

    @Test
    fun `registro en dos tomas promedia los intervalos`() {
        val e = RhythmEnroller(matcher)
        assertEquals(RhythmEnroller.Step.NeedConfirmation, e.submit(stamps(0, 400, 800, 200)))
        val done = assertIs<RhythmEnroller.Step.Done>(e.submit(stamps(0, 440, 760, 220)))
        assertEquals(listOf(420L, 780L, 210L), done.rhythm.intervalsMs)
    }

    @Test
    fun `serializa y deserializa`() {
        assertEquals(secret, Rhythm.deserialize(secret.serialize()))
        assertNull(Rhythm.deserialize("hola"))
    }
}
