package com.minimal.tvlauncher

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun appId_isLauncherPackage() {
        assertEquals("com.minimal.tvlauncher", BuildConfig.APPLICATION_ID)
    }
}
