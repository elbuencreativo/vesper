package com.minimal.tvlauncher.actions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRedirectTest {
    private val stock = "com.google.android.apps.tv.launcherx"

    @Test
    fun redirects_whenTheStockHomeWindowAppears() {
        assertTrue(shouldRedirectToLauncher(stock, "com.google.android.apps.tv.launcherx.home.HomeActivity", now = 100, suppressUntil = 0))
        assertTrue(shouldRedirectToLauncher(stock, "com.google.android.apps.tv.launcherx.home.VanillaModeHomeActivity", now = 100, suppressUntil = 0))
    }

    @Test
    fun ignoresOtherWindows_includingOtherWindowsOfTheStockLauncher() {
        assertFalse(shouldRedirectToLauncher("com.netflix.ninja", "com.netflix.ninja.MainActivity", now = 100, suppressUntil = 0))
        assertFalse(shouldRedirectToLauncher(stock, "com.google.android.apps.tv.launcherx.search.SearchActivity", now = 100, suppressUntil = 0))
        assertFalse(shouldRedirectToLauncher(null, null, now = 100, suppressUntil = 0))
        assertFalse(shouldRedirectToLauncher("com.minimal.tvlauncher", "com.minimal.tvlauncher.HomeActivity", now = 100, suppressUntil = 0))
    }

    @Test
    fun doesNotRedirect_whileSuppressed_soTheCrashGuardCannotLoop() {
        val home = "com.google.android.apps.tv.launcherx.home.HomeActivity"
        assertFalse(shouldRedirectToLauncher(stock, home, now = 100, suppressUntil = 500))
        assertTrue(shouldRedirectToLauncher(stock, home, now = 500, suppressUntil = 500))
    }
}
