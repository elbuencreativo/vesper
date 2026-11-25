package com.minimal.tvlauncher.ui

import androidx.compose.ui.focus.FocusRequester

/**
 * Recuerda qué elemento del inicio tenía el foco para devolvérselo al cerrar un menú modal
 * (mientras el menú está abierto, el fondo no puede tomar el foco y al reactivarse no queda ninguno).
 */
class FocusMemory {
    val requesters = mutableMapOf<String, FocusRequester>()
    var last: String? = null
        private set

    /** Mientras hay un menú abierto el foco se mueve solo; esos cambios no deben sobrescribir lo recordado. */
    var frozen = false

    fun remember(key: String) { if (!frozen) last = key }

    /** Intenta devolver el foco al último elemento; false si aún no está disponible. */
    fun restore(fallback: FocusRequester?): Boolean {
        val target = last?.let { requesters[it] } ?: fallback ?: return false
        return runCatching { target.requestFocus() }.isSuccess
    }
}
