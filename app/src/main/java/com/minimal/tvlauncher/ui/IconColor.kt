package com.minimal.tvlauncher.ui

import kotlin.math.roundToInt

/**
 * Color promedio del borde (anillo exterior) de un icono, ignorando píxeles transparentes.
 * Sirve para pintar el marco redondeado del color de fondo del icono y evitar el "doble marco".
 * Devuelve ARGB opaco, o null si el borde no tiene color (icono con bordes transparentes).
 */
fun edgeColor(pixels: IntArray, width: Int, height: Int): Int? {
    if (width <= 0 || height <= 0 || pixels.size < width * height) return null
    var r = 0L
    var g = 0L
    var b = 0L
    var n = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (x != 0 && y != 0 && x != width - 1 && y != height - 1) continue
            val p = pixels[y * width + x]
            if ((p ushr 24) < 200) continue
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
            n++
        }
    }
    if (n == 0) return null
    return (0xFF shl 24) or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
}

/** El mismo monocromo cálido que [WarmMonochrome], aplicado a un color suelto (para el marco sin foco). */
fun warmMono(argb: Int): Int {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val gray = 0.2126f * r + 0.7152f * g + 0.0722f * b
    fun c(v: Float) = v.roundToInt().coerceIn(0, 255)
    return (argb and 0xFF000000.toInt()) or (c(gray) shl 16) or (c(gray * 0.86f) shl 8) or c(gray * 0.70f)
}
