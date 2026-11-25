package com.minimal.tvlauncher.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PrefsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun prefs() = Prefs(PreferenceDataStoreFactory.create { File(tmp.root, "t.preferences_pb") })

    @Test
    fun order_roundTrips_andDefaultsToEmpty() = runTest {
        val p = prefs()
        assertEquals(emptyList<String>(), p.order.first())
        p.setOrder(listOf("a.b", "c.d"))
        assertEquals(listOf("a.b", "c.d"), p.order.first())
    }

    @Test
    fun dismissed_roundTrips_mergesKeys_andKeepsOnlyTheMostRecentOnes() = runTest {
        val p = prefs()
        assertEquals(emptyMap<String, Long>(), p.dismissed.first())
        p.dismiss("a|mobland", 10)
        p.dismiss("b|brooklyn\tnine-nine", 20) // tabuladores/saltos de línea en el título no rompen el guardado
        p.dismiss("a|mobland", 30) // actualiza la fecha de la misma clave
        val saved = p.dismissed.first()
        assertEquals(2, saved.size)
        assertEquals(30L, saved["a|mobland"])
        assertEquals(20L, saved.entries.first { it.key.startsWith("b|") }.value)

        repeat(120) { p.dismiss("k$it", 1000L + it) }
        val capped = p.dismissed.first()
        assertEquals(Prefs.MAX_DISMISSED, capped.size)
        assertEquals(true, "k119" in capped) // se conservan los más recientes
        assertEquals(false, "a|mobland" in capped)
    }

    @Test
    fun updateCheck_roundTrips_andDefaultsToNeverChecked() = runTest {
        val p = prefs()
        assertEquals(0L, p.lastUpdateCheckMs.first())
        assertEquals(null, p.knownUpdate.first())
        p.saveUpdateCheck(5_000, KnownUpdate(3, "0.3.0"))
        assertEquals(5_000L, p.lastUpdateCheckMs.first())
        assertEquals(KnownUpdate(3, "0.3.0"), p.knownUpdate.first())
        assertEquals(0, p.announcedUpdateCode.first())
        p.setAnnouncedUpdateCode(3)
        assertEquals(3, p.announcedUpdateCode.first())
        p.saveUpdateCheck(9_000, null) // ya estás al día: se borra el aviso
        assertEquals(9_000L, p.lastUpdateCheckMs.first())
        assertEquals(null, p.knownUpdate.first())
    }

    @Test
    fun hidden_roundTrips_andDefaultsToEmpty() = runTest {
        val p = prefs()
        assertEquals(emptySet<String>(), p.hidden.first())
        p.setHidden(setOf("x.y"))
        assertEquals(setOf("x.y"), p.hidden.first())
    }
}
