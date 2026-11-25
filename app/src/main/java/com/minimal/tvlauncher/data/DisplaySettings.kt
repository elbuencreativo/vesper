package com.minimal.tvlauncher.data

import java.time.ZoneId
import java.time.ZonedDateTime

enum class ThemeMode { Auto, Light, Dark }

/** Ciudad para los relojes: nombre, país y zona horaria. */
data class City(val name: String, val country: String, val zoneId: String)

object Cities {
    val all = listOf(
        City("Medellín", "Colombia", "America/Bogota"),
        City("Bogotá", "Colombia", "America/Bogota"),
        City("Zúrich", "Suiza", "Europe/Zurich"),
        City("Ginebra", "Suiza", "Europe/Zurich"),
        City("Madrid", "España", "Europe/Madrid"),
        City("Barcelona", "España", "Europe/Madrid"),
        City("Londres", "Reino Unido", "Europe/London"),
        City("París", "Francia", "Europe/Paris"),
        City("Berlín", "Alemania", "Europe/Berlin"),
        City("Roma", "Italia", "Europe/Rome"),
        City("Lisboa", "Portugal", "Europe/Lisbon"),
        City("Ámsterdam", "Países Bajos", "Europe/Amsterdam"),
        City("Estambul", "Turquía", "Europe/Istanbul"),
        City("Dubái", "Emiratos Árabes", "Asia/Dubai"),
        City("Tokio", "Japón", "Asia/Tokyo"),
        City("Seúl", "Corea del Sur", "Asia/Seoul"),
        City("Singapur", "Singapur", "Asia/Singapore"),
        City("Sídney", "Australia", "Australia/Sydney"),
        City("Nueva York", "Estados Unidos", "America/New_York"),
        City("Miami", "Estados Unidos", "America/New_York"),
        City("Chicago", "Estados Unidos", "America/Chicago"),
        City("Denver", "Estados Unidos", "America/Denver"),
        City("Los Ángeles", "Estados Unidos", "America/Los_Angeles"),
        City("Ciudad de México", "México", "America/Mexico_City"),
        City("Lima", "Perú", "America/Lima"),
        City("Santiago", "Chile", "America/Santiago"),
        City("Buenos Aires", "Argentina", "America/Argentina/Buenos_Aires"),
        City("São Paulo", "Brasil", "America/Sao_Paulo"),
    )

    /** Ciudad guardada por su nombre; si ya no existe, null. */
    fun byName(name: String?): City? = all.firstOrNull { it.name == name }
}

/** Preferencias de presentación: relojes, saludo y tema. */
data class DisplaySettings(
    val primaryCity: String = "Medellín",
    /** null = sin reloj secundario. */
    val secondaryCity: String? = "Zúrich",
    val greetingEnabled: Boolean = true,
    val greetingName: String = "familia",
    val useProfileName: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.Auto,
)

object ThemeLogic {
    const val DAY_START_HOUR = 6
    const val DAY_END_HOUR = 18

    /** Claro de 6:00 a 18:00 y oscuro de 18:00 a 6:00; Light/Dark lo fijan. */
    fun isDark(mode: ThemeMode, hour: Int): Boolean = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.Auto -> hour < DAY_START_HOUR || hour >= DAY_END_HOUR
    }

    fun isDarkNow(mode: ThemeMode, zoneId: String, nowMs: Long): Boolean {
        val zone = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault())
        return isDark(mode, ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMs), zone).hour)
    }
}

/** Nombre que se usa en el saludo: el del perfil de Android si se pidió y existe; si no, el escrito. */
fun greetingNameFor(settings: DisplaySettings, profileName: String?): String {
    val typed = settings.greetingName.trim().ifEmpty { "familia" }
    if (!settings.useProfileName) return typed
    return profileName?.trim()?.takeIf { it.isNotEmpty() } ?: typed
}
