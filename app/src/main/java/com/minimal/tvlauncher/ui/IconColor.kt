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

/**
 * Color de marca de un icono: el matiz más presente entre los píxeles vivos (saturados y no oscuros).
 * Devuelve ARGB opaco con brillo suficiente para verse sobre fondo oscuro, o null si el icono es
 * casi monocromo (blanco, negro, grises): quien llama usa entonces el acento.
 */
fun brandColor(pixels: IntArray, width: Int, height: Int): Int? {
    if (width <= 0 || height <= 0 || pixels.size < width * height) return null
    val buckets = 12
    val weight = DoubleArray(buckets)
    val sr = DoubleArray(buckets)
    val sg = DoubleArray(buckets)
    val sb = DoubleArray(buckets)
    val hsv = FloatArray(3)
    for (p in pixels) {
        if ((p ushr 24) < 200) continue
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        val mx = maxOf(r, g, b)
        val mn = minOf(r, g, b)
        if (mx == 0) continue
        val sat = (mx - mn).toFloat() / mx
        val v = mx / 255f
        if (sat < 0.4f || v < 0.3f) continue
        val h = hue(r, g, b, mx, mn)
        val i = ((h / 360f) * buckets).toInt().coerceIn(0, buckets - 1)
        val w = (sat * v).toDouble()
        weight[i] += w; sr[i] += r * w; sg[i] += g * w; sb[i] += b * w
    }
    val best = weight.indices.maxByOrNull { weight[it] } ?: return null
    if (weight[best] < pixels.size * 0.02) return null
    var r = (sr[best] / weight[best]).toInt()
    var g = (sg[best] / weight[best]).toInt()
    var b = (sb[best] / weight[best]).toInt()
    val mx = maxOf(r, g, b)
    if (mx in 1..199) { // subir el brillo hasta ~200 conservando el matiz
        val k = 200f / mx
        r = (r * k).toInt().coerceAtMost(255); g = (g * k).toInt().coerceAtMost(255); b = (b * k).toInt().coerceAtMost(255)
    }
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

private fun hue(r: Int, g: Int, b: Int, mx: Int, mn: Int): Float {
    val d = (mx - mn).toFloat()
    if (d == 0f) return 0f
    val h = when (mx) {
        r -> ((g - b) / d) % 6f
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    } * 60f
    return if (h < 0) h + 360f else h
}
