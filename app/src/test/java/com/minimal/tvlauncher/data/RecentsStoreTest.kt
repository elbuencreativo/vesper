package com.minimal.tvlauncher.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecentsStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun store() =
        RecentsStore(Prefs(PreferenceDataStoreFactory.create { File(tmp.root, "r.preferences_pb") }))

    @Test
    fun record_movesToFront_noDuplicates_capsAtLimit() = runTest {
        val s = store()
        s.record("a")
        s.record("b")
        s.record("a")
        assertEquals(listOf("a", "b"), s.recent().first())

        (1..12).forEach { s.record("p$it") }
        val recent = s.recent(limit = 10).first()
        assertEquals(10, recent.size)
        assertEquals("p12", recent.first())
        assertEquals(recent.distinct(), recent)
    }
}
