package com.minimal.tvlauncher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Paleta elegida por el usuario (10 tonos, de azul noche a ámbar), organizada con roles tonales
 * al estilo Material 3: fondo, superficies, texto, acento de foco y contornos.
 */
object Palette {
    // Los 10 tonos de la paleta
    val Navy = Color(0xFF04071D)
    val Wine900 = Color(0xFF320A17)
    val Wine700 = Color(0xFF611214)
    val Crimson = Color(0xFF901C16)
    val Red = Color(0xFFBF281B)
    val Vermilion = Color(0xFFCA4021)
    val Orange = Color(0xFFD7662A)
    val Amber = Color(0xFFE69135)
    val Gold = Color(0xFFEEA73C)
    val Sun = Color(0xFFF5BD42)

    // Roles
    val Background = Navy
    val Surface = Color(0xFF14091A) // contenedor tonal: azul noche con un toque de vino
    val SurfaceHigh = Wine900
    val Text = Color(0xFFFFF3E4)
    val TextMuted = Color(0xFFD2B9A6)
    val Accent = Sun
    val AccentGlow = Orange
    val Focus = Sun
    val Line = Sun.copy(alpha = 0.18f)
}

/** Iconos en monocromo cálido (sepia suave); recuperan su color al enfocar. */
val WarmMonochrome: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
            0.1828f, 0.6151f, 0.0621f, 0f, 0f,
            0.1488f, 0.5006f, 0.0505f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)
