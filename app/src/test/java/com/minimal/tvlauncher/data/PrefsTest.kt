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
    fun hidden_roundTrips_andDefaultsToEmpty() = runTest {
        val p = prefs()
        assertEquals(emptySet<String>(), p.hidden.first())
        p.setHidden(setOf("x.y"))
        assertEquals(setOf("x.y"), p.hidden.first())
    }
}
