package com.minimal.tvlauncher.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeKeyStatusTest {
    private val pkg = "com.minimal.tvlauncher"

    @Test
    fun isHomeKeyServiceEnabled_findsItInTheColonSeparatedList() {
        assertTrue(isHomeKeyServiceEnabled("$pkg/$pkg.actions.HomeKeyService", pkg))
        assertTrue(isHomeKeyServiceEnabled("dev.otro/dev.otro.Svc:$pkg/$pkg.actions.HomeKeyService", pkg))
        assertTrue(isHomeKeyServiceEnabled("$pkg/.actions.HomeKeyService", pkg)) // forma corta
    }

    @Test
    fun isHomeKeyServiceEnabled_isFalseWhenMissingOrNull() {
        assertFalse(isHomeKeyServiceEnabled(null, pkg))
        assertFalse(isHomeKeyServiceEnabled("", pkg))
        assertFalse(isHomeKeyServiceEnabled("dev.couchtv.ontv/dev.couchtv.ontv.WatchService", pkg))
        assertFalse(isHomeKeyServiceEnabled("$pkg/$pkg.actions.OtroServicio", pkg))
    }

    @Test
    fun accessibilityShortcut_opensAccessibilitySettings_withGeneralSettingsAsFallback() {
        val targets = targetsFor(Shortcut.Accessibility)
        assertEquals(Target.ByAction("android.settings.ACCESSIBILITY_SETTINGS"), targets.first())
        assertEquals(Target.ByAction("android.settings.SETTINGS"), targets.last())
    }
}
