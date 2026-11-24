package com.minimal.tvlauncher.actions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.minimal.tvlauncher.data.AppEntry
import com.minimal.tvlauncher.data.Prefs
import com.minimal.tvlauncher.data.RecentsStore
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppActionsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private class FakeContext(private val missing: Boolean) : ContextWrapper(null) {
        val started = mutableListOf<Intent>()
        override fun getPackageName(): String = "com.minimal.tvlauncher"
        override fun startActivity(intent: Intent) {
            if (missing) throw ActivityNotFoundException("no hay actividad")
            started += intent
        }
    }

    private fun recents() =
        RecentsStore(Prefs(PreferenceDataStoreFactory.create { File(tmp.root, "a.preferences_pb") }))

    private fun entry(pkg: String) = AppEntry(pkg, pkg, Intent(pkg))

    @Test
    fun launch_returnsFalse_andSkipsRecents_whenActivityMissing() = runTest {
        val recents = recents()
        val actions = AppActions(FakeContext(missing = true) as Context, recents)

        assertFalse(actions.launch(entry("com.gone")))
        assertEquals(emptyList<String>(), recents.recent().first())
    }

    @Test
    fun close_opensSystemAppInfoScreen_forOtherPackages() = runTest {
        val ctx = FakeContext(missing = false)
        val actions = AppActions(ctx, recents())

        assertTrue(actions.close("com.netflix.ninja"))
        assertEquals(1, ctx.started.size)
        assertEquals(
            AppInfoSpec("android.settings.APPLICATION_DETAILS_SETTINGS", "package:com.netflix.ninja"),
            appInfoSpec("com.netflix.ninja"),
        )
    }

    @Test
    fun close_refusesTheLauncherItself_andReportsFailureWhenScreenMissing() = runTest {
        val ctx = FakeContext(missing = false)
        assertFalse(AppActions(ctx, recents()).close(ctx.packageName))
        assertEquals(0, ctx.started.size)

        assertFalse(AppActions(FakeContext(missing = true), recents()).close("com.netflix.ninja"))
    }

    @Test
    fun launch_startsActivity_andRecordsRecent_onSuccess() = runTest {
        val ctx = FakeContext(missing = false)
        val recents = recents()
        val actions = AppActions(ctx, recents)

        assertTrue(actions.launch(entry("com.netflix.ninja")))
        assertEquals(1, ctx.started.size)
        assertEquals(listOf("com.netflix.ninja"), recents.recent().first())
    }
}
