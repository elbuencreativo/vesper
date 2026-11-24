package com.minimal.tvlauncher.util

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.minimal.tvlauncher.data.Prefs
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashGuardTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private var now = 0L
    private val clock = Clock { now }

    private fun prefs() = Prefs(PreferenceDataStoreFactory.create { File(tmp.root, "c.preferences_pb") })

    private suspend fun CrashGuard.startAt(ms: Long) {
        now = ms
        onStart()
    }

    @Test
    fun shouldYieldToStock_afterThreeCrashesInTwoMinutes_andNotWhenSpreadOut() = runTest {
        val guard = CrashGuard(prefs(), clock)
        guard.startAt(0)
        guard.startAt(30_000)
        assertFalse(guard.shouldYieldToStock())
        guard.startAt(60_000)
        assertTrue(guard.shouldYieldToStock())

        val spread = CrashGuard(Prefs(PreferenceDataStoreFactory.create { File(tmp.root, "spread.preferences_pb") }), clock)
        spread.startAt(0)
        spread.startAt(70_000)
        spread.startAt(150_000)
        assertFalse(spread.shouldYieldToStock())
    }

    @Test
    fun markHealthy_clearsTheCountdown() = runTest {
        val guard = CrashGuard(prefs(), clock)
        guard.startAt(0)
        guard.startAt(10_000)
        guard.startAt(20_000)
        assertTrue(guard.shouldYieldToStock())
        guard.markHealthy()
        assertFalse(guard.shouldYieldToStock())
    }

    @Test
    fun startTimesFromBeforeAReboot_areIgnored() = runTest {
        val p = prefs()
        p.setStartTimes(listOf(9_000_000, 9_000_100, 9_000_200)) // el reloj monotónico volvió a empezar
        now = 5_000
        assertFalse(CrashGuard(p, clock).shouldYieldToStock())
    }
}
