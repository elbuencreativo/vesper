package com.minimal.tvlauncher.actions

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.minimal.tvlauncher.HomeActivity

data class HomeKeyDecision(val consume: Boolean, val openLauncher: Boolean)

/** HOME se consume al pulsar y abre el launcher al soltar; el resto de teclas no se tocan. */
fun homeKeyDecision(keyCode: Int, action: Int): HomeKeyDecision = when {
    keyCode != KeyEvent.KEYCODE_HOME -> HomeKeyDecision(consume = false, openLauncher = false)
    action == KeyEvent.ACTION_UP -> HomeKeyDecision(consume = true, openLauncher = true)
    else -> HomeKeyDecision(consume = true, openLauncher = false)
}

/**
 * Captura la tecla HOME antes de que llegue al home de Google TV (que tiene prioridad sobre
 * cualquier launcher de terceros) y abre TV Launcher. Solo filtra teclas: no lee la pantalla.
 * El usuario debe activarlo en Accesibilidad.
 */
class HomeKeyService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val decision = homeKeyDecision(event.keyCode, event.action)
        if (decision.openLauncher) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }
        return decision.consume
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
