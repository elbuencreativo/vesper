package com.minimal.tvlauncher.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemShortcutsTest {

    @Test
    fun openFirst_fallsBackToGeneralSettings_whenSpecificMissing() {
        val tried = mutableListOf<Target>()
        val opened = openFirst(targetsFor(Shortcut.Wifi)) { t ->
            tried += t
            t == Target.ByAction("android.settings.SETTINGS")
        }
        assertEquals(Target.ByAction("android.settings.SETTINGS"), opened)
        assertEquals(Target.ByAction("android.settings.WIFI_SETTINGS"), tried.first())
    }

    @Test
    fun openFirst_returnsNull_whenNothingCanStart() {
        assertNull(openFirst(targetsFor(Shortcut.AndroidSettings)) { false })
    }

    @Test
    fun targetsFor_bluetoothTriesAccessoryScreenBeforeGeneralSettings() {
        val targets = targetsFor(Shortcut.Bluetooth)
        val accessory = Target.ByAction("com.google.android.intent.action.CONNECT_INPUT")
        assertEquals(Target.ByAction("android.settings.BLUETOOTH_SETTINGS"), targets.first())
        assertEquals(Target.ByAction("android.settings.SETTINGS"), targets.last())
        assertEquals(true, targets.indexOf(accessory) in 1 until targets.lastIndex)
    }
}
