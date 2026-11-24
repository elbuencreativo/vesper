package com.minimal.tvlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconColorTest {

    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()
    private val clear = 0x00000000

    /** Imagen 4x4: borde [border], centro [center]. */
    private fun image(border: Int, center: Int) =
        IntArray(16) { i ->
            val x = i % 4
            val y = i / 4
            if (x == 0 || y == 0 || x == 3 || y == 3) border else center
        }

    @Test
    fun edgeColor_isTheColorOfTheBorder_notTheCenter() {
        assertEquals(red, edgeColor(image(border = red, center = blue), 4, 4))
    }

    @Test
    fun edgeColor_ignoresTransparentBorders_andReturnsNullWhenThereIsNoColor() {
        assertNull(edgeColor(image(border = clear, center = blue), 4, 4))
        assertNull(edgeColor(IntArray(0), 0, 0))
    }

    @Test
    fun edgeColor_averagesOpaqueBorderPixels() {
        val pixels = image(border = red, center = blue)
        for (x in 0 until 4) pixels[x] = blue // fila de arriba azul: 4 de 12 píxeles del borde
        val c = edgeColor(pixels, 4, 4)!!
        val r = (c shr 16) and 0xFF
        val b = c and 0xFF
        assertEquals(0xFF, c ushr 24)
        assertEquals(true, r in 120..200 && b in 40..140 && r > b)
    }

    @Test
    fun warmMono_matchesTheIconFilter() {
        assertEquals(0xFF645646.toInt(), warmMono(0xFF646464.toInt())) // gris 100 -> (100, 86, 70)
        assertEquals(0xFF000000.toInt(), warmMono(0xFF000000.toInt()))
    }
}
