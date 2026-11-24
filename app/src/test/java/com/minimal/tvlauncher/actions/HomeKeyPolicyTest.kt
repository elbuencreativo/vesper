package com.minimal.tvlauncher.actions

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeKeyPolicyTest {

    @Test
    fun home_isConsumedOnDownAndOpensTheLauncherOnUp() {
        assertEquals(HomeKeyDecision(consume = true, openLauncher = false), homeKeyDecision(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_DOWN))
        assertEquals(HomeKeyDecision(consume = true, openLauncher = true), homeKeyDecision(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_UP))
    }

    @Test
    fun everyOtherKey_isLeftAlone() {
        listOf(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MENU).forEach { key ->
            assertEquals(HomeKeyDecision(consume = false, openLauncher = false), homeKeyDecision(key, KeyEvent.ACTION_UP))
            assertEquals(HomeKeyDecision(consume = false, openLauncher = false), homeKeyDecision(key, KeyEvent.ACTION_DOWN))
        }
    }
}
