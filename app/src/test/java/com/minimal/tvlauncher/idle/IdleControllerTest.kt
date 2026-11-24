package com.minimal.tvlauncher.idle

import com.minimal.tvlauncher.util.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IdleControllerTest {
    private val minute = 60_000L

    private fun TestScope.controller() =
        IdleController(backgroundScope, Clock { currentTime }, idleAfterMs = 5 * minute, darkAfterMs = 20 * minute)

    private suspend fun TestScope.pass(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun entersArtAfterFiveMinutes_andDarkAfterTwentyMinutesOfArt() = runTest {
        val c = controller()
        pass(4 * minute + 59_000)
        assertEquals(IdleState.Active, c.state.value)
        pass(2_000)
        assertEquals(IdleState.Art, c.state.value)
        pass(19 * minute)
        assertEquals(IdleState.Art, c.state.value)
        pass(2 * minute)
        assertEquals(IdleState.Dark, c.state.value)
    }

    @Test
    fun userInput_whileIdle_isSwallowedAndWakesUp_whileActiveIsPassedThrough() = runTest {
        val c = controller()
        assertFalse(c.onUserInput())
        pass(6 * minute)
        assertEquals(IdleState.Art, c.state.value)
        assertTrue(c.onUserInput())
        assertEquals(IdleState.Active, c.state.value)

        pass(30 * minute)
        assertEquals(IdleState.Dark, c.state.value)
        assertTrue(c.onUserInput())
        assertEquals(IdleState.Active, c.state.value)
        assertFalse(c.onUserInput())
    }

    @Test
    fun resuming_restartsTheTimer_andPausedLauncherNeverGoesIdle() = runTest {
        val c = controller()
        pass(4 * minute)
        c.onLauncherPaused()
        pass(30 * minute)
        assertEquals(IdleState.Active, c.state.value)
        c.onLauncherResumed()
        pass(4 * minute)
        assertEquals(IdleState.Active, c.state.value)
        pass(2 * minute)
        assertEquals(IdleState.Art, c.state.value)
    }
}
