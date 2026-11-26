package com.minimal.tvlauncher.actions

import android.content.Context
import android.view.KeyEvent

/**
 * Botones de acceso directo del control (YouTube, Paramount, Disney+, la estrella…) reasignados a un app.
 * Cada botón se identifica por su código de tecla; si Android no le puso uno, por su código de hardware.
 * Se guarda en SharedPreferences (lectura inmediata, la usa el servicio de accesibilidad en cada tecla).
 */
object RemoteKeys {
    private const val FILE = "remote_keys"
    private const val ENABLED = "enabled"
    private const val MAP = "map"

    /** Si no es null, el servicio entrega aquí la próxima tecla "asignable" en vez de ejecutarla (modo aprender). */
    @Volatile
    var learning: ((String) -> Unit)? = null

    private val NEVER = setOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
    )

    fun keyId(keyCode: Int, scanCode: Int): String =
        if (keyCode != KeyEvent.KEYCODE_UNKNOWN) "k$keyCode" else "s$scanCode"

    /** Las teclas de navegación, atrás, inicio, encendido y volumen nunca se reasignan. */
    fun isAssignable(keyCode: Int): Boolean = keyCode !in NEVER

    fun label(id: String): String = when {
        id.startsWith("k") -> KeyEvent.keyCodeToString(id.drop(1).toIntOrNull() ?: 0).removePrefix("KEYCODE_").replace('_', ' ')
        else -> "Botón ${id.drop(1)}"
    }

    fun parse(raw: String?): Map<String, String> =
        raw.orEmpty().split(';').mapNotNull { e -> e.split('=', limit = 2).takeIf { it.size == 2 && it[0].isNotEmpty() && it[1].isNotEmpty() }?.let { it[0] to it[1] } }.toMap()

    fun serialize(map: Map<String, String>): String = map.entries.joinToString(";") { "${it.key}=${it.value}" }

    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(c: Context): Boolean = prefs(c).getBoolean(ENABLED, true)
    fun setEnabled(c: Context, on: Boolean) = prefs(c).edit().putBoolean(ENABLED, on).apply()
    fun load(c: Context): Map<String, String> = parse(prefs(c).getString(MAP, null))
    fun save(c: Context, map: Map<String, String>) = prefs(c).edit().putString(MAP, serialize(map)).apply()
}
