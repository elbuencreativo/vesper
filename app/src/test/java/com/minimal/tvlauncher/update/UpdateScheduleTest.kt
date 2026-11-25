package com.minimal.tvlauncher.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateScheduleTest {
    private val hour = 3_600_000L

    @Test
    fun shouldCheckForUpdate_whenNeverCheckedOrTheIntervalHasPassed() {
        assertTrue(shouldCheckForUpdate(lastCheckMs = 0, nowMs = 1_000_000))
        assertTrue(shouldCheckForUpdate(lastCheckMs = 100 * hour, nowMs = 112 * hour))
        assertFalse(shouldCheckForUpdate(lastCheckMs = 100 * hour, nowMs = 111 * hour))
    }

    @Test
    fun shouldCheckForUpdate_treatsAClockThatWentBackwardsAsStale() {
        assertTrue(shouldCheckForUpdate(lastCheckMs = 500 * hour, nowMs = 100 * hour))
    }

    @Test
    fun defaultInterval_isTwelveHours() {
        assertEquals(12 * hour, UPDATE_INTERVAL_MS)
    }

    @Test
    fun shouldAnnounce_onlyNewerVersionsNotAnnouncedBefore() {
        assertTrue(shouldAnnounce(availableCode = 3, currentCode = 2, announcedCode = 0))
        assertFalse(shouldAnnounce(availableCode = 3, currentCode = 2, announcedCode = 3))
        assertTrue(shouldAnnounce(availableCode = 4, currentCode = 2, announcedCode = 3))
        assertFalse(shouldAnnounce(availableCode = 2, currentCode = 2, announcedCode = 0))
    }
}
