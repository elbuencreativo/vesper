package com.minimal.tvlauncher.actions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log

sealed interface Target {
    val action: String

    data class ByAction(override val action: String) : Target
}

enum class Shortcut { Wifi, Bluetooth, AndroidSettings, ChooseHome, Accessibility }

private val GENERAL_SETTINGS = Target.ByAction("android.settings.SETTINGS")

/**
 * Candidatos en orden: pantalla específica primero, ajustes generales al final.
 * En Google TV (Onn 4K) no existe android.settings.BLUETOOTH_SETTINGS. La pantalla de accesorios
 * Bluetooth (donde se vinculan audífonos) no es exportada por componente, pero responde a la acción
 * com.google.android.intent.action.CONNECT_INPUT (verificado en el dispositivo).
 */
fun targetsFor(shortcut: Shortcut): List<Target> = when (shortcut) {
    Shortcut.Wifi -> listOf(Target.ByAction("android.settings.WIFI_SETTINGS"), GENERAL_SETTINGS)
    Shortcut.Bluetooth -> listOf(
        Target.ByAction("android.settings.BLUETOOTH_SETTINGS"),
        Target.ByAction("com.google.android.intent.action.CONNECT_INPUT"),
        GENERAL_SETTINGS,
    )
    Shortcut.AndroidSettings -> listOf(GENERAL_SETTINGS)
    Shortcut.Accessibility -> listOf(Target.ByAction("android.settings.ACCESSIBILITY_SETTINGS"), GENERAL_SETTINGS)
    // "App de inicio predeterminada": lista real de launchers (verificado en el Onn).
    Shortcut.ChooseHome -> listOf(Target.ByAction("android.settings.HOME_SETTINGS"), GENERAL_SETTINGS)
}

/** Abre el primer candidato que se pueda iniciar. Devuelve cuál abrió, o null si ninguno. */
fun openFirst(targets: List<Target>, start: (Target) -> Boolean): Target? = targets.firstOrNull(start)

private const val TAG = "SystemShortcuts"

class SystemShortcuts(private val context: Context) {

    fun open(shortcut: Shortcut): Boolean = openFirst(targetsFor(shortcut), ::tryStart) != null

    private fun tryStart(target: Target): Boolean {
        val intent = Intent(target.action)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No existe ${target.action}")
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Sin permiso para ${target.action}: ${e.message}")
            false
        }
    }
}
