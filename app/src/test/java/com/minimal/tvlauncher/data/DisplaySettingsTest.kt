package com.minimal.tvlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplaySettingsTest {
    @Test fun autoIsLightFromSixToSix() {
        assertTrue(ThemeLogic.isDark(ThemeMode.Auto, 5))
        assertFalse(ThemeLogic.isDark(ThemeMode.Auto, 6))
        assertFalse(ThemeLogic.isDark(ThemeMode.Auto, 17))
        assertTrue(ThemeLogic.isDark(ThemeMode.Auto, 18))
        assertTrue(ThemeLogic.isDark(ThemeMode.Auto, 0))
    }

    @Test fun fixedModesIgnoreHour() {
        assertFalse(ThemeLogic.isDark(ThemeMode.Light, 23))
        assertTrue(ThemeLogic.isDark(ThemeMode.Dark, 12))
    }

    @Test fun greetingNameFallbacks() {
        val s = DisplaySettings(greetingName = "  ", useProfileName = false)
        assertEquals("familia", greetingNameFor(s, "Leo"))
        assertEquals("Leo", greetingNameFor(s.copy(useProfileName = true), "Leo"))
        assertEquals("Los Gómez", greetingNameFor(s.copy(greetingName = "Los Gómez", useProfileName = true), " "))
        assertEquals("Los Gómez", greetingNameFor(s.copy(greetingName = "Los Gómez"), "Leo"))
    }

    @Test fun defaultCitiesExist() {
        assertEquals("America/Bogota", Cities.byName("Medellín")?.zoneId)
        assertEquals("Europe/Zurich", Cities.byName("Zúrich")?.zoneId)
    }
}

class StoicQuotesTest {
    @Test fun hasFiftyAndAvoidsRepeat() {
        assertEquals(50, StoicQuotes.all.size)
        val first = StoicQuotes.all[0]
        repeat(200) { assertTrue(StoicQuotes.random(first) != first) }
    }
}
