package com.minimal.tvlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OkPressTrackerTest {

    @Test
    fun normalPress_clicksOnRelease() {
        val t = OkPressTracker()
        assertFalse(t.onDown(repeatCount = 0, isLongPress = false, hasLongHandler = true))
        assertTrue(t.onUp())
    }

    @Test
    fun releaseWithoutAPressOnThisTile_isIgnored_soTheMenuDoesNotTriggerItself() {
        // Mantienes OK en un app -> se abre el menú -> sueltas: ese "soltar" llega a "Mover" y no debe activarlo.
        val menuItem = OkPressTracker()
        assertFalse(menuItem.onUp())
        // Las repeticiones de la tecla aún pulsada tampoco cuentan en un botón que no vio el primer "pulsar".
        assertFalse(menuItem.onDown(repeatCount = 3, isLongPress = true, hasLongHandler = true))
        assertFalse(menuItem.onUp())
    }

    @Test
    fun longPress_firesOnce_andTheReleaseDoesNotClick() {
        val t = OkPressTracker()
        t.onDown(0, false, true)
        assertTrue(t.onDown(repeatCount = 1, isLongPress = true, hasLongHandler = true))
        assertFalse(t.onDown(repeatCount = 2, isLongPress = true, hasLongHandler = true))
        assertFalse(t.onUp())
        // la siguiente pulsación normal vuelve a funcionar
        t.onDown(0, false, true)
        assertTrue(t.onUp())
    }

    @Test
    fun tileWithoutLongHandler_stillClicksAfterAHold() {
        val t = OkPressTracker()
        t.onDown(0, false, hasLongHandler = false)
        assertFalse(t.onDown(repeatCount = 2, isLongPress = true, hasLongHandler = false))
        assertEquals(true, t.onUp())
    }
}
