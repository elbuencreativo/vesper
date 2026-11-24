package com.minimal.tvlauncher.ui

/**
 * Sigue una pulsación de OK sobre UN elemento: clic al soltar, o pulsación larga si se mantiene.
 * Solo cuenta lo que este elemento vio empezar: si OK ya estaba pulsada cuando el elemento recibió el foco
 * (por ejemplo, el primer botón de un menú que se abrió al mantener OK), soltarla no hace nada.
 */
class OkPressTracker {
    private var pressed = false
    private var longFired = false

    /** Devuelve true si hay que disparar la pulsación larga ahora. */
    fun onDown(repeatCount: Int, isLongPress: Boolean, hasLongHandler: Boolean): Boolean {
        if (repeatCount == 0) {
            pressed = true
            longFired = false
            return false
        }
        if (pressed && !longFired && hasLongHandler && isLongPress) {
            longFired = true
            return true
        }
        return false
    }

    /** Devuelve true si hay que disparar el clic ahora. */
    fun onUp(): Boolean {
        val click = pressed && !longFired
        pressed = false
        longFired = false
        return click
    }
}
