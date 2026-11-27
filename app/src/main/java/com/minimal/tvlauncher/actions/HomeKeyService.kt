package com.minimal.tvlauncher.actions

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.minimal.tvlauncher.HomeActivity

const val STOCK_HOME_PACKAGE = "com.google.android.apps.tv.launcherx"

data class HomeKeyDecision(val consume: Boolean, val openLauncher: Boolean)

/**
 * HOME abre el launcher al soltar, pero NO se consume: el sistema debe verla también. Es el sistema quien, al pulsar HOME,
 * avisa por HDMI-CEC al televisor ("One Touch Play": Active Source) para que cambie a la entrada del Onn.
 * Si se consumiera, el televisor ya no cambiaría de entrada. El resto de teclas no se tocan.
 */
fun homeKeyDecision(keyCode: Int, action: Int): HomeKeyDecision = when {
    keyCode != KeyEvent.KEYCODE_HOME -> HomeKeyDecision(consume = false, openLauncher = false)
    action == KeyEvent.ACTION_UP -> HomeKeyDecision(consume = false, openLauncher = true)
    else -> HomeKeyDecision(consume = false, openLauncher = false)
}

/**
 * ¿Está activo el servicio? [enabledServices] es el valor de Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
 * (componentes separados por ':'). Android lo apaga si se fuerza el cierre del app; por eso se muestra su estado.
 */
fun isHomeKeyServiceEnabled(enabledServices: String?, packageName: String): Boolean {
    if (enabledServices.isNullOrBlank()) return false
    val full = "$packageName/$packageName.actions.HomeKeyService"
    val short = "$packageName/.actions.HomeKeyService"
    return enabledServices.split(':').any { it.equals(full, ignoreCase = true) || it.equals(short, ignoreCase = true) }
}

/**
 * En Google TV la tecla HOME puede no pasar por el filtro de teclas: el sistema abre el home de Google.
 * Entonces se detecta que esa pantalla apareció y se redirige a Vesper. [suppressUntil] pausa la
 * redirección (el salvavidas de arranque cede al home de Google y no debe rebotar).
 */
fun shouldRedirectToLauncher(packageName: String?, className: String?, now: Long, suppressUntil: Long): Boolean =
    now >= suppressUntil &&
        packageName == STOCK_HOME_PACKAGE &&
        className?.let { it.endsWith(".home.HomeActivity") || it.endsWith(".home.VanillaModeHomeActivity") } == true

/**
 * Hace que HOME abra Vesper aunque el home de Google tenga prioridad. Solo mira teclas y qué
 * pantalla aparece (paquete y clase); no lee el contenido. El usuario debe activarlo en Accesibilidad.
 */
class HomeKeyService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val decision = homeKeyDecision(event.keyCode, event.action)
        if (decision.openLauncher) openLauncher()
        if (decision.consume) return true
        return remoteKey(event)
    }

    /** Botones de acceso directo del control: en modo "aprender" se entregan al ajuste; si no, abren el app asignado. */
    private fun remoteKey(event: KeyEvent): Boolean {
        if (!RemoteKeys.isAssignable(event.keyCode)) return false
        val id = RemoteKeys.keyId(event.keyCode, event.scanCode)
        // Un botón recién aprendido: el resto de su pulsación (repeticiones y soltada) también se traga,
        // si no el sistema abriría su app de fábrica al soltarlo.
        if (id in swallowed) {
            if (event.action == KeyEvent.ACTION_UP) swallowed.remove(id)
            return true
        }
        RemoteKeys.learning?.let { learn ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                swallowed.add(id)
                if (event.repeatCount == 0) learn(id)
            }
            return true
        }
        if (!RemoteKeys.isEnabled(this)) return false
        val pkg = RemoteKeys.load(this)[id] ?: return false
        val intent = packageManager.getLeanbackLaunchIntentForPackage(pkg) ?: packageManager.getLaunchIntentForPackage(pkg) ?: return false
        if (event.action == KeyEvent.ACTION_UP) startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (shouldRedirectToLauncher(event.packageName?.toString(), event.className?.toString(), SystemClock.elapsedRealtime(), suppressUntil)) {
            openLauncher()
        }
    }

    private val swallowed = HashSet<String>()

    override fun onInterrupt() = Unit

    private fun openLauncher() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    companion object {
        @Volatile
        private var suppressUntil = 0L

        /** Pausa la redirección unos milisegundos (misma app/proceso que HomeActivity). */
        fun suppressRedirectFor(ms: Long) {
            suppressUntil = SystemClock.elapsedRealtime() + ms
        }
    }
}
